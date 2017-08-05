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
import com.io7m.callisto.prototype0.messages.CoHello;
import com.io7m.callisto.prototype0.messages.CoHelloResponse;
import com.io7m.callisto.prototype0.messages.CoHelloResponseError;
import com.io7m.callisto.prototype0.messages.CoHelloResponseOK;
import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSocketType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public final class CoTransportServer implements CoTransportServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoTransportServer.class);

  private final CoNetworkPacketSocketType socket;
  private final ListenerType listener;
  private final byte[] password;
  private final CoIDPoolUnpredictable connection_id_pool;
  private final CoStringConstantPoolReadableType strings;
  private final Int2ReferenceOpenHashMap<CoTransportConnection> connections;

  public CoTransportServer(
    final CoStringConstantPoolReadableType in_strings,
    final byte[] in_password,
    final ListenerType in_listener,
    final CoNetworkPacketSocketType in_socket)
  {
    this.strings =
      NullCheck.notNull(in_strings, "Strings");
    this.password =
      NullCheck.notNull(in_password, "Password");
    this.listener =
      NullCheck.notNull(in_listener, "Listener");
    this.socket =
      NullCheck.notNull(in_socket, "Socket");

    this.connection_id_pool =
      new CoIDPoolUnpredictable();
    this.connections =
      new Int2ReferenceOpenHashMap<>();
  }

  private static int packetConnectionId(
    final CoPacket p)
  {
    switch (p.getValueCase()) {
      case HELLO:
      case HELLO_RESPONSE:
      case VALUE_NOT_SET: {
        break;
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

  private static ByteBuffer helloOK(
    final int connection_id)
  {
    final CoHelloResponseOK hr_ok =
      CoHelloResponseOK.newBuilder()
        .setConnectionId(connection_id)
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
      case VALUE_NOT_SET: {
        this.listener.onReceivePacketUnrecognized(address, p);
        break;
      }
      case HELLO_RESPONSE: {
        this.listener.onReceivePacketUnexpected(address, p);
        break;
      }

      case DATA_RELIABLE:
      case DATA_UNRELIABLE:
      case DATA_RELIABLE_FRAGMENT: {
        this.onReceiveConnectionPacket(packetConnectionId(p), p);
        break;
      }
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

    if (this.password.length > 0) {
      final ByteString received_password = hello.getPassword();
      if (!comparePasswords(this.password, received_password.toByteArray())) {
        LOG.trace("{}: received bad password", address);
        this.socket.send(address, helloBadPassword());
        return;
      }
    }

    LOG.trace("{}: received good password", address);

    final int connection_id = this.connection_id_pool.fresh();
    final CoTransportConnection.ListenerType connection_listener =
      new ConnectionListener(this, address, connection_id);

    final CoTransportConnection connection =
      new CoTransportConnection(
        connection_listener,
        this.strings,
        this.socket,
        address,
        connection_id);

    this.connections.put(connection_id, connection);
    this.socket.send(address, helloOK(connection_id));
    this.listener.onConnectionCreated(connection);
  }

  private void onConnectionClosed(
    final CoTransportConnectionUsableType connection)
  {
    this.connections.remove(connection.id());
    this.listener.onConnectionClosed(connection);
  }

  private static final class ConnectionListener
    implements CoTransportConnection.ListenerType
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
    public void onConnectionClosed(
      final CoTransportConnectionUsableType connection)
    {
      this.server.onConnectionClosed(connection);
    }

    @Override
    public void onConnectionTimedOut(
      final CoTransportConnectionUsableType connection)
    {
      this.server.onConnectionClosed(connection);
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
          "onSendPacketUnreliable: [{}:{}] sequence {}: {} octets",
          Integer.toUnsignedString(this.connection_id, 16),
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
  }
}
