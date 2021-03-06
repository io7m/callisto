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

package com.io7m.callisto.prototype0.idpool;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;

public final class CoIDPool implements CoIDPoolType
{
  private final IntRBTreeSet entities_ids_free;
  private final Object lock;
  private int entities_id_next;

  public CoIDPool()
  {
    this.entities_id_next = 0;
    this.entities_ids_free = new IntRBTreeSet();
    this.lock = new Object();
  }

  @Override
  public int fresh()
  {
    synchronized (this.lock) {
      if (!this.entities_ids_free.isEmpty()) {
        final int id = this.entities_ids_free.firstInt();
        this.entities_ids_free.remove(id);
        return id;
      }

      final int id = this.entities_id_next;
      if (Integer.compareUnsigned(id + 1, id) < 0) {
        throw new IllegalStateException("Identifier pool overflow.");
      }

      this.entities_id_next = this.entities_id_next + 1;
      return id;
    }
  }

  @Override
  public void release(
    final int x)
  {
    synchronized (this.lock) {
      this.entities_ids_free.add(x);
    }
  }
}
