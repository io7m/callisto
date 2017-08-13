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
import com.io7m.callisto.prototype0.idpool.CoIDPoolUnpredictable;
import com.io7m.callisto.prototype0.messages.CoBye;
import com.io7m.callisto.prototype0.messages.CoHello;
import com.io7m.callisto.prototype0.messages.CoHelloResponse;
import com.io7m.callisto.prototype0.messages.CoHelloResponseError;
import com.io7m.callisto.prototype0.messages.CoHelloResponseOK;
import com.io7m.callisto.prototype0.messages.CoMessage;
import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSocketType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantReference;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

public final class CoTransportServer implements CoTransportServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransportServer.class);

  private final CoNetworkPacketSocketType socket;
  private final CoTransportServerListenerType listener;
  private final CoIDPoolUnpredictable connection_id_pool;
  private final CoStringConstantPoolReadableType strings;
  private final Int2ReferenceOpenHashMap<CoTransportConnection> connections;
  private final CoTransportServerConfiguration config;
  private final Clock clock;

  public CoTransportServer(
    final Clock in_clock,
    final CoStringConstantPoolReadableType in_strings,
    final CoTransportServerListenerType in_listener,
    final CoNetworkPacketSocketType in_socket,
    final CoTransportServerConfiguration in_config)
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
      NullCheck.notNull(in_config, "Config");

    this.connection_id_pool =
      new CoIDPoolUnpredictable();
    this.connections =
      new Int2ReferenceOpenHashMap<>();
  }

  private static int packetConnectionId(
    final CoPacket p)
  {
    switch (p.getValueCase()) {
      case BYE:
      case HELLO:
      case HELLO_RESPONSE:
      case VALUE_NOT_SET: {
        break;
      }

      case PING: {
        return p.getPing().getConnectionId();
      }
      case PONG: {
        return p.getPong().getConnectionId();
      }
      case DATA_ACK: {
        return p.getDataAck().getId().getConnectionId();
      }
      case DATA_RELIABLE: {
        return p.getDataReliable().getId().getConnectionId();
      }
      case DATA_UNRELIABLE: {
        return p.getDataUnreliable().getId().getConnectionId();
      }
      case DATA_RELIABLE_FRAGMENT: {
        return p.getDataReliableFragment().getId().getConnectionId();
      }
    }

    throw new UnreachableCodeException();
  }

  private static ByteBuffer helloBadPassword()
  {
    final CoHelloResponseError hr_error =
      CoHelloResponseError.newBuilder()
        .setMessage("Incorrect password.")
        .build();

    final CoHelloResponse hr =
      CoHelloResponse.newBuilder()
        .setError(hr_error)
        .build();

    final CoPacket p =
      CoPacket.newBuilder()
        .setHelloResponse(hr)
        .build();

    return ByteBuffer.wrap(p.toByteArray());
  }

  private static boolean comparePasswords(
    final byte[] a,
    final byte[] b)
  {
    if (a.length != b.length) {
      return false;
    }

    boolean matches = true;
    for (int i = 0; i < a.length; i++) {
      matches &= a[i] == b[i];
    }
    return matches;
  }

  private static ByteBuffer bye(
    final int id,
    final String message)
  {
    final CoBye bye =
      CoBye.newBuilder()
        .setConnectionId(id)
        .setMessage(message)
        .build();

    final CoPacket p =
      CoPacket.newBuilder()
        .setBye(bye)
        .build();

    return ByteBuffer.wrap(p.toByteArray());
  }

  private ByteBuffer helloOK(
    final int connection_id)
  {
    final CoHelloResponseOK hr_ok =
      CoHelloResponseOK.newBuilder()
        .setConnectionId(connection_id)
        .setTicksPerSecond(this.config.ticksPerSecond())
        .setTicksReliableTtl(this.config.ticksReliableTTL())
        .setTicksTimeout(this.config.ticksTimeout())
        .build();

    final CoHelloResponse hr =
      CoHelloResponse.newBuilder()
        .setOk(hr_ok)
        .build();

    final CoPacket p =
      CoPacket.newBuilder()
        .setHelloResponse(hr)
        .build();

    return ByteBuffer.wrap(p.toByteArray());
  }

  @Override
  public void close()
    throws IOException
  {

  }

  @Override
  public void tick()
  {
    this.socket.poll(this::onReceivePacket);

    for (final int id : this.connections.keySet()) {
      final CoTransportConnection connection = this.connections.get(id);
      connection.tick();
    }
  }

  private void onReceivePacket(
    final SocketAddress address,
    final ByteBuffer data)
  {
    NullCheck.notNull(address, "Address");
    NullCheck.notNull(data, "Data");

    LOG.trace(
      "{}: received {} octets",
      address,
      Integer.valueOf(data.remaining()));

    final CoPacket p;
    try {
      p = CoPacket.parseFrom(data);
    } catch (final Exception e) {
      this.listener.onReceivePacketUnparseable(address, data, e);
      return;
    }

    switch (p.getValueCase()) {
      case HELLO: {
        this.onReceivedHello(address, p.getHello());
        break;
      }

      case BYE: {
        this.onReceivedBye(address, p.getBye());
        break;
      }

      case VALUE_NOT_SET: {
        this.listener.onReceivePacketUnrecognized(address, p);
        break;
      }

      case HELLO_RESPONSE: {
        this.listener.onReceivePacketUnexpected(address, p);
        break;
      }

      case PING:
      case PONG:
      case DATA_ACK:
      case DATA_RELIABLE:
      case DATA_UNRELIABLE:
      case DATA_RELIABLE_FRAGMENT: {
        this.onReceiveConnectionPacket(packetConnectionId(p), p);
        break;
      }
    }
  }

  private void onReceivedBye(
    final SocketAddress address,
    final CoBye bye)
  {
    final int connection_id = bye.getConnectionId();
    if (this.connections.containsKey(connection_id)) {
      final CoTransportConnection connection =
        this.connections.get(connection_id);

      if (Objects.equals(address, connection.remote())) {
        this.onConnectionClosed(connection, "Client closed the connection");
        return;
      }

      this.listener.onClientConnectionPacketIgnoredBye(connection, address);
    }
  }

  private void onReceiveConnectionPacket(
    final int connection_id,
    final CoPacket p)
  {
    if (this.connections.containsKey(connection_id)) {
      final CoTransportConnection connection =
        this.connections.get(connection_id);
      connection.receive(p);
    }
  }

  private void onReceivedHello(
    final SocketAddress address,
    final CoHello hello)
  {
    LOG.trace("{}: received hello packet", address);

    final byte[] password = this.config.password();
    if (password.length > 0) {
      final ByteString received_password = hello.getPassword();
      if (!comparePasswords(password, received_password.toByteArray())) {
        LOG.trace("{}: received bad password", address);
        this.socket.send(address, helloBadPassword());
        return;
      }
    }

    LOG.trace("{}: received good password", address);

    final int connection_id = this.connection_id_pool.fresh();
    final CoTransportConnectionListenerType connection_listener =
      new ConnectionListener(this, address, connection_id);

    final CoTransportConnectionConfiguration config =
      CoTransportConnectionConfiguration.builder()
        .setTicksPerSecond(this.config.ticksPerSecond())
        .setTicksTimeout(this.config.ticksTimeout())
        .build();

    final CoTransportConnection connection =
      new CoTransportConnection(
        this.clock,
        connection_listener,
        this.strings,
        this.socket,
        config,
        address,
        connection_id);

    this.connections.put(connection_id, connection);
    this.socket.send(address, this.helloOK(connection_id));
    this.listener.onClientConnectionCreated(connection);
  }

  private void onConnectionClosed(
    final CoTransportConnectionUsableType connection,
    final String message)
  {
    this.connections.remove(connection.id());
    this.listener.onClientConnectionClosed(connection, message);
  }

  private void onConnectionTimedOut(
    final CoTransportConnectionUsableType connection)
  {
    this.connections.remove(connection.id());
    this.listener.onClientConnectionTimedOut(connection);
  }

  @Override
  public void closeConnection(
    final int id,
    final String message)
  {
    NullCheck.notNull(message, "Message");

    if (this.connections.containsKey(id)) {
      final CoTransportConnection connection = this.connections.get(id);
      this.socket.send(connection.remote(), bye(id, message));
      this.onConnectionClosed(connection, message);
    }
  }

  private static final class ConnectionListener
    implements CoTransportConnectionListenerType
  {
    private final int connection_id;
    private final CoTransportServer server;
    private final SocketAddress address;

    ConnectionListener(
      final CoTransportServer in_server,
      final SocketAddress in_address,
      final int in_connection_id)
    {
      this.server = NullCheck.notNull(in_server, "Server");
      this.address = NullCheck.notNull(in_address, "Address");
      this.connection_id = in_connection_id;
    }

    @Override
    public void onClosed(
      final CoTransportConnectionUsableType connection,
      final String message)
    {
      this.server.onConnectionClosed(connection, message);
    }

    @Override
    public void onTimedOut(
      final CoTransportConnectionUsableType connection)
    {
      this.server.onConnectionTimedOut(connection);
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

      this.server.listener.onClientConnectionPacketSendReliable(
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
          "onSendPacketUnreliable: [{}:{}] sequence {}: {} octets",
          Integer.toUnsignedString(this.connection_id, 16),
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.server.listener.onClientConnectionPacketSendUnreliable(
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

      this.server.listener.onClientConnectionPacketSendReliableFragment(
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
          "onSendPacketReceipts: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.server.listener.onClientConnectionPacketSendAck(
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
          "onDropPacketUnreliable: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.server.listener.onClientConnectionPacketDropUnreliable(
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
          "onMessageReceived: {}: {}",
          connection,
          Integer.valueOf(channel),
          message);
      }

      final CoStringConstantReference type_ref =
        CoStringConstantReference.of(message.getMessageType().getValue());
      final Optional<String> type_name_opt =
        this.server.strings.lookupString(type_ref);

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

      this.server.listener.onClientConnectionMessageReceived(
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

      this.server.listener.onClientConnectionPacketReceiveDeliverReliable(
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

      this.server.listener.onClientConnectionPacketReceiveDeliverUnreliable(
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

      this.server.listener.onClientConnectionPacketReceiveReliable(
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

      this.server.listener.onClientConnectionPacketReceiveUnreliable(
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

      this.server.listener.onClientConnectionPacketReceiveReliableFragment(
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
          "onReceivePacketReceipt: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.server.listener.onClientConnectionPacketReceiveAck(
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

      this.server.listener.onClientConnectionPacketSendReliableSaved(
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
          "onSavedPacketReliablePurge: {}:{} sequence {}: {} octets",
          connection,
          Integer.valueOf(channel),
          Integer.valueOf(sequence),
          Integer.valueOf(size));
      }

      this.server.listener.onClientConnectionPacketSendReliableExpired(
        connection, channel, sequence, size);
    }

    @Override
    public void onReceivePacketPing(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onReceivePacketPing: {}", connection);
      }

      this.server.listener.onClientConnectionPacketReceivePing(connection);
    }

    @Override
    public void onSendPacketPong(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onSendPacketPong: {}", connection);
      }

      this.server.listener.onClientConnectionPacketSendPong(connection);
    }

    @Override
    public void onReceivePacketPong(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onReceivePacketPong: {}", connection);
      }

      this.server.listener.onClientConnectionPacketReceivePong(connection);
    }

    @Override
    public void onSendPacketPing(
      final CoTransportConnectionUsableType connection)
    {
      if (LOG.isTraceEnabled()) {
        LOG.trace("onSendPacketPing: {}", connection);
      }

      this.server.listener.onClientConnectionPacketSendPing(connection);
    }
  }
}
