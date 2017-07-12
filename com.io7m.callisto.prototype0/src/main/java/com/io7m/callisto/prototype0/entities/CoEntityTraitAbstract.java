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
import com.io7m.jnull.Nullable;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstract implementation of the {@link CoEntityTraitType} interface.
 */

public abstract class CoEntityTraitAbstract implements CoEntityTraitType
{
  private final AtomicReference<CoEntityType> owner;

  protected CoEntityTraitAbstract()
  {
    this.owner = new AtomicReference<>();
  }

  @Override
  public final CoEntityType owner()
  {
    return this.owner.get();
  }

  /**
   * Called when the owner of this trait has changed.
   *
   * @param previous The previous owner
   */

  protected void onOwnerChanged(
    final @Nullable CoEntityType previous)
  {

  }

  @Override
  public final void onAttach(
    final CoEntityType in_owner)
    throws CoException
  {
    final CoEntityType previous =
      this.owner.getAndSet(NullCheck.notNull(in_owner, "Owner"));
    this.onOwnerChanged(previous);
  }
}
