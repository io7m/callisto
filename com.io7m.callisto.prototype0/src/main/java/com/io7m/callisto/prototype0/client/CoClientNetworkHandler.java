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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
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
import com.io7m.callisto.prototype0.transport.CoTransportClientConfiguration;
import com.io7m.callisto.prototype0.transport.CoTransportClientListenerType;
import com.io7m.callisto.prototype0.transport.CoTransportConnection;
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
  implements Closeable, CoTransportClientListenerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoClientNetworkHandler.class);

  private final CoNetworkPacketSocketType peer;
  private final CoTransportClient client;
  private final CoEventServiceType events;
  private final CoEventNetworkSerializerRegistryType event_serializers;
  private final Metrics metrics;

  public CoClientNetworkHandler(
    final MetricRegistry in_metrics,
    final CoEventServiceType in_events,
    final CoEventNetworkSerializerRegistryType in_event_serializers,
    final CoNetworkProviderType in_network,
    final CoStringConstantPoolReadableType in_strings,
    final Properties props,
    final CoTransportClientConfiguration config)
  {
    this.metrics =
      new Metrics(NullCheck.notNull(in_metrics, "Metrics"));
    this.events =
      NullCheck.notNull(in_events, "Events");
    this.event_serializers =
      NullCheck.notNull(in_event_serializers, "Event serializers");
    this.peer =
      NullCheck.notNull(in_network, "Network").createSocket(props);
    this.client =
      new CoTransportClient(in_strings, this, this.peer, config);
  }

  @Override
  public void onPacketReceiveUnparseable(
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
  public void onPacketReceiveUnrecognized(
    final SocketAddress address,
    final CoPacket packet)
  {
    LOG.error("onReceivePacketUnrecognized: {}: {}", address, packet);
  }

  @Override
  public void onPacketReceiveUnexpected(
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
  public void onConnectionMessageReceived(
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
  public void onConnectionReceiveDropUnreliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {
    this.metrics.received_dropped_unreliable_packets.mark();
  }

  @Override
  public void onConnectionSendReceipt(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {
    this.metrics.sent_receipt_packets.mark();
    this.metrics.sent_receipt_octets.mark(Integer.toUnsignedLong(size));
  }

  @Override
  public void onConnectionSendReliableFragment(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onConnectionSendUnreliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {
    this.metrics.sent_unreliable_packets.mark();
    this.metrics.sent_unreliable_octets.mark(Integer.toUnsignedLong(size));
  }

  @Override
  public void onConnectionSendReliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {
    this.metrics.sent_reliable_packets.mark();
    this.metrics.sent_reliable_octets.mark(Integer.toUnsignedLong(size));
  }

  @Override
  public void onConnectionClosed(
    final CoTransportConnectionUsableType connection,
    final String message)
  {
    LOG.info("onConnectionClosed: {}: {}", connection, message);
    this.events.post(CoClientNetworkEventDisconnected.of(connection.id()));
  }

  @Override
  public void onConnectionTimedOut(
    final CoTransportConnectionUsableType connection)
  {
    LOG.error("onConnectionTimedOut: {}", connection);
    this.events.post(CoClientNetworkEventConnectionTimedOut.of(
      "Connection timed out"));
  }

  @Override
  public void onConnectionReceiveDeliverReliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onConnectionReceiveDeliverUnreliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onConnectionReceiveReliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {
    this.metrics.received_reliable_packets.mark();
    this.metrics.received_reliable_octets.mark(Integer.toUnsignedLong(size));
  }

  @Override
  public void onConnectionReceiveUnreliable(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {
    this.metrics.received_unreliable_packets.mark();
    this.metrics.received_unreliable_octets.mark(Integer.toUnsignedLong(size));
  }

  @Override
  public void onConnectionReceiveReliableFragment(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {

  }

  @Override
  public void onConnectionReceiveReceipt(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {
    this.metrics.received_receipt_packets.mark();
    this.metrics.received_receipt_octets.mark(Integer.toUnsignedLong(size));
  }

  @Override
  public void onConnectionSendReliableSaved(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {
    this.metrics.sent_reliable_saved_packets.inc();
    this.metrics.sent_reliable_saved_octets.inc(Integer.toUnsignedLong(size));
  }

  @Override
  public void onConnectionSendReliableExpired(
    final CoTransportConnectionUsableType connection,
    final int channel,
    final int sequence,
    final int size)
  {
    this.metrics.sent_reliable_saved_packets.dec();
    this.metrics.sent_reliable_saved_octets.dec(Integer.toUnsignedLong(size));
  }

  @Override
  public void onHelloTimedOut(
    final SocketAddress address,
    final String message)
  {
    LOG.error("onHelloTimedOut: {}: {}", address, message);
    this.events.post(CoClientNetworkEventConnectionTimedOut.of(message));
  }

  @Override
  public void onHelloSend(
    final SocketAddress address,
    final int attempt,
    final int max_attempts)
  {
    if (LOG.isDebugEnabled()) {
      LOG.debug(
        "onHelloSend: {}: {}/{}",
        address,
        Integer.valueOf(attempt),
        Integer.valueOf(max_attempts));
    }
  }

  @Override
  public void onHelloRefused(
    final SocketAddress address,
    final String message)
  {
    LOG.error("onHelloRefused: {}: {}", address, message);
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

  private static final class Metrics
  {
    private final Meter sent_reliable_packets;
    private final Meter sent_reliable_octets;
    private final Meter sent_receipt_packets;
    private final Meter sent_receipt_octets;
    private final Meter received_reliable_packets;
    private final Meter received_reliable_octets;
    private final Meter received_receipt_packets;
    private final Meter received_receipt_octets;
    private final Meter received_unreliable_packets;
    private final Meter received_unreliable_octets;
    private final Meter sent_unreliable_packets;
    private final Meter sent_unreliable_octets;
    private final Meter received_dropped_unreliable_packets;
    private final Counter sent_reliable_saved_packets;
    private final Counter sent_reliable_saved_octets;

    Metrics(
      final MetricRegistry metrics)
    {
      this.sent_reliable_packets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "sent_reliable"));
      this.sent_reliable_octets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "sent_reliable_octets"));

      this.sent_reliable_saved_packets =
        metrics.counter(MetricRegistry.name(
          CoClientNetworkHandler.class, "sent_reliable_saved"));
      this.sent_reliable_saved_octets =
        metrics.counter(MetricRegistry.name(
          CoClientNetworkHandler.class, "sent_reliable_saved_octets"));

      this.sent_unreliable_packets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "sent_unreliable"));
      this.sent_unreliable_octets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "sent_unreliable_octets"));

      this.sent_receipt_packets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "sent_receipt"));
      this.sent_receipt_octets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "sent_receipt_octets"));

      this.received_reliable_packets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "received_reliable"));
      this.received_reliable_octets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "received_reliable_octets"));

      this.received_unreliable_packets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "received_unreliable"));
      this.received_unreliable_octets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "received_unreliable_octets"));

      this.received_receipt_packets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "received_receipt"));
      this.received_receipt_octets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "received_receipt_octets"));

      this.received_dropped_unreliable_packets =
        metrics.meter(MetricRegistry.name(
          CoClientNetworkHandler.class, "received_dropped_unreliable"));
    }
  }
}
