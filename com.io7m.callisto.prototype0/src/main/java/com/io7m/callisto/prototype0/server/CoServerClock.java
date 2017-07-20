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

import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.process.CoProcessAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class CoServerClock extends CoProcessAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoServerClock.class);

  private static final CoServerTickEvent TICK =
    CoServerTickEvent.of(60);

  private final ScheduledExecutorService sched_exec;

  public CoServerClock(
    final CoEventServiceType in_events)
  {
    super(
      in_events,
      r -> {
        final Thread th = new Thread(r);
        th.setName("com.io7m.callisto.server.clock." + th.getId());
        return th;
      });

    this.sched_exec =
      Executors.newScheduledThreadPool(1, r -> {
        final Thread th = new Thread(r);
        th.setName("com.io7m.callisto.server.clock.act." + th.getId());
        return th;
      });
  }

  @Override
  public String name()
  {
    return "clock";
  }

  private void doTick()
  {
    this.events().post(TICK);
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }

  @Override
  protected void doInitialize()
  {
    LOG.debug("initialize");
  }

  @Override
  protected void doStart()
  {
    LOG.debug("start");
    this.sched_exec.scheduleAtFixedRate(
      this::doTick, 0L, 1000L / 60L, TimeUnit.MILLISECONDS);
  }

  @Override
  protected void doStop()
  {
    LOG.debug("stop");
    this.sched_exec.shutdown();
  }

  @Override
  protected void doDestroy()
  {
    LOG.debug("destroy");
  }
}
