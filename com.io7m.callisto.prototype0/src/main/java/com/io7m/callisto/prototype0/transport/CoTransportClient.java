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
import com.io7m.jranges.RangeCheck;
import com.io7m.jranges.RangeInclusiveI;
import com.io7m.junreachable.UnimplementedCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public final class CoTransportClient implements CoTransportClientType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransportClient.class);

  private final CoStringConstantPoolReadableType strings;
  private final byte[] password;
  private final ListenerType listener;
  private final CoNetworkPacketSocketType socket;
  private final FSMEnumMutable<State> state;
  private final SocketAddress remote;
  private final int ticks_per_second;
  private final ConnectionListener connection_listener;
  private int hello_attempts;
  private long time;
  private CoTransportConnection connection;

  public CoTransportClient(
    final CoStringConstantPoolReadableType in_strings,
    final byte[] in_password,
    final ListenerType in_listener,
    final CoNetworkPacketSocketType in_socket,
    final int in_ticks_per_second)
  {
    this.strings =
      NullCheck.notNull(in_strings, "Strings");
    this.password =
      NullCheck.notNull(in_password, "Password");
    this.listener =
      NullCheck.notNull(in_listener, "Listener");
    this.socket =
      NullCheck.notNull(in_socket, "Socket");
    this.remote =
      this.socket.remote().get();

    this.ticks_per_second =
      RangeCheck.checkIncludedInInteger(
        in_ticks_per_second,
        "Ticks per second",
        new RangeInclusiveI(1, 60),
        "Valid ticks per second");

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

    this.socket.send(this.remote, hello(this.password));
    ++this.hello_attempts;
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
        if (this.hello_attempts == 10) {
          this.state.transition(State.STATE_DISCONNECTED);
          this.listener.onConnectionTimedOut(
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
    return this.time % (3L * (long) this.ticks_per_second) == 0L;
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
      this.listener.onReceivePacketUnparseable(address, data, e);
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
        this.listener.onReceivePacketUnexpected(address, p);
        break;
      }

      case VALUE_NOT_SET: {
        this.listener.onReceivePacketUnrecognized(address, p);
        break;
      }

      case HELLO_RESPONSE: {
        this.onReceivePacketHelloResponse(address, p);
        break;
      }

      case DATA_RECEIPT:
      case DATA_RELIABLE:
      case DATA_UNRELIABLE:
      case DATA_RELIABLE_FRAGMENT: {
        this.onReceiveConnectionPacket(address, p);
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
        this.listener.onReceivePacketUnexpected(address, p);
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
        this.connection =
          new CoTransportConnection(
            this.connection_listener,
            this.strings,
            this.socket,
            address,
            ok.getConnectionId());
        this.state.transition(State.STATE_CONNECTED);
        this.listener.onConnectionCreated(this.connection);
        break;
      }

      case ERROR: {
        this.state.transition(State.STATE_DISCONNECTED);
        this.listener.onConnectionRefused(
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
        this.listener.onReceivePacketUnrecognized(address, p);
        this.listener.onConnectionRefused(
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
    implements CoTransportConnection.ListenerType
  {
    private final CoTransportClient client;

    public ConnectionListener(
      final CoTransportClient in_client)
    {
      this.client = NullCheck.notNull(in_client, "Client");
    }

    @Override
    public void onConnectionClosed(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onConnectionClosed: {}", connection);
      }
    }

    @Override
    public void onConnectionTimedOut(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onConnectionTimedOut: {}", connection);
      }

      this.client.listener.onConnectionTimedOut(
        connection.remote(), "Timed out");
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
    public void onEnqueuePacketReceipt(
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
    }

    @Override
    public void onSendPacketReceipt(
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
    }

    @Override
    public void onDropPacketUnreliable(
      final CoTransportConnectionUsableType connection,
      final int channel,
      final int sequence,
      final int size)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onDropPacketUnreliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }
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

      final int type_index =
        message.getMessageType().getValue();
      final CoStringConstantReference type_ref =
        CoStringConstantReference.of(type_index);
      final String type_name =
        this.client.strings.lookupString(type_ref);

      if (LOG.isTraceEnabled()) {
        LOG.trace(
          "onMessageReceived: {}:{} message type {}",
          connection,
          Integer.valueOf(channel),
          type_name);
      }

      this.client.listener.onMessageReceived(
        connection,
        channel,
        type_name,
        message.getMessageData().asReadOnlyByteBuffer());
    }
  }
}
