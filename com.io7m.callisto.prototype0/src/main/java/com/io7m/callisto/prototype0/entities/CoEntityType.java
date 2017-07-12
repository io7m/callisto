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

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The type of entities.
 */

public interface CoEntityType
{
  /**
   * @return The unique ID of the entity
   */

  CoEntityID id();

  /**
   * Retrieve the trait registered under the given class.
   *
   * @param c   The class
   * @param <T> The precise type of trait
   *
   * @return The trait, or nothing if no trait is registered under the given
   * class
   *
   * @throws CoEntityDestroyedException If the entity has already been
   *                                    destroyed
   */

  <T extends CoEntityTraitType> Optional<T> trait(
    Class<T> c)
    throws CoEntityDestroyedException;

  /**
   * Retrieve the trait registered under the given class.
   *
   * @param c   The class
   * @param <T> The precise type of trait
   *
   * @return The trait
   *
   * @throws CoEntityTraitNonexistentException If no trait is registered under
   *                                           the given class
   * @throws CoEntityDestroyedException        If the entity has already been
   *                                           destroyed
   */

  <T extends CoEntityTraitType> T traitGet(
    Class<T> c)
    throws CoEntityDestroyedException, CoEntityTraitNonexistentException;

  /**
   * Register the trait under the given class.
   *
   * @param c     The class
   * @param <T>   The precise type of trait
   * @param value The trait
   *
   * @throws CoEntityTraitDuplicateException If a trait is already registered
   *                                         under the given class
   * @throws CoEntityDestroyedException      If the entity has already been
   *                                         destroyed
   */

  <T extends CoEntityTraitType> void traitSet(
    Class<T> c,
    T value)
    throws CoEntityDestroyedException, CoEntityTraitDuplicateException;

  /**
   * Destroy any trait registered under the given class. If no trait is
   * registered, the call is ignored.
   *
   * @param c   The class
   * @param <T> The precise type of trait
   *
   * @throws CoEntityDestroyedException If the entity has already been
   *                                    destroyed
   */

  <T extends CoEntityTraitType> void traitDestroy(
    Class<T> c)
    throws CoEntityDestroyedException;

  /**
   * Provide read-only access to the entity messages. Any message
   * sent to the entity with {@link #send(CoEntityMessageType)} will be sent
   * to any subscribers.
   *
   * @return The observable message queue
   */

  Observable<CoEntityMessageType> messages();

  /**
   * Subscribe to messages sent to this entity.
   *
   * @param type The type of messages that should be received
   * @param f    The function that will receive messages
   * @param <M>  The precise type of messages
   *
   * @return A subscription to the messages
   */

  default <M extends CoEntityMessageType> Disposable subscribe(
    final Class<M> type,
    final Consumer<M> f)
  {
    return this.messages().ofType(type).subscribe(f::accept);
  }

  /**
   * Send a message to this entity.
   *
   * @param m The message
   *
   * @throws CoEntityDestroyedException If the entity has already been
   *                                    destroyed
   */

  void send(
    final CoEntityMessageType m)
    throws CoEntityDestroyedException;

  /**
   * @return {@code true} if the entity has been destroyed
   */

  boolean isDestroyed();

  /**
   * Destroy the entity. If the entity has already been destroyed, the call
   * is simply ignored.
   */

  void destroy();
}
