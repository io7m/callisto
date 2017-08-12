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

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.security.SecureRandom;

public final class CoIDPoolUnpredictable implements CoIDPoolType
{
  private final IntOpenHashSet entities_ids;
  private final SecureRandom random;

  public CoIDPoolUnpredictable()
  {
    this.entities_ids = new IntOpenHashSet();
    this.random = new SecureRandom();
  }

  @Override
  public int fresh()
  {
    synchronized (this.entities_ids) {
      for (int index = 0; index < 100; ++index) {
        final int id = this.random.nextInt();
        if (this.entities_ids.contains(id)) {
          continue;
        }
        this.entities_ids.add(id);
        return id;
      }

      throw new IllegalStateException(
        "Could not generate fresh ID after 100 attempts");
    }
  }

  @Override
  public void release(
    final int x)
  {
    synchronized (this.entities_ids) {
      this.entities_ids.remove(x);
    }
  }
}
