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

package com.io7m.callisto.prototype0.client;

import com.io7m.callisto.prototype0.events.CoEventNetworkSerializerRegistryType;
import com.io7m.callisto.prototype0.events.CoEventNetworkSerializerType;
import com.io7m.callisto.prototype0.events.CoEventNetworkType;
import com.io7m.callisto.prototype0.events.CoEventSerializationException;
import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSocketType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.callisto.prototype0.transport.CoTransportClient;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionUsableType;
import com.io7m.jnull.NullCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Properties;

public final class CoClientNetworkHandler
  implements Closeable, CoTransportClient.ListenerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoClientNetworkHandler.class);

  private final CoNetworkPacketSocketType peer;
  private final CoTransportClient client;
  private final CoEventServiceType events;
  private final CoEventNetworkSerializerRegistryType event_serializers;

  public CoClientNetworkHandler(
    final CoEventServiceType in_events,
    final CoEventNetworkSerializerRegistryType in_event_serializers,
    final CoNetworkProviderType in_network,
    final CoStringConstantPoolReadableType in_strings,
    final byte[] in_password,
    final Properties props)
  {
    this.events =
      NullCheck.notNull(in_events, "Events");
    this.event_serializers =
      NullCheck.notNull(in_event_serializers, "Event serializers");
    this.peer =
      NullCheck.notNull(in_network, "Network").createSocket(props);
    this.client =
      new CoTransportClient(
        in_strings, in_password, this, this.peer, 30);
  }

  @Override
  public void onReceivePacketUnparseable(
    final SocketAddress address,
    final ByteBuffer data,
    final Exception e)
  {
    LOG.error(
      "onReceivePacketUnparseable: {}: {} octets: ",
      address,
      Integer.valueOf(data.remaining()),
      e);
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
    this.events.post(CoClientNetworkEventConnected.of(connection.id()));
  }

  @Override
  public void onMessageReceived(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final String type_name,
    final ByteBuffer data)
  {
    try {
      final CoEventNetworkSerializerType serializer =
        this.event_serializers.lookupSerializer(type_name);
      final CoEventNetworkType event =
        serializer.eventDeserialize(data);
      this.events.post(event);
    } catch (final CoEventSerializationException e) {
      LOG.error(
        "could not deserialize event: type {} size {}: ",
        type_name,
        Integer.valueOf(data.remaining()), e);
    }
  }

  @Override
  public void onConnectionDisconnected(
    final CoTransportConnectionUsableType connection,
    final String message)
  {
    LOG.info("onConnectionCreated: {}: {}", connection, message);
    this.events.post(CoClientNetworkEventDisconnected.of(connection.id()));
  }

  @Override
  public void onConnectionTimedOut(
    final SocketAddress address,
    final String message)
  {
    LOG.error("onConnectionTimedOut: {}: {}", address, message);
    this.events.post(CoClientNetworkEventConnectionTimedOut.of(message));
  }

  @Override
  public void onConnectionRefused(
    final SocketAddress address,
    final String message)
  {
    LOG.error("onConnectionRefused: {}: {}", address, message);
    this.events.post(CoClientNetworkEventConnectionRefused.of(message));
  }

  @Override
  public void close()
    throws IOException
  {
    this.client.close();
  }

  public void start()
  {
    this.client.start();
  }

  public void tick()
  {
    this.client.tick();
  }
}
