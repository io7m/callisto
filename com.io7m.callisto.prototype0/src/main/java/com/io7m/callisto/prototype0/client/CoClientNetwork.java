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

import com.codahale.metrics.MetricRegistry;
import com.io7m.callisto.prototype0.events.CoEventNetworkSerializerRegistryType;
import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderType;
import com.io7m.callisto.prototype0.process.CoProcessAbstract;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolReadableType;
import com.io7m.callisto.prototype0.ticks.CoTickDivisor;
import com.io7m.callisto.prototype0.transport.CoTransportClientConfiguration;
import com.io7m.jnull.NullCheck;
import com.io7m.jproperties.JPropertyNonexistent;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.util.Properties;
import java.util.concurrent.Future;

public final class CoClientNetwork extends CoProcessAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoClientNetwork.class);

  private final CoNetworkProviderType network;
  private final Disposable sub_tick;
  private final CoTickDivisor tick_divisor;
  private final CoStringConstantPoolReadableType strings;
  private final Disposable sub_net_events;
  private final CoEventNetworkSerializerRegistryType event_serializers;
  private final MetricRegistry metrics;
  private final Clock clock;
  private CoClientNetworkHandler handler;

  public CoClientNetwork(
    final Clock in_clock,
    final MetricRegistry in_metrics,
    final CoEventServiceType in_events,
    final CoEventNetworkSerializerRegistryType in_event_serializers,
    final CoStringConstantPoolReadableType in_strings,
    final CoNetworkProviderType in_network)
  {
    super(
      in_events,
      r -> {
        final Thread th = new Thread(r);
        th.setName("com.io7m.callisto.client.network." + th.getId());
        return th;
      });

    this.clock =
      NullCheck.notNull(in_clock, "Clock");
    this.metrics =
      NullCheck.notNull(in_metrics, "Metrics");
    this.event_serializers =
      NullCheck.notNull(in_event_serializers, "Event serializers");
    this.strings =
      NullCheck.notNull(in_strings, "Strings");
    this.network =
      NullCheck.notNull(in_network, "Network");

    this.tick_divisor =
      new CoTickDivisor(60.0, 30.0);

    this.sub_tick =
      in_events.events()
        .ofType(CoClientTickEvent.class)
        .observeOn(this.scheduler())
        .subscribe(this::onTickEvent);

    this.sub_net_events =
      in_events.events()
        .ofType(CoClientNetworkEventType.class)
        .observeOn(this.scheduler())
        .subscribe(this::onNetworkEvent);
  }

  private void onNetworkEvent(
    final CoClientNetworkEventType e)
  {
    switch (e.type()) {
      case CLIENT_CONNECTED: {
        break;
      }

      case CLIENT_CONNECTION_REFUSED:
      case CLIENT_CONNECTION_TIMED_OUT:
      case CLIENT_DISCONNECTED: {
        try {
          LOG.debug("closing handler");
          this.handler.close();
          this.handler = null;
        } catch (final IOException ex) {
          LOG.error("i/o error: ", ex);
        }
        break;
      }
    }
  }

  private void onTickEvent(
    final CoClientTickEvent e)
  {
    if (this.tick_divisor.tickNow()) {
      final CoClientNetworkHandler h = this.handler;
      if (h != null) {
        h.tick();
      }
    }
  }

  @Override
  public String name()
  {
    return "network";
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }

  @Override
  protected void doInitialize()
  {
    LOG.trace("initialize");
  }

  @Override
  protected void doStart()
  {
    LOG.trace("start");
  }

  @Override
  protected void doStop()
  {
    LOG.trace("stop");
    this.sub_tick.dispose();
    this.sub_net_events.dispose();

    final CoClientNetworkHandler h = this.handler;
    if (h != null) {
      try {
        h.close();
      } catch (final IOException e) {
        LOG.error("i/o error: ", e);
      }
    }
  }

  @Override
  protected void doDestroy()
  {
    LOG.trace("destroy");
  }

  public Future<Void> connect(
    final Properties props)
  {
    return this.executor().submit(() -> this.doConnect(props));
  }

  private Void doConnect(
    final Properties props)
    throws IOException, JPropertyNonexistent
  {
    LOG.debug("opening connection to server");

    final CoTransportClientConfiguration config =
      CoTransportClientConfiguration.builder()
        .setHelloRetryCount(10)
        .setHelloRetryDelayInTicks(60)
        .setTicksPerSecond(30)
        .build();

    this.handler =
      new CoClientNetworkHandler(
        this.metrics,
        this.clock,
        this.events(),
        this.event_serializers,
        this.network,
        this.strings,
        props, config);

    this.handler.start();
    return null;
  }
}
