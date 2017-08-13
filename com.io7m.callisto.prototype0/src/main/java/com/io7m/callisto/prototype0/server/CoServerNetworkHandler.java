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

import com.io7m.callisto.prototype0.events.CoEventNetworkSerializerRegistryType;
import com.io7m.callisto.prototype0.events.CoEventNetworkSerializerType;
import com.io7m.callisto.prototype0.events.CoEventNetworkType;
import com.io7m.callisto.prototype0.events.CoEventSerializationException;
import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.messages.CoPacket;
import com.io7m.callisto.prototype0.network.CoNetworkPacketSocketType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolMessages;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionUsableType;
import com.io7m.callisto.prototype0.transport.CoTransportServer;
import com.io7m.callisto.prototype0.transport.CoTransportServerConfiguration;
import com.io7m.callisto.prototype0.transport.CoTransportServerListenerType;
import com.io7m.jnull.NullCheck;
import io.reactivex.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.Map;
import java.util.Properties;

import static com.io7m.callisto.prototype0.transport.CoTransportConnectionUsableType.Reliability;

public final class CoServerNetworkHandler
  implements Closeable, CoTransportServerListenerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoServerNetworkHandler.class);

  private final CoNetworkPacketSocketType peer;
  private final CoTransportServer server;
  private final CoStringConstantPoolReadableType strings;
  private final CoEventServiceType events;
  private final CoEventNetworkSerializerRegistryType event_serializers;
  private final Scheduler scheduler;
  private final Clock clock;

  public CoServerNetworkHandler(
    final Clock in_clock,
    final Scheduler in_scheduler,
    final CoNetworkProviderType in_network,
    final CoEventNetworkSerializerRegistryType in_event_serializers,
    final CoEventServiceType in_events,
    final CoStringConstantPoolReadableType in_strings,
    final Properties props,
    final CoTransportServerConfiguration config)
  {
    NullCheck.notNull(in_network, "Network");

    this.clock =
      NullCheck.notNull(in_clock, "Clock");
    this.scheduler =
      NullCheck.notNull(in_scheduler, "Scheduler");
    this.strings =
      NullCheck.notNull(in_strings, "Strings");
    this.events =
      NullCheck.notNull(in_events, "Events");
    this.event_serializers =
      NullCheck.notNull(in_event_serializers, "Event serializers");
    this.peer =
      in_network.createSocket(props);
    this.server =
      new CoTransportServer(in_clock, in_strings, this, this.peer, config);
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
  public void onClientConnectionCreated(
    final CoTransportConnectionUsableType connection)
  {
    LOG.info("onConnectionCreated: {}", connection);
    this.sendInitialStringTable(connection);
    this.events.post(CoServerNetworkEventConnected.of(
      connection.id(),
      connection.remote()));
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
      CoStringConstantPoolMessages.createEventUpdateCompressedSerialized(xs));
  }

  @Override
  public void onClientConnectionClosed(
    final CoTransportConnectionUsableType connection,
    final String message)
  {
    LOG.info("onConnectionClosed: {}", connection);
  }

  @Override
  public void onClientConnectionTimedOut(
    final CoTransportConnectionUsableType connection)
  {
    LOG.error("onClientConnectionTimedOut: {}", connection);
    this.events.post(CoServerNetworkEventDisconnected.of(
      connection.id(), connection.remote()));
  }

  @Override
  public void onClientConnectionPacketSendReliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketSendUnreliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketSendReliableFragment(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketSendAck(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketDropUnreliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionMessageReceived(
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
        "onClientConnectionMessageReceived: {}: could not deserialize event: type {} size {}: ",
        connection,
        type_name,
        Integer.valueOf(data.remaining()),
        e);
    }
  }

  @Override
  public void onClientConnectionPacketIgnoredBye(
    final CoTransportConnectionUsableType connection,
    final SocketAddress sender)
  {
    LOG.warn(
      "onClientConnectionPacketIgnoredBye: {}: ignored a Bye message sent from an unknown sender {}",
      connection,
      sender);
  }

  @Override
  public void onClientConnectionPacketReceiveDeliverReliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketReceiveDeliverUnreliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketReceiveReliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketReceiveUnreliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketReceivePing(
    final CoTransportConnectionUsableType connection)
  {

  }

  @Override
  public void onClientConnectionPacketReceivePong(
    final CoTransportConnectionUsableType connection)
  {

  }

  @Override
  public void onClientConnectionPacketReceiveReliableFragment(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketReceiveAck(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketSendReliableSaved(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketSendReliableExpired(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onClientConnectionPacketSendPong(
    final CoTransportConnectionUsableType connection)
  {

  }

  @Override
  public void onClientConnectionPacketSendPing(
    final CoTransportConnectionUsableType connection)
  {

  }
}
