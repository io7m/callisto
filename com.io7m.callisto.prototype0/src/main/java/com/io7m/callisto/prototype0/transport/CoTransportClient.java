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
import com.io7m.callisto.prototype0.messages.CoBye;
import com.io7m.callisto.prototype0.messages.CoHello;
import com.io7m.callisto.prototype0.messages.CoHelloResponse;
import com.io7m.callisto.prototype0.messages.CoHelloResponseOK;
import com.io7m.callisto.prototype0.messages.CoMessage;
import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSocketType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantReference;
import com.io7m.jfsm.core.FSMEnumMutable;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class CoTransportClient implements CoTransportClientType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransportClient.class);

  private final CoStringConstantPoolReadableType strings;
  private final CoTransportClientListenerType listener;
  private final CoNetworkPacketSocketType socket;
  private final FSMEnumMutable<State> state;
  private final SocketAddress remote;
  private final ConnectionListener connection_listener;
  private final CoTransportClientConfiguration config;
  private final Clock clock;
  private int hello_attempts;
  private long time;
  private CoTransportConnection connection;

  public CoTransportClient(
    final Clock in_clock,
    final CoStringConstantPoolReadableType in_strings,
    final CoTransportClientListenerType in_listener,
    final CoNetworkPacketSocketType in_socket,
    final CoTransportClientConfiguration in_config)
  {
    this.clock =
      NullCheck.notNull(in_clock, "Clock");
    this.strings =
      NullCheck.notNull(in_strings, "Strings");
    this.listener =
      NullCheck.notNull(in_listener, "Listener");
    this.socket =
      NullCheck.notNull(in_socket, "Socket");
    this.config =
      NullCheck.notNull(in_config, "Configuration");

    this.remote =
      this.socket.remote().get();

    this.state =
      FSMEnumMutable.builder(State.STATE_INITIAL)
        .addTransition(State.STATE_INITIAL, State.STATE_WAITING_FOR_HELLO)
        .addTransition(State.STATE_WAITING_FOR_HELLO, State.STATE_CONNECTED)
        .addTransition(State.STATE_WAITING_FOR_HELLO, State.STATE_DISCONNECTED)
        .addTransition(State.STATE_CONNECTED, State.STATE_DISCONNECTED)
        .build();

    this.connection_listener =
      new ConnectionListener(this);
  }

  private static ByteBuffer hello(
    final byte[] password)
  {
    return ByteBuffer.wrap(
      CoPacket.newBuilder()
        .setHello(CoHello.newBuilder().setPassword(ByteString.copyFrom(password)))
        .build()
        .toByteArray());
  }

  @Override
  public void start()
  {
    switch (this.state.current()) {
      case STATE_INITIAL: {
        this.sendHello();
        this.state.transition(State.STATE_WAITING_FOR_HELLO);
        break;
      }
      case STATE_WAITING_FOR_HELLO:
      case STATE_CONNECTED:
      case STATE_DISCONNECTED: {
        throw new UnimplementedCodeException();
      }
    }
  }

  private void sendHello()
  {
    if (LOG.isTraceEnabled()) {
      LOG.trace("{}: sending hello packet", this.remote);
    }

    this.socket.send(this.remote, hello(this.config.password()));
    ++this.hello_attempts;
    this.listener.onHelloSend(
      this.remote,
      this.hello_attempts,
      this.config.helloRetryCount());
  }

  @Override
  public void tick()
  {
    ++this.time;

    switch (this.state.current()) {
      case STATE_DISCONNECTED: {
        return;
      }

      case STATE_WAITING_FOR_HELLO: {
        if (this.hello_attempts == this.config.helloRetryCount()) {
          this.state.transition(State.STATE_DISCONNECTED);
          this.listener.onHelloTimedOut(
            this.remote,
            String.format(
              "Could not establish a connection to the server after %d attempts",
              Integer.valueOf(this.hello_attempts)));
          return;
        }

        if (this.timeToRetryHello()) {
          this.sendHello();
        }
        break;
      }

      case STATE_INITIAL:
      case STATE_CONNECTED: {
        this.connection.tick();
        break;
      }
    }

    this.socket.poll(this::onReceivePacket);
  }

  private boolean timeToRetryHello()
  {
    return (this.time % (long) this.config.helloRetryDelayInTicks()) == 0L;
  }

  private void onReceivePacket(
    final SocketAddress address,
    final ByteBuffer data)
  {
    NullCheck.notNull(address, "Address");
    NullCheck.notNull(data, "Data");

    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "{}: received {} octets",
        address,
        Integer.valueOf(data.remaining()));
    }

    final CoPacket p;
    try {
      p = CoPacket.parseFrom(data);
    } catch (final Exception e) {
      this.listener.onPacketReceiveUnparseable(address, data, e);
      return;
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "{}: received {} packet",
        address,
        p.getValueCase());
    }

    switch (p.getValueCase()) {
      case HELLO: {
        this.listener.onPacketReceiveUnexpected(address, p);
        break;
      }

      case BYE: {
        this.onReceiveBye(address, p);
        break;
      }

      case VALUE_NOT_SET: {
        this.listener.onPacketReceiveUnrecognized(address, p);
        break;
      }

      case HELLO_RESPONSE: {
        this.onReceivePacketHelloResponse(address, p);
        break;
      }

      case PING:
      case PONG:
      case DATA_ACK:
      case DATA_RELIABLE:
      case DATA_UNRELIABLE:
      case DATA_RELIABLE_FRAGMENT: {
        this.onReceiveConnectionPacket(address, p);
        break;
      }
    }
  }

  private void onReceiveBye(
    final SocketAddress address,
    final CoPacket p)
  {
    switch (this.state.current()) {
      case STATE_INITIAL:
      case STATE_DISCONNECTED: {
        this.listener.onPacketReceiveUnexpected(address, p);
        break;
      }

      case STATE_WAITING_FOR_HELLO:
      case STATE_CONNECTED: {
        final CoBye b = p.getBye();
        if (Objects.equals(address, this.remote)) {
          this.state.transition(State.STATE_DISCONNECTED);
          this.listener.onConnectionClosed(this.connection, b.getMessage());
          return;
        }
        break;
      }
    }
  }

  private void onReceiveConnectionPacket(
    final SocketAddress address,
    final CoPacket p)
  {
    switch (this.state.current()) {
      case STATE_INITIAL:
      case STATE_WAITING_FOR_HELLO:
      case STATE_DISCONNECTED: {
        this.listener.onPacketReceiveUnexpected(address, p);
        break;
      }

      case STATE_CONNECTED: {
        this.connection.receive(p);
        break;
      }
    }
  }

  private void onReceivePacketHelloResponse(
    final SocketAddress address,
    final CoPacket p)
  {
    final CoHelloResponse pr = p.getHelloResponse();
    switch (pr.getValueCase()) {

      case OK: {
        final CoHelloResponseOK ok = pr.getOk();

        final CoTransportConnectionConfiguration connection_config =
          CoTransportConnectionConfiguration.builder()
            .setTicksPerSecond(this.config.ticksPerSecond())
            .setTicksTimeout(this.config.ticksTimeout())
            .build();

        this.connection =
          new CoTransportConnection(
            this.clock,
            this.connection_listener,
            this.strings,
            this.socket,
            connection_config,
            address,
            ok.getConnectionId());
        this.state.transition(State.STATE_CONNECTED);
        this.listener.onConnectionCreated(this.connection);
        break;
      }

      case ERROR: {
        this.state.transition(State.STATE_DISCONNECTED);
        this.listener.onHelloRefused(
          address,
          new StringBuilder(128)
            .append("Connection refused.")
            .append(System.lineSeparator())
            .append("  Server responded: ")
            .append(pr.getError().getMessage())
            .append(System.lineSeparator())
            .toString());
        break;
      }

      case VALUE_NOT_SET: {
        this.state.transition(State.STATE_DISCONNECTED);
        this.listener.onPacketReceiveUnrecognized(address, p);
        this.listener.onHelloRefused(
          address,
          new StringBuilder(128)
            .append("Connection refused.")
            .append(System.lineSeparator())
            .append(
              "  Server responded with an unrecognized type of HelloResponse packet")
            .append(pr.getError().getMessage())
            .append(System.lineSeparator())
            .toString());
        break;
      }
    }
  }

  @Override
  public void close()
    throws IOException
  {

  }

  private enum State
  {
    STATE_INITIAL,
    STATE_WAITING_FOR_HELLO,
    STATE_CONNECTED,
    STATE_DISCONNECTED
  }

  private static final class ConnectionListener
    implements CoTransportConnectionListenerType
  {
    private final CoTransportClient client;

    public ConnectionListener(
      final CoTransportClient in_client)
    {
      this.client = NullCheck.notNull(in_client, "Client");
    }

    @Override
    public void onClosed(
      final CoTransportConnectionUsableType connection,
      final String message)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onClosed: {}: {}", connection, message);
      }

      this.client.listener.onConnectionClosed(connection, message);
    }

    @Override
    public void onTimedOut(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onTimedOut: {}", connection);
      }

      this.client.listener.onConnectionTimedOut(connection);
    }

    @Override
    public void onEnqueuePacketReliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onEnqueuePacketReliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }
    }

    @Override
    public void onEnqueuePacketReliableRequeue(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onEnqueuePacketReliableRequeue: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }
    }

    @Override
    public void onEnqueuePacketUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onEnqueuePacketUnreliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }
    }

    @Override
    public void onEnqueuePacketReliableFragment(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onEnqueuePacketReliableFragment: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }
    }

    @Override
    public void onEnqueuePacketAck(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onEnqueuePacketReceipt: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }
    }

    @Override
    public void onSendPacketReliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onSendPacketReliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionSendReliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onSendPacketUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onSendPacketUnreliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionSendUnreliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onSendPacketReliableFragment(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onSendPacketReliableFragment: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionSendReliableFragment(
        connection, channel, sequence, size);
    }

    @Override
    public void onSendPacketAck(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onSendPacketReceipt: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionSendReceipt(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceiveDropPacketUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onReceiveDropPacketUnreliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionReceiveDropUnreliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onMessageReceived(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final CoMessage message)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onMessageReceived: {}:{} {}",
          connection,
          Integer.valueOf(channel),
          message);
      }

      final CoStringConstantReference type_ref =
        CoStringConstantReference.of(message.getMessageType().getValue());
      final Optional<String> type_name_opt =
        this.client.strings.lookupString(type_ref);

      if (!type_name_opt.isPresent()) {
        LOG.error(
          "onMessageReceived: {}:{} unrecognized string constant {}",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(type_ref.value()));
        return;
      }

      final String type_name = type_name_opt.get();
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onMessageReceived: {}:{} message type {}",
          connection,
          Integer.valueOf(channel),
          type_name);
      }

      this.client.listener.onConnectionMessageReceived(
        connection,
        channel,
        type_name,
        message.getMessageData().asReadOnlyByteBuffer());
    }

    @Override
    public void onReceivePacketDeliverReliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onReceivePacketDeliverReliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionReceiveDeliverReliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketDeliverUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onReceivePacketDeliverUnreliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionReceiveDeliverUnreliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketReliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onReceivePacketReliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionReceiveReliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onReceivePacketUnreliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionReceiveUnreliable(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketReliableFragment(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onReceivePacketReliableFragment: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionReceiveReliableFragment(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketAck(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onReceivePacketAck: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionReceiveAck(
        connection, channel, sequence, size);
    }

    @Override
    public void onSavedPacketReliableSave(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onSavedPacketReliableSave: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionSendReliableSaved(
        connection, channel, sequence, size);
    }

    @Override
    public void onSavedPacketReliableExpire(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onSavedPacketReliableExpire: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.client.listener.onConnectionSendReliableExpired(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketPing(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onReceivePacketPing: {}", connection);
      }

      this.client.listener.onConnectionReceivePing(connection);
    }

    @Override
    public void onSendPacketPong(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onSendPacketPong: {}", connection);
      }

      this.client.listener.onConnectionSendPong(connection);
    }

    @Override
    public void onReceivePacketPong(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onReceivePacketPong: {}", connection);
      }

      this.client.listener.onConnectionReceivePong(connection);
    }

    @Override
    public void onSendPacketPing(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onSendPacketPing: {}", connection);
      }

      this.client.listener.onConnectionSendPing(connection);
    }
  }
}
