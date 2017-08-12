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

package com.io7m.callisto.core;

import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;

import java.util.Optional;

/**
 * The type of exceptions raised by the engine.
 */

public abstract class CoException extends RuntimeException
{
  private @Nullable CoException next;

  /**
   * @return The next exception, if any
   */

  public final Optional<CoException> next()
  {
    return Optional.ofNullable(this.next);
  }

  /**
   * Set the next exception.
   *
   * @param new_next The next exception, or {@code null} if there isn't one
   *
   * @return {@code new_next}
   */

  public final CoException setNext(
    final CoException new_next)
  {
    this.next = new_next;
    return this.next;
  }

  /**
   * Start or continue an exception chain. If {@code ex == null}, then the
   * exception {@code next} will be the start of the chain.
   * Otherwise, the exception {@code next} will be added to the end
   * of the chain.
   *
   * @param ex   The current exception
   * @param next The next exception
   *
   * @return The exception chain
   */

  public static CoException chain(
    final @Nullable CoException ex,
    final CoException next)
  {
    NullCheck.notNull(next, "next");

    if (ex == null) {
      return next;
    }

    CoException e_last = ex;
    while (e_last.next != null) {
      e_last = e_last.next;
    }

    NullCheck.notNull(e_last, "Last");
    e_last.setNext(next);
    return ex;
  }

  /**
   * Construct an exception.
   *
   * @param message The message
   */

  public CoException(
    final String message)
  {
    super(message);
  }

  /**
   * Construct an exception.
   *
   * @param message The message
   * @param cause   The cause
   */

  public CoException(
    final String message,
    final Throwable cause)
  {
    super(message, cause);
  }

  /**
   * Construct an exception.
   *
   * @param cause The cause
   */

  public CoException(
    final Throwable cause)
  {
    super(cause);
  }
}
