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

import com.io7m.callisto.prototype0.client.CoClientAudio;
import com.io7m.callisto.prototype0.client.CoClientRendering;
import com.io7m.callisto.prototype0.client.CoClientType;
import com.io7m.callisto.prototype0.events.CoEventService;
import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderType;
import com.io7m.callisto.prototype0.process.CoProcessSupervisor;
import com.io7m.callisto.prototype0.process.CoProcessType;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolServiceType;
import com.io7m.jnull.NullCheck;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public final class CoServer implements CoServerType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoServer.class);

  private final CoEventServiceType events;
  private final ReferenceArrayList<CoProcessType> processes;
  private final CoNetworkProviderType network;

  public CoServer(
    final CoNetworkProviderType in_network,
    final CoStringConstantPoolServiceType in_strings,
    final CoEventServiceType in_events)
  {
    this.network =
      NullCheck.notNull(in_network, "Network");
    this.events =
      NullCheck.notNull(in_events, "Events");

    this.processes = new ReferenceArrayList<>();
    this.processes.add(new CoServerClock(this.events));
    this.processes.add(new CoServerLogic(this.events));
    this.processes.add(new CoServerNetwork(this.events, in_strings, in_network));
    this.processes.add(new CoProcessSupervisor(this.events, this.processes));
  }

  private static void waitForFutures(
    final Iterable<Future<Void>> futures,
    final long timeout,
    final TimeUnit unit)
    throws ExecutionException, TimeoutException
  {
    final long end = System.nanoTime() + unit.toNanos(timeout);
    for (final Future<Void> f : futures) {
      try {
        f.get(end - System.nanoTime(), TimeUnit.NANOSECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void startSynchronously(
    final long time,
    final TimeUnit unit)
    throws TimeoutException, ExecutionException
  {
    LOG.debug("initializing processes");

    final List<Future<Void>> futures0 =
      this.processes.stream()
        .map(CoProcessType::initialize)
        .collect(Collectors.toList());

    LOG.debug("waiting for processes to initialize");
    waitForFutures(futures0, time, unit);

    LOG.debug("starting processes");

    final List<Future<Void>> futures1 =
      this.processes.stream()
        .map(CoProcessType::start)
        .collect(Collectors.toList());

    LOG.debug("waiting for processes to start");
    waitForFutures(futures1, time, unit);
  }

  public void shutDownSynchronously(
    final long time,
    final TimeUnit unit)
    throws TimeoutException, ExecutionException
  {
    LOG.debug("stopping processes");

    final List<Future<Void>> futures0 =
      this.processes.stream()
        .map(CoProcessType::stop)
        .collect(Collectors.toList());

    LOG.debug("waiting for processes to be stopped");
    waitForFutures(futures0, time, unit);

    LOG.debug("destroying processes");

    final List<Future<Void>> futures1 =
      this.processes.stream()
        .map(CoProcessType::destroy)
        .collect(Collectors.toList());

    LOG.debug("waiting for processes to be destroyed");
    waitForFutures(futures1, time, unit);
  }
}
