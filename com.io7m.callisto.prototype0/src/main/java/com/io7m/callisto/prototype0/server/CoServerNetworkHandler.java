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

package com.io7m.callisto.prototype0.server;

import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSocketType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolMessages;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionUsableType;
import com.io7m.callisto.prototype0.transport.CoTransportServer;
import com.io7m.jnull.NullCheck;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;

import static com.io7m.callisto.prototype0.transport.CoTransportConnectionUsableType.Reliability;

public final class CoServerNetworkHandler
  implements Closeable, CoTransportServer.ListenerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoServerNetworkHandler.class);

  private final CoNetworkPacketSocketType peer;
  private final CoTransportServer server;
  private final CoStringConstantPoolReadableType strings;

  public CoServerNetworkHandler(
    final CoNetworkProviderType in_network,
    final CoStringConstantPoolReadableType in_strings,
    final byte[] in_password,
    final Properties props)
  {
    NullCheck.notNull(in_network, "Network");
    this.strings = NullCheck.notNull(in_strings, "Strings");
    this.peer = in_network.createSocket(props);
    this.server =
      new CoTransportServer(in_strings, in_password, this, this.peer);
  }

  public void tick()
  {
    this.server.tick();
  }

  @Override
  public void close()
    throws IOException
  {
    this.server.close();
    this.peer.close();
  }

  @Override
  public void onReceivePacketUnparseable(
    final SocketAddress address,
    final ByteBuffer data,
    final Exception e)
  {
    LOG.error("onReceivePacketUnparseable: {}: ", address, e);
  }

  @Override
  public void onReceivePacketUnrecognized(
    final SocketAddress address,
    final CoPacket packet)
  {
    LOG.error("onReceivePacketUnrecognized: {}: {}", address, packet);
  }

  @Override
  public void onReceivePacketUnexpected(
    final SocketAddress address,
    final CoPacket packet)
  {
    LOG.error("onReceivePacketUnexpected: {}: {}", address, packet);
  }

  @Override
  public void onConnectionCreated(
    final CoTransportConnectionUsableType connection)
  {
    LOG.info("onConnectionCreated: {}", connection);
    this.sendInitialStringTable(connection);
  }

  private void sendInitialStringTable(
    final CoTransportConnectionUsableType connection)
  {
    final Map<Integer, String> xs = this.strings.view();
    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "sending initial string table ({} strings)",
        Integer.valueOf(xs.size()));
    }

    connection.send(
      Reliability.MESSAGE_RELIABLE,
      0,
      CoStringConstantPoolMessages.eventCompressedUpdateTypeName(),
      CoStringConstantPoolMessages.eventCompressedUpdateSerialized(xs));
  }

  @Override
  public void onConnectionClosed(
    final CoTransportConnectionUsableType connection)
  {
    LOG.info("onConnectionClosed: {}", connection);
  }
}
