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

import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSendableType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantReference;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

public final class CoTransportConnection implements CoTransportConnectionType
{
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

  private static int packetChannelID(
    final CoPacket p)
  {
    switch (p.getValueCase()) {
      case HELLO:
      case HELLO_RESPONSE:
      case VALUE_NOT_SET:
        throw new UnreachableCodeException();

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
        throw new UnreachableCodeException();

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
    private final CoTransportPacketBuilder packets;
    private final Int2ReferenceOpenHashMap<CoPacket> sent_ack_waiting;
    private final CoTransportConnection connection;

    CoTransportConnectionChannel(
      final CoTransportConnection in_connection,
      final int in_channel)
    {
      this.connection = NullCheck.notNull(in_connection, "Connection");
      this.channel = in_channel;
      this.q_sending = new ArrayDeque<>(16);
      this.q_receive = new ArrayDeque<>(16);
      this.sent_ack_waiting = new Int2ReferenceOpenHashMap<>(32);

      this.packets =
        new CoTransportPacketBuilder(
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

          case DATA_RELIABLE: {
            final int sequence =
              p.getDataReliable().getId().getSequence();
            this.sent_ack_waiting.put(sequence, p);
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
            this.sent_ack_waiting.put(sequence, p);
            this.connection.listener.onSendPacketReliableFragment(
              this.connection, this.channel, sequence, size);
            break;
          }
        }
      }
    }

    public void handleReceives()
    {

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

    public void receive(
      final CoPacket p)
    {
      this.q_receive.add(p);
    }
  }
}
