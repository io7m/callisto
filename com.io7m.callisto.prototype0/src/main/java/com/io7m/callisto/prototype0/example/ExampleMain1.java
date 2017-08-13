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

package com.io7m.callisto.prototype0.example;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.io7m.callisto.prototype0.client.CoClient;
import com.io7m.callisto.prototype0.client.CoClientTickEvent;
import com.io7m.callisto.prototype0.events.CoEventNetworkSerializerRegistryServiceLoader;
import com.io7m.callisto.prototype0.events.CoEventService;
import com.io7m.callisto.prototype0.network.CoNetworkProviderLocal;
import com.io7m.callisto.prototype0.network.CoNetworkProviderUDP;
import com.io7m.callisto.prototype0.server.CoServer;
import com.io7m.callisto.prototype0.server.CoServerTickEvent;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolService;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolServiceType;
import com.io7m.timehack6435126.TimeHack6435126;
import io.reactivex.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ExampleMain1
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ExampleMain1.class);

  private ExampleMain1()
  {

  }

  public static void main(
    final String[] args)
    throws TimeoutException, ExecutionException, InterruptedException
  {
    TimeHack6435126.enableHighResolutionTimer();

    final MetricRegistry metrics =
      new MetricRegistry();
    final Meter client_event_meter =
      metrics.meter("com.io7m.callisto.client.events");
    final Meter server_event_meter =
      metrics.meter("com.io7m.callisto.server.events");

    final CoEventService client_events = new CoEventService();
    client_events.onActivate();
    client_events.events()
      .observeOn(Schedulers.single())
      .map(e -> {
        client_event_meter.mark();
        return e;
      })
      .filter(e -> !(e instanceof CoClientTickEvent))
      .subscribe(e -> LOG.trace("client event: {}", e));

    final CoEventService server_events = new CoEventService();
    server_events.onActivate();
    server_events.events()
      .observeOn(Schedulers.single())
      .map(e -> {
        server_event_meter.mark();
        return e;
      })
      .filter(e -> !(e instanceof CoServerTickEvent))
      .subscribe(e -> LOG.trace("server event: {}", e));

    final Slf4jReporter reporter =
      Slf4jReporter.forRegistry(metrics)
        .outputTo(LOG)
        .convertRatesTo(TimeUnit.SECONDS)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .build();
    reporter.start(5L, TimeUnit.SECONDS);

    final JmxReporter jmx_reporter = JmxReporter.forRegistry(metrics).build();
    jmx_reporter.start();

    final Clock clock = Clock.systemUTC();

    final CoNetworkProviderUDP network = new CoNetworkProviderUDP();

    final CoEventNetworkSerializerRegistryServiceLoader client_serializers =
      new CoEventNetworkSerializerRegistryServiceLoader(client_events);
    client_serializers.onActivate();

    final CoStringConstantPoolServiceType client_strings =
      new CoStringConstantPoolService(client_events);

    final CoClient client =
      new CoClient(
        clock,
        metrics,
        network,
        client_strings,
        client_events,
        client_serializers);
    client.startSynchronously(3L, TimeUnit.SECONDS);

    final CoEventNetworkSerializerRegistryServiceLoader server_serializers =
      new CoEventNetworkSerializerRegistryServiceLoader(server_events);
    server_serializers.onActivate();

    final CoStringConstantPoolServiceType server_strings =
      new CoStringConstantPoolService(server_events);

    final CoServer server =
      new CoServer(
        clock,
        metrics,
        network,
        server_strings,
        server_events,
        server_serializers);
    server.startSynchronously(3L, TimeUnit.SECONDS);

    final Properties props = new Properties();
    props.setProperty("remote_address", "::1");
    props.setProperty("remote_port", "9999");
    client.connect(props).get(10L, TimeUnit.SECONDS);

    //    Thread.sleep(TimeUnit.MILLISECONDS.convert(3L, TimeUnit.SECONDS));
    //    LOG.debug("enabling full packet loss");
    //    network.setPacketLoss(1.0);

    //    LOG.debug("waiting 5S");
    //    Thread.sleep(TimeUnit.MILLISECONDS.convert(5L, TimeUnit.SECONDS));
    //    LOG.debug("waited");

    final Thread th = new Thread(() -> {
      try {
        System.in.read();
        client.shutDownSynchronously(3L, TimeUnit.SECONDS);
        server.shutDownSynchronously(3L, TimeUnit.SECONDS);
      } catch (final Exception e) {
        LOG.error("failed: ", e);
      }
    });
    th.setName("com.io7m.callisto.example.wait_for_key");
    th.start();
    th.join();
  }
}
