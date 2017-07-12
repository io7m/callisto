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

package com.io7m.callisto.prototype0.entities;

import com.io7m.callisto.core.CoException;
import com.io7m.jnull.NullCheck;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import net.jcip.annotations.ThreadSafe;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The default implementation of the {@link CoEntityType} interface.
 */

@ThreadSafe
public final class CoEntity implements CoEntityType
{
  private final PublishSubject<CoEntityMessageType> messages;
  private final CoEntityID id;
  private final CoEntityListenerType listener;
  private final AtomicBoolean destroyed;
  private volatile Map<Class<? extends CoEntityTraitType>, CoEntityTraitType> traits;

  private CoEntity(
    final CoEntityListenerType in_listener,
    final PublishSubject<CoEntityMessageType> in_messages,
    final Map<Class<? extends CoEntityTraitType>, CoEntityTraitType> in_traits,
    final CoEntityID in_id)
  {
    this.listener =
      NullCheck.notNull(in_listener, "Listener");
    this.id =
      NullCheck.notNull(in_id, "ID");
    this.messages =
      NullCheck.notNull(in_messages, "Messages");
    this.traits =
      NullCheck.notNull(in_traits, "Traits");

    this.destroyed = new AtomicBoolean(false);
  }

  /**
   * Create an entity.
   *
   * @param in_listener A listener that will receive events
   * @param id          The entity ID
   *
   * @return A new entity
   */

  public static CoEntity create(
    final CoEntityListenerType in_listener,
    final CoEntityID id)
  {
    NullCheck.notNull(id, "ID");
    return new CoEntity(
      in_listener, PublishSubject.create(), HashMap.empty(), id);
  }

  @Override
  public CoEntityID id()
  {
    return this.id;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends CoEntityTraitType> Optional<T> trait(
    final Class<T> c)
    throws CoEntityDestroyedException, CoEntityTraitNonexistentException
  {
    NullCheck.notNull(c, "Class");
    this.checkNotDestroyed();
    return this.traits.get(c).map(x -> (T) x).toJavaOptional();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends CoEntityTraitType> T traitGet(
    final Class<T> c)
    throws CoEntityDestroyedException, CoEntityTraitNonexistentException
  {
    NullCheck.notNull(c, "Class");
    this.checkNotDestroyed();

    final Map<Class<? extends CoEntityTraitType>, CoEntityTraitType> t = this.traits;
    return (T) t.getOrElseThrow(() -> new CoEntityTraitNonexistentException("x"))._2;
  }

  private void checkNotDestroyed()
  {
    if (this.isDestroyed()) {
      throw new CoEntityDestroyedException("x");
    }
  }

  @Override
  public <T extends CoEntityTraitType> void traitSet(
    final Class<T> c,
    final T value)
  {
    NullCheck.notNull(c, "Class");
    NullCheck.notNull(value, "Value");
    this.checkNotDestroyed();

    Map<Class<? extends CoEntityTraitType>, CoEntityTraitType> t = this.traits;

    if (t.containsKey(c)) {
      final CoEntityTraitDuplicateException x =
        new CoEntityTraitDuplicateException("x");
      this.listener.onEntityTraitAddError(this, value, x);
      throw x;
    }

    t = t.put(c, value);

    CoException ex = null;
    try {
      value.onAttach(this);
    } catch (final Exception ea) {
      ex = CoException.chain(ex, new CoEntityOnAttachException(ea));
      try {
        value.onDestroy();
      } catch (final Exception ed) {
        ex = CoException.chain(ex, new CoEntityOnDestroyException(ed));
      }
      this.listener.onEntityTraitAddError(this, value, ex);
      throw ex;
    }

    this.traits = t;
    this.listener.onEntityTraitAdded(this, value);
  }

  @Override
  public <T extends CoEntityTraitType> void traitDestroy(
    final Class<T> c)
    throws CoEntityDestroyedException
  {
    NullCheck.notNull(c, "Class");
    this.checkNotDestroyed();

    final Map<Class<? extends CoEntityTraitType>, CoEntityTraitType> t = this.traits;
    final Option<CoEntityTraitType> tr_opt = t.get(c);
    this.traits = t.remove(c);

    if (tr_opt.isDefined()) {
      final CoEntityTraitType tr = tr_opt.get();
      try {
        tr.onDestroy();
        this.listener.onEntityTraitDestroyed(this, tr);
      } catch (final Exception e) {
        final CoEntityTraitOnDestroyException ex =
          new CoEntityTraitOnDestroyException(e, e.getMessage());
        this.listener.onEntityTraitDestroyError(this, tr, ex);
      }
    }
  }

  @Override
  public boolean equals(
    final Object o)
  {
    if (this == o) {
      return true;
    }
    if (o == null || this.getClass() != o.getClass()) {
      return false;
    }

    final CoEntity coEntity = (CoEntity) o;
    return Objects.equals(this.id, coEntity.id);
  }

  @Override
  public int hashCode()
  {
    return this.id.hashCode();
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder(32);
    sb.append("[CoEntity ");
    sb.append(Long.toUnsignedString(this.id.value()));
    sb.append("]");
    return sb.toString();
  }

  @Override
  public Observable<CoEntityMessageType> messages()
  {
    return this.messages;
  }

  @Override
  public void send(
    final CoEntityMessageType m)
    throws CoException
  {
    NullCheck.notNull(m, "Message");
    this.checkNotDestroyed();
    this.messages.onNext(m);
  }

  @Override
  public boolean isDestroyed()
  {
    return this.destroyed.get();
  }

  @Override
  public void destroy()
  {
    if (this.destroyed.compareAndSet(false, true)) {
      try {
        this.messages.onComplete();

        final Map<Class<? extends CoEntityTraitType>, CoEntityTraitType> t = this.traits;
        CoException e = null;
        for (final CoEntityTraitType trait : t.values()) {
          try {
            trait.onDestroy();
          } catch (final Exception ex) {
            e = CoException.chain(e, new CoEntityOnDestroyException(ex));
          }
        }

        if (e != null) {
          this.listener.onEntityDestroyError(e);
        }
      } finally {
        this.listener.onEntityDestroyed(this);
      }
    }
  }
}
