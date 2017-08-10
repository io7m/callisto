/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.callisto.prototype0.transport;

import com.io7m.callisto.prototype0.messages.CoDataReceipt;
import com.io7m.callisto.prototype0.messages.CoDataReliable;
import com.io7m.callisto.prototype0.messages.CoDataUnreliable;
import com.io7m.callisto.prototype0.messages.CoMessage;
import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSendableType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantReference;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public final class CoTransportConnection implements CoTransportConnectionType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransportConnection.class);

  private final int id;
  private final CoNetworkPacketSendableType socket;
  private final SocketAddress remote;
  private final Int2ReferenceOpenHashMap<CoTransportConnectionChannel> channels;
  private final CoStringConstantPoolReadableType strings;
  private final ListenerType listener;

  CoTransportConnection(
    final ListenerType in_listener,
    final CoStringConstantPoolReadableType in_strings,
    final CoNetworkPacketSendableType in_socket,
    final SocketAddress in_remote,
    final int in_id)
  {
    this.listener = NullCheck.notNull(in_listener, "Listener");
    this.strings = NullCheck.notNull(in_strings, "Strings");
    this.socket = NullCheck.notNull(in_socket, "Socket");
    this.remote = NullCheck.notNull(in_remote, "Remote");
    this.id = in_id;
    this.channels = new Int2ReferenceOpenHashMap<>();
  }

  public static CoTransportConnectionType create(
    final ListenerType in_listener,
    final CoStringConstantPoolReadableType in_strings,
    final CoNetworkPacketSendableType in_socket,
    final SocketAddress in_remote,
    final int in_id)
  {
    return new CoTransportConnection(
      in_listener, in_strings, in_socket, in_remote, in_id);
  }

  private static int packetChannelID(
    final CoPacket p)
  {
    switch (p.getValueCase()) {
      case HELLO:
      case HELLO_RESPONSE:
      case VALUE_NOT_SET:
        throw new UnreachableCodeException();

      case DATA_RECEIPT: {
        return p.getDataReceipt().getId().getChannel();
      }
      case DATA_RELIABLE: {
        return p.getDataReliable().getId().getChannel();
      }
      case DATA_UNRELIABLE: {
        return p.getDataUnreliable().getId().getChannel();
      }
      case DATA_RELIABLE_FRAGMENT: {
        return p.getDataReliableFragment().getId().getChannel();
      }
    }

    throw new UnreachableCodeException();
  }

  @Override
  public String toString()
  {
    return new StringBuilder(64)
      .append("[CoTransportConnection 0x")
      .append(Integer.toUnsignedString(this.id, 16))
      .append(" ")
      .append(this.remote)
      .append("]")
      .toString();
  }

  @Override
  public SocketAddress remote()
  {
    return this.remote;
  }

  @Override
  public int id()
  {
    return this.id;
  }

  @Override
  public void send(
    final Reliability reliability,
    final int channel,
    final String type,
    final ByteBuffer message)
  {
    NullCheck.notNull(reliability, "Reliability");
    NullCheck.notNull(type, "Type");
    NullCheck.notNull(message, "Message");

    final CoTransportConnectionChannel transport_channel =
      this.createOrFindChannel(channel);

    transport_channel.enqueue(
      reliability,
      this.strings.lookupReference(type),
      message);
  }

  private CoTransportConnectionChannel createOrFindChannel(
    final int channel)
  {
    final CoTransportConnectionChannel transport_channel;
    if (this.channels.containsKey(channel)) {
      transport_channel = this.channels.get(channel);
    } else {
      transport_channel = new CoTransportConnectionChannel(this, channel);
      this.channels.put(channel, transport_channel);
    }
    return transport_channel;
  }

  @Override
  public void receive(
    final CoPacket p)
  {
    NullCheck.notNull(p, "Packet");

    switch (p.getValueCase()) {
      case HELLO:
      case HELLO_RESPONSE:
      case VALUE_NOT_SET:
        throw new UnimplementedCodeException();

      case DATA_RECEIPT:
      case DATA_RELIABLE:
      case DATA_UNRELIABLE:
      case DATA_RELIABLE_FRAGMENT: {
        final int channel_id = packetChannelID(p);
        final CoTransportConnectionChannel transport_channel =
          this.createOrFindChannel(channel_id);

        transport_channel.receive(p);
        break;
      }
    }
  }

  @Override
  public void tick()
  {
    for (final int key : this.channels.keySet()) {
      final CoTransportConnectionChannel ch = this.channels.get(key);
      ch.handleReceives();
      ch.handleSends();
    }
  }

  private static final class CoTransportConnectionChannel
    implements CoTransportPacketBuilder.ListenerType
  {
    private final int channel;
    private final ArrayDeque<CoPacket> q_sending;
    private final ArrayDeque<CoPacket> q_receive;
    private final Int2ReferenceRBTreeMap<CoMessage> q_receive_messages;
    private final CoTransportPacketBuilder packets;
    private final CoTransportConnection connection;
    private final CoTransportSequenceNumberTracker sequences;

    CoTransportConnectionChannel(
      final CoTransportConnection in_connection,
      final int in_channel)
    {
      this.connection = NullCheck.notNull(in_connection, "Connection");
      this.channel = in_channel;

      this.sequences =
        new CoTransportSequenceNumberTracker();
      this.q_sending =
        new ArrayDeque<>(16);
      this.q_receive =
        new ArrayDeque<>(16);
      this.q_receive_messages =
        new Int2ReferenceRBTreeMap<>(this::compareSequenceNumbers);

      this.packets =
        new CoTransportPacketBuilder(
          this.sequences,
          this.connection.socket.maximumTransferUnit(),
          this.channel,
          this.connection.id);
    }

    void enqueue(
      final Reliability reliability,
      final CoStringConstantReference message_type,
      final ByteBuffer message_data)
    {
      switch (reliability) {
        case MESSAGE_RELIABLE: {
          this.packets.reliableAppend(this, message_type, message_data);
          break;
        }
        case MESSAGE_UNRELIABLE: {
          this.packets.unreliableAppend(this, message_type, message_data);
          break;
        }
      }
    }

    public void handleReceives()
    {
      this.handleReceivesScanQueue();

      /*
       * If the sequence tracker indicates that there is an unbroken sequence
       * of received packets (the typical case when no packets have been lost),
       * then deliver all of the messages to the application and clear the
       * receive queue.
       */

      final CoTransportReliableReceiverWindow window =
        this.sequences.reliableReceiverWindow();
      if (window.missed().isEmpty()) {
        this.handleReceivesDeliverAll();
        return;
      }

      /*
       * Otherwise, there are missing reliable packets yet to be received.
       * Scan the queue and drop any unreliable packets (because the data
       * is time-critical and therefore it's better avoid delivering the
       * data than to deliver it late).
       */

      this.handleReceivesDropUnreliables();
      this.handleReceivesEnqueueReceipt();
    }

    /**
     * Send a receipt for any missing packets, and the sequence number of
     * the highest received reliable packet prior to any missing packets.
     */

    private void handleReceivesEnqueueReceipt()
    {
      this.packets.receipts(this);
    }

    private void handleReceivesDeliverAll()
    {
      {
        final Iterator<CoPacket> iter = this.q_receive.iterator();
        while (iter.hasNext()) {
          final CoPacket p = iter.next();
          switch (p.getValueCase()) {
            case HELLO:
            case HELLO_RESPONSE:
            case DATA_RELIABLE_FRAGMENT:
            case VALUE_NOT_SET: {
              throw new UnreachableCodeException();
            }

            case DATA_RELIABLE: {
              final CoDataReliable d = p.getDataReliable();
              for (int index = 0; index < d.getMessagesCount(); ++index) {
                final CoMessage m = d.getMessages(index);
                final int id = m.getMessageId();
                if (!this.q_receive_messages.containsKey(id)) {
                  this.q_receive_messages.put(id, m);
                }
              }
              break;
            }

            case DATA_RECEIPT: {
              break;
            }

            case DATA_UNRELIABLE: {
              final CoDataUnreliable d = p.getDataUnreliable();
              for (int index = 0; index < d.getMessagesCount(); ++index) {
                final CoMessage m = d.getMessages(index);
                final int id = m.getMessageId();
                if (!this.q_receive_messages.containsKey(id)) {
                  this.q_receive_messages.put(id, m);
                }
              }
              break;
            }
          }

          iter.remove();
        }
      }

      Postconditions.checkPostcondition(
        this.q_receive.isEmpty(),
        "Receive queue must be empty");

      {
        final IntBidirectionalIterator iter =
          this.q_receive_messages.keySet().iterator();

        while (iter.hasNext()) {
          final int message_id = iter.nextInt();
          final CoMessage message = this.q_receive_messages.get(message_id);
          this.connection.listener.onMessageReceived(
            this.connection, this.channel, message);
          iter.remove();
        }
      }

      Postconditions.checkPostcondition(
        this.q_receive_messages.isEmpty(),
        "Receive messages must be empty");

      this.handleReceivesEnqueueReceipt();
      this.sequences.reliableReceiverWindow().reset();
    }

    /**
     * Drop all unreliable packets from the queue.
     */

    private void handleReceivesDropUnreliables()
    {
      final Iterator<CoPacket> iter = this.q_receive.iterator();
      while (iter.hasNext()) {
        final CoPacket p = iter.next();
        switch (p.getValueCase()) {
          case HELLO:
          case HELLO_RESPONSE:
          case VALUE_NOT_SET:
          case DATA_RELIABLE:
          case DATA_RELIABLE_FRAGMENT:
          case DATA_RECEIPT: {
            break;
          }

          case DATA_UNRELIABLE: {
            iter.remove();
            this.connection.listener.onDropPacketUnreliable(
              this.connection,
              this.channel,
              p.getDataUnreliable().getId().getSequence(),
              p.getSerializedSize());
            break;
          }
        }
      }
    }

    /**
     * Scan the queue. Any received reliable packets are marked as having been
     * received. Any receipts are examined (and removed from the queue) to
     * enqueue re-sends of missed packets and purge old packets that definitely
     * will not need to be re-sent.
     */

    private void handleReceivesScanQueue()
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("scanning receive queue");
      }

      final Iterator<CoPacket> iter = this.q_receive.iterator();
      while (iter.hasNext()) {
        final CoPacket p = iter.next();
        switch (p.getValueCase()) {
          case HELLO:
          case HELLO_RESPONSE:
          case VALUE_NOT_SET: {
            throw new UnreachableCodeException();
          }

          case DATA_RELIABLE: {
            final int sequence =
              p.getDataReliable().getId().getSequence();
            this.sequences.reliableReceiverWindow().receive(sequence);

            if (LOG.isTraceEnabled()) {
              LOG.trace("received reliable {}", Integer.valueOf(sequence));
            }
            break;
          }

          case DATA_RELIABLE_FRAGMENT: {
            final int sequence =
              p.getDataReliableFragment().getId().getSequence();
            this.sequences.reliableReceiverWindow().receive(sequence);

            if (LOG.isTraceEnabled()) {
              LOG.trace(
                "received reliable fragment {}",
                Integer.valueOf(sequence));
            }
            break;
          }

          case DATA_UNRELIABLE: {
            break;
          }

          case DATA_RECEIPT: {
            final CoDataReceipt r = p.getDataReceipt();
            for (int index = 0;
                 index < r.getSequenceReliableNotReceivedCount();
                 ++index) {
              final int not_received =
                r.getSequenceReliableNotReceived(index);
              this.enqueueOldSavedPacket(not_received);
            }

            this.purgeOldSentPackets(r.getSequenceReliableReceived());
            iter.remove();
            break;
          }
        }
      }
    }

    private void purgeOldSentPackets(
      final int received)
    {
      LOG.trace(
        "discarding acknowledged packets up to {}",
        Integer.valueOf(received));

      LOG.trace("purgeOldSentPackets: ", new UnimplementedCodeException());
    }

    private void enqueueOldSavedPacket(
      final int not_received)
    {
      LOG.trace(
        "resending old packet {}: ",
        Integer.valueOf(not_received));

      LOG.trace("enqueueOldSavedPacket: ", new UnimplementedCodeException());
    }

    public void handleSends()
    {
      /*
       * Finish any packets that are currently being built.
       */

      this.packets.reliableFinishRemaining(this);
      this.packets.unreliableFinishRemaining(this);

      /*
       * Send everything that's queued.
       */

      while (!this.q_sending.isEmpty()) {
        final CoPacket p = this.q_sending.remove();
        this.connection.socket.send(
          this.connection.remote, ByteBuffer.wrap(p.toByteArray()));

        final int size = p.getSerializedSize();
        switch (p.getValueCase()) {
          case HELLO:
          case HELLO_RESPONSE:
          case VALUE_NOT_SET: {
            throw new UnreachableCodeException();
          }

          case DATA_RECEIPT: {
            final int sequence =
              p.getDataReceipt().getId().getSequence();

            this.connection.listener.onSendPacketReceipt(
              this.connection, this.channel, sequence, size);
            break;
          }

          case DATA_RELIABLE: {
            final int sequence =
              p.getDataReliable().getId().getSequence();
            this.connection.listener.onSendPacketReliable(
              this.connection, this.channel, sequence, size);
            break;
          }

          case DATA_UNRELIABLE: {
            final int sequence =
              p.getDataUnreliable().getId().getSequence();
            this.connection.listener.onSendPacketUnreliable(
              this.connection, this.channel, sequence, size);
            break;
          }

          case DATA_RELIABLE_FRAGMENT: {
            final int sequence =
              p.getDataReliableFragment().getId().getSequence();
            this.connection.listener.onSendPacketReliableFragment(
              this.connection, this.channel, sequence, size);
            break;
          }
        }
      }
    }

    @Override
    public void onCreatedPacketReliable(
      final CoPacket p)
    {
      this.q_sending.add(p);
      this.connection.listener.onEnqueuePacketReliable(
        this.connection,
        this.channel,
        p.getDataReliable().getId().getSequence(),
        p.getSerializedSize());
    }

    @Override
    public void onCreatedPacketUnreliable(
      final CoPacket p)
    {
      this.q_sending.add(p);
      this.connection.listener.onEnqueuePacketUnreliable(
        this.connection,
        this.channel,
        p.getDataUnreliable().getId().getSequence(),
        p.getSerializedSize());
    }

    @Override
    public void onCreatedPacketReliableFragment(
      final CoPacket p)
    {
      this.q_sending.add(p);
      this.connection.listener.onEnqueuePacketReliableFragment(
        this.connection,
        this.channel,
        p.getDataReliableFragment().getId().getSequence(),
        p.getSerializedSize());
    }

    @Override
    public void onCreatedPacketReceipt(
      final CoPacket p)
    {
      this.q_sending.add(p);
      this.connection.listener.onEnqueuePacketReceipt(
        this.connection,
        this.channel,
        p.getDataReliableFragment().getId().getSequence(),
        p.getSerializedSize());
    }

    public void receive(
      final CoPacket p)
    {
      switch (p.getValueCase()) {
        case HELLO:
        case HELLO_RESPONSE:
        case VALUE_NOT_SET: {
          throw new UnreachableCodeException();
        }

        case DATA_RELIABLE:
        case DATA_UNRELIABLE:
        case DATA_RELIABLE_FRAGMENT:
        case DATA_RECEIPT: {
          this.q_receive.add(p);
          break;
        }
      }
    }

    private int compareSequenceNumbers(
      final Integer s0,
      final Integer s1)
    {
      return this.sequences.serial().compare(s0.intValue(), s1.intValue());
    }
  }
}
