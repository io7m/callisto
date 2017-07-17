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

package com.io7m.callisto.prototype0.events;

import com.io7m.callisto.prototype0.services.CoAbstractService;
import com.io7m.callisto.prototype0.entities.CoEntityLifecycleEvent;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class CoEventService
  extends CoAbstractService implements CoEventServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoEventService.class);

  private final PublishSubject<CoEventType> events;
  private final Disposable entity_events;
  private final Int2ReferenceOpenHashMap<PublishSubject<CoEventType>> entity_subjects;

  public CoEventService()
  {
    this.events = PublishSubject.create();

    this.entity_events =
      this.events.ofType(CoEntityLifecycleEvent.class)
        .subscribe(this::onEntityLifecycleEvent);

    this.entity_subjects =
      new Int2ReferenceOpenHashMap<>();
  }

  private void onEntityLifecycleEvent(
    final CoEntityLifecycleEvent e)
  {
    switch (e.lifecycle()) {

      case ENTITY_CREATED: {
        synchronized (this.entity_subjects) {
          this.entity_subjects.put(
            e.entity().id().value(), PublishSubject.create());
        }
        return;
      }

      case ENTITY_TRAITS_CHANGED: {
        return;
      }

      case ENTITY_DESTROYED: {
        synchronized (this.entity_subjects) {
          this.entity_subjects.remove(
            e.entity().id().value());
        }
        return;
      }
    }

    throw new UnreachableCodeException();
  }

  @Activate
  public void onActivate()
  {
    this.onActivateActual();
  }

  @Override
  @Deactivate
  public void shutDown()
  {
    this.checkActivated();

    this.events.onComplete();

    synchronized (this.entity_subjects) {
      for (final PublishSubject<CoEventType> s : this.entity_subjects.values()) {
        s.onComplete();
      }
    }

    this.entity_events.dispose();
  }

  @Override
  public void post(
    final CoEventType e)
  {
    NullCheck.notNull(e, "Event");
    this.checkActivated();
    this.events.onNext(e);
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }
}
