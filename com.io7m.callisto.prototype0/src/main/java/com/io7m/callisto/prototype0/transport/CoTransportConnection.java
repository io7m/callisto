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

import com.google.protobuf.ByteString;
import com.io7m.callisto.prototype0.messages.CoDataAck;
import com.io7m.callisto.prototype0.messages.CoDataReliable;
import com.io7m.callisto.prototype0.messages.CoDataUnreliable;
import com.io7m.callisto.prototype0.messages.CoMessage;
import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.messages.CoPing;
import com.io7m.callisto.prototype0.messages.CoPong;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSendableType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantReference;
import com.io7m.jaffirm.core.Invariants;
import com.io7m.jaffirm.core.Postconditions;
import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceRBTreeMap;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Optional;

public final class CoTransportConnection implements CoTransportConnectionType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransportConnection.class);

  private final int id;
  private final CoNetworkPacketSendableType socket;
  private final SocketAddress remote;
  private final Int2ReferenceOpenHashMap<CoTransportConnectionChannel> channels;
  private final CoStringConstantPoolReadableType strings;
  private final CoTransportConnectionListenerType listener;
  private final CoTransportConnectionConfiguration config;
  private final Clock clock;
  private final ByteString pong;
  private final ByteString ping;
  private long ticks;
  private int ticks_since_receive;
  private long time_ping_sent_ms;
  private volatile long round_trip_time;

  CoTransportConnection(
    final Clock in_clock,
    final CoTransportConnectionListenerType in_listener,
    final CoStringConstantPoolReadableType in_strings,
    final CoNetworkPacketSendableType in_socket,
    final CoTransportConnectionConfiguration in_configuration,
    final SocketAddress in_remote,
    final int in_id)
  {
    this.clock = NullCheck.notNull(in_clock, "Clock");
    this.listener = NullCheck.notNull(in_listener, "Listener");
    this.strings = NullCheck.notNull(in_strings, "Strings");
    this.socket = NullCheck.notNull(in_socket, "Socket");
    this.config = NullCheck.notNull(in_configuration, "Configuration");
    this.remote = NullCheck.notNull(in_remote, "Remote");
    this.id = in_id;
    this.channels = new Int2ReferenceOpenHashMap<>();
    this.time_ping_sent_ms = 0L;
    this.round_trip_time = 0L;
    this.pong = makePong(this.id);
    this.ping = makePing(this.id);
    this.ticks = 0L;
  }

  private static ByteString makePong(
    final int id)
  {
    final CoPong po = CoPong.newBuilder().setConnectionId(id).build();
    final CoPacket p = CoPacket.newBuilder().setPong(po).build();
    return p.toByteString();
  }

  private static ByteString makePing(
    final int id)
  {
    final CoPing pi = CoPing.newBuilder().setConnectionId(id).build();
    final CoPacket p = CoPacket.newBuilder().setPing(pi).build();
    return p.toByteString();
  }

  public static CoTransportConnectionType create(
    final Clock in_clock,
    final CoTransportConnectionListenerType in_listener,
    final CoStringConstantPoolReadableType in_strings,
    final CoNetworkPacketSendableType in_socket,
    final CoTransportConnectionConfiguration in_configuration,
    final SocketAddress in_remote,
    final int in_id)
  {
    return new CoTransportConnection(
      in_clock,
      in_listener,
      in_strings,
      in_socket,
      in_configuration,
      in_remote,
      in_id);
  }

  private static int packetChannelID(
    final CoPacket p)
  {
    switch (p.getValueCase()) {
      case PING:
      case PONG:
      case BYE:
      case HELLO:
      case HELLO_RESPONSE:
      case VALUE_NOT_SET: {
        throw new UnreachableCodeException();
      }

      case DATA_ACK: {
        return p.getDataAck().getId().getChannel();
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

    final Optional<CoStringConstantReference> type_ref_opt =
      this.strings.lookupReference(type);

    if (!type_ref_opt.isPresent()) {
      throw new IllegalArgumentException(
        "No string constant for type: " + type);
    }

    final CoStringConstantReference type_ref =
      type_ref_opt.get();
    final CoTransportConnectionChannel transport_channel =
      this.createOrFindChannel(channel);

    transport_channel.enqueue(reliability, type_ref, message);
  }

  @Override
  public long roundTripTime()
  {
    return this.round_trip_time;
  }

  private CoTransportConnectionChannel createOrFindChannel(
    final int channel)
  {
    final CoTransportConnectionChannel transport_channel;
    if (this.channels.containsKey(channel)) {
      transport_channel = this.channels.get(channel);
    } else {
      transport_channel =
        new CoTransportConnectionChannel(this, channel);
      this.channels.put(channel, transport_channel);
    }
    return transport_channel;
  }

  @Override
  public void receive(
    final CoPacket p)
  {
    NullCheck.notNull(p, "Packet");

    this.ticks_since_receive = 0;

    switch (p.getValueCase()) {
      case PING: {
        this.sendPacket(this.remote, this.pong.asReadOnlyByteBuffer());
        this.listener.onReceivePacketPing(this);
        this.listener.onSendPacketPong(this);
        break;
      }

      case PONG: {
        final long time_now =
          this.clock.millis();
        this.round_trip_time =
          Math.max(0L, time_now - this.time_ping_sent_ms);

        this.listener.onReceivePacketPong(this);
        break;
      }

      case BYE:
      case HELLO:
      case HELLO_RESPONSE:
      case VALUE_NOT_SET: {
        throw new UnimplementedCodeException();
      }

      case DATA_ACK:
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

  private void sendPacket(
    final SocketAddress receiver,
    final ByteBuffer data)
  {
    this.socket.send(receiver, data);
  }

  @Override
  public void tick()
  {
    ++this.ticks;

    final int timeout =
      this.config.ticksTimeout();
    this.ticks_since_receive =
      Math.max(0, Math.min(this.ticks_since_receive + 1, timeout));

    if (this.ticks_since_receive >= timeout) {
      this.listener.onTimedOut(this);
      return;
    }

    if (this.ticks % (long) this.config.ticksPingRate() == 0L) {
      this.time_ping_sent_ms = this.clock.millis();
      this.sendPacket(this.remote, this.ping.asReadOnlyByteBuffer());
      this.listener.onSendPacketPing(this);
    }

    for (final int key : this.channels.keySet()) {
      final CoTransportConnectionChannel ch = this.channels.get(key);
      ch.tick();
    }
  }

  private static final class CoTransportConnectionChannel
    implements CoTransportPacketBuilderListenerType
  {
    private final int channel;
    private final ArrayDeque<CoPacket> q_sending;
    private final ArrayDeque<CoPacket> q_receive;
    private final Int2ReferenceRBTreeMap<CoMessage> q_receive_messages;
    private final CoTransportPacketBuilder packets;
    private final CoTransportConnection connection;
    private final CoTransportSequenceNumberTracker sequences;
    private final Int2ReferenceOpenHashMap<CoPacket> q_sent_reliable;
    private final Int2IntOpenHashMap q_sent_reliable_ttls;

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
      this.q_sent_reliable =
        new Int2ReferenceOpenHashMap<>();
      this.q_sent_reliable_ttls =
        new Int2IntOpenHashMap();

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

    private void handleReceives()
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
     * Send a receipt for any missing packets.
     */

    private void handleReceivesEnqueueReceipt()
    {
      if (this.receiptRequired()) {
        this.packets.receipts(this);
      }
    }

    private boolean receiptRequired()
    {
      return !this.sequences.reliableReceiverWindow().missed().isEmpty();
    }

    private void handleReceivesDeliverAll()
    {
      {
        final Iterator<CoPacket> iter = this.q_receive.iterator();
        while (iter.hasNext()) {
          final CoPacket p = iter.next();
          switch (p.getValueCase()) {
            case PING:
            case PONG:
            case BYE:
            case HELLO:
            case HELLO_RESPONSE:
            case DATA_RELIABLE_FRAGMENT:
            case VALUE_NOT_SET: {
              throw new UnreachableCodeException();
            }

            case DATA_RELIABLE: {
              final CoDataReliable d = p.getDataReliable();

              this.connection.listener.onReceivePacketDeliverReliable(
                this.connection,
                this.channel,
                d.getId().getSequence(),
                p.getSerializedSize());

              for (int index = 0; index < d.getMessagesCount(); ++index) {
                final CoMessage m = d.getMessages(index);
                final int id = m.getMessageId();
                if (!this.q_receive_messages.containsKey(id)) {
                  this.q_receive_messages.put(id, m);
                }
              }
              break;
            }

            case DATA_ACK: {
              break;
            }

            case DATA_UNRELIABLE: {
              final CoDataUnreliable d = p.getDataUnreliable();

              this.connection.listener.onReceivePacketDeliverUnreliable(
                this.connection,
                this.channel,
                d.getId().getSequence(),
                p.getSerializedSize());

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
          case PING:
          case PONG:
          case BYE:
          case HELLO:
          case HELLO_RESPONSE:
          case VALUE_NOT_SET:
          case DATA_RELIABLE:
          case DATA_RELIABLE_FRAGMENT:
          case DATA_ACK: {
            break;
          }

          case DATA_UNRELIABLE: {
            iter.remove();
            this.connection.listener.onReceiveDropPacketUnreliable(
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
          case PING:
          case PONG:
          case BYE:
          case HELLO:
          case HELLO_RESPONSE:
          case VALUE_NOT_SET: {
            throw new UnreachableCodeException();
          }

          case DATA_RELIABLE: {
            final int sequence = p.getDataReliable().getId().getSequence();
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

          case DATA_ACK: {
            final CoDataAck r = p.getDataAck();

            final int count = r.getSequencesReliableNotReceivedCount();
            for (int index = 0; index < count; ++index) {
              final int sequence = r.getSequencesReliableNotReceived(index);
              this.enqueueOldSavedPacket(sequence);
            }

            iter.remove();
            break;
          }
        }
      }
    }

    private void enqueueOldSavedPacket(
      final int not_received)
    {
      if (this.q_sent_reliable.containsKey(not_received)) {
        LOG.trace("resending old packet {}: ", Integer.valueOf(not_received));
        final CoPacket p = this.q_sent_reliable.get(not_received);

        Invariants.checkInvariant(
          p.getValueCase() == CoPacket.ValueCase.DATA_RELIABLE,
          "Must be reliable packet");

        this.q_sending.add(p);
        this.connection.listener.onEnqueuePacketReliableRequeue(
          this.connection, this.channel, not_received, p.getSerializedSize());
        return;
      }

      LOG.error(
        "requested to re-queue missing packet {}",
        Integer.valueOf(not_received));

      throw new UnimplementedCodeException();
    }

    private void handleSends()
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
        this.connection.sendPacket(
          this.connection.remote,
          p.toByteString().asReadOnlyByteBuffer());

        final int size = p.getSerializedSize();
        switch (p.getValueCase()) {
          case PING:
          case PONG:
          case BYE:
          case HELLO:
          case HELLO_RESPONSE:
          case VALUE_NOT_SET: {
            throw new UnreachableCodeException();
          }

          case DATA_ACK: {
            final int sequence =
              p.getDataAck().getId().getSequence();

            this.connection.listener.onSendPacketAck(
              this.connection, this.channel, sequence, size);
            break;
          }

          case DATA_RELIABLE: {
            final int sequence = p.getDataReliable().getId().getSequence();
            this.saveSentPacket(p);
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

    private void saveSentPacket(
      final CoPacket p)
    {
      Preconditions.checkPrecondition(
        p.getValueCase() == CoPacket.ValueCase.DATA_RELIABLE,
        "Packet must be reliable");

      final int sequence = p.getDataReliable().getId().getSequence();
      this.q_sent_reliable.put(sequence, p);
      final int ttl = this.connection.config.ticksReliableTTL();
      this.q_sent_reliable_ttls.put(sequence, ttl);

      this.connection.listener.onSavedPacketReliableSave(
        this.connection, this.channel, sequence, p.getSerializedSize());
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
    public void onCreatedPacketAck(
      final CoPacket p)
    {
      this.q_sending.add(p);
      this.connection.listener.onEnqueuePacketAck(
        this.connection,
        this.channel,
        p.getDataReliableFragment().getId().getSequence(),
        p.getSerializedSize());
    }

    void receive(
      final CoPacket p)
    {
      switch (p.getValueCase()) {
        case PING:
        case PONG:
        case BYE:
        case HELLO:
        case HELLO_RESPONSE:
        case VALUE_NOT_SET: {
          throw new UnreachableCodeException();
        }

        case DATA_RELIABLE: {
          this.connection.listener.onReceivePacketReliable(
            this.connection,
            this.channel,
            p.getDataReliable().getId().getSequence(),
            p.getSerializedSize());
          break;
        }

        case DATA_UNRELIABLE: {
          this.connection.listener.onReceivePacketUnreliable(
            this.connection,
            this.channel,
            p.getDataUnreliable().getId().getSequence(),
            p.getSerializedSize());
          break;
        }

        case DATA_RELIABLE_FRAGMENT: {
          this.connection.listener.onReceivePacketReliableFragment(
            this.connection,
            this.channel,
            p.getDataReliableFragment().getId().getSequence(),
            p.getSerializedSize());
          break;
        }

        case DATA_ACK: {
          this.connection.listener.onReceivePacketAck(
            this.connection,
            this.channel,
            p.getDataAck().getId().getSequence(),
            p.getSerializedSize());
          break;
        }
      }

      this.q_receive.add(p);
    }

    private int compareSequenceNumbers(
      final Integer s0,
      final Integer s1)
    {
      return this.sequences.serial().compare(s0.intValue(), s1.intValue());
    }

    public void tick()
    {
      this.handleReceives();
      this.handleSends();
      this.handlePacketExpirations();
    }

    private void handlePacketExpirations()
    {
      final IntIterator iter = this.q_sent_reliable_ttls.keySet().iterator();
      while (iter.hasNext()) {
        final int sequence = iter.nextInt();
        final int time = this.q_sent_reliable_ttls.get(sequence);

        Invariants.checkInvariantI(
          sequence,
          this.q_sent_reliable.containsKey(sequence),
          s -> "Packet must have been saved");

        final CoPacket p = this.q_sent_reliable.get(sequence);

        Invariants.checkInvariant(
          p,
          p.getValueCase() == CoPacket.ValueCase.DATA_RELIABLE,
          q -> "Packet must be reliable");

        final int time_next = time - 1;
        if (time_next <= 0) {
          this.q_sent_reliable.remove(sequence);
          iter.remove();
          this.connection.listener.onSavedPacketReliableExpire(
            this.connection, this.channel, sequence, p.getSerializedSize());
          continue;
        }

        this.q_sent_reliable_ttls.put(sequence, time_next);
      }
    }
  }
}
