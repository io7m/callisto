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

package com.io7m.callisto.prototype0.process;

import com.io7m.callisto.prototype0.events.CoEventServiceType;
import io.reactivex.disposables.Disposable;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.jcip.annotations.GuardedBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class CoProcessSupervisor extends CoProcessAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoProcessSupervisor.class);

  private static final CoProcessSupervisorEventRequest REQUEST =
    CoProcessSupervisorEventRequest.builder().build();

  private final ReferenceOpenHashSet<CoProcessType> watched;
  private final
  @GuardedBy("watched") ReferenceOpenHashSet<CoProcessType> inactive;
  private final Disposable supervise_sub;
  private final ScheduledExecutorService sched_exec;

  public CoProcessSupervisor(
    final CoEventServiceType in_events,
    final List<CoProcessType> in_processes)
  {
    super(
      in_events,
      r -> {
        final Thread th = new Thread(r);
        th.setName("com.io7m.callisto.supervisor." + th.getId());
        return th;
      });

    this.watched = new ReferenceOpenHashSet<>(in_processes);
    this.inactive = new ReferenceOpenHashSet<>();

    this.sched_exec = Executors.newScheduledThreadPool(1, r -> {
      final Thread th = new Thread(r);
      th.setName("com.io7m.callisto.supervisor.timed." + th.getId());
      th.setPriority(Thread.MIN_PRIORITY);
      return th;
    });

    this.supervise_sub =
      this.events().events()
        .ofType(CoProcessSupervisorEventType.class)
        .subscribeOn(this.scheduler())
        .subscribe(
          this::onSupervisorEvent,
          CoProcessSupervisor::onSupervisorEventError);
  }

  private static void onSupervisorEventError(
    final Throwable ex)
  {
    LOG.error("onSupervisorEventError: ", ex);
  }

  private void onSupervisorEvent(
    final CoProcessSupervisorEventType event)
  {
    event.match(
      this,
      CoProcessSupervisor::onSupervisorEventRequest,
      CoProcessSupervisor::onSupervisorEventResponse,
      CoProcessSupervisor::onSupervisorEventTimedOut);
  }

  private Void onSupervisorEventTimedOut(
    final CoProcessSupervisorEventTimedOutType input)
  {
    LOG.error(
      "supervised process hanging or unresponsive: {}",
      input.subsystem());
    return null;
  }

  private Void onSupervisorEventResponse(
    final CoProcessSupervisorEventResponseType input)
  {
    LOG.trace("process {} responded", input.subsystem());

    synchronized (this.watched) {
      this.inactive.remove(input.subsystem());
    }
    return null;
  }

  private Void onSupervisorEventRequest(
    final CoProcessSupervisorEventRequestType input)
  {
    return null;
  }

  @Override
  public String name()
  {
    return "supervisor";
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

    synchronized (this.watched) {
      this.inactive.clear();
    }

    this.sched_exec.scheduleAtFixedRate(
      this::doTick, 0L, 5L, TimeUnit.SECONDS);
  }

  @Override
  protected void doDestroy()
  {
    LOG.debug("destroy");
  }

  @Override
  protected void doStop()
  {
    LOG.debug("stop");
    this.supervise_sub.dispose();
    this.sched_exec.shutdown();
  }

  private void doTick()
  {
    /*
     * Check if all processes have removed themselves from the inactive set.
     */

    LOG.trace("checking processes");

    final ReferenceOpenHashSet<CoProcessType> current_inactive;
    synchronized (this.watched) {
      current_inactive = new ReferenceOpenHashSet<>(this.inactive);
    }

    LOG.trace("inactive: {}", current_inactive);

    if (!current_inactive.isEmpty()) {
      current_inactive.forEach(
        c -> this.events().post(CoProcessSupervisorEventTimedOut.of(c)));
    }

    /*
     * Mark all processes as inactive and then allow them to mark themselves
     * as active when requested.
     */

    synchronized (this.watched) {
      this.inactive.addAll(this.watched);
    }

    this.events().post(REQUEST);
  }
}
