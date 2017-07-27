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

package com.io7m.callisto.prototype0.stringconstants;

import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.jnull.NullCheck;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;

import java.util.NoSuchElementException;

public final class CoStringConstantPoolService
  implements CoStringConstantPoolServiceType
{
  private final Int2ReferenceOpenHashMap<String> constants;
  private final CoEventServiceType events;

  private static final CoStringConstantPoolEventUpdated UPDATED =
    CoStringConstantPoolEventUpdated.builder().build();

  public CoStringConstantPoolService(
    final CoEventServiceType in_events)
  {
    this.events = NullCheck.notNull(in_events, "Events");
    this.constants = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  public String lookup(
    final CoStringConstantReference r)
  {
    NullCheck.notNull(r, "Reference");

    synchronized (this.constants) {
      final int k = r.value();
      if (this.constants.containsKey(k)) {
        return this.constants.get(k);
      }

      throw new NoSuchElementException(
        "No such string constant: " + Integer.toUnsignedString(k));
    }
  }

  @Override
  public Int2ReferenceMap<String> view()
  {
    synchronized (this.constants) {
      return new Int2ReferenceOpenHashMap<>(this.constants);
    }
  }

  @Override
  public CoStringConstantPoolUpdateType update()
  {
    return new Update();
  }

  @Override
  public void shutDown()
  {

  }

  private final class Update implements CoStringConstantPoolUpdateType
  {
    private final Int2ReferenceOpenHashMap<String> update =
      new Int2ReferenceOpenHashMap<>();

    Update()
    {

    }

    @Override
    public void set(
      final int index,
      final String value)
    {
      this.update.put(index, NullCheck.notNull(value, "Value"));
    }

    @Override
    public void update()
    {
      synchronized (CoStringConstantPoolService.this.constants) {
        CoStringConstantPoolService.this.constants.putAll(this.update);
        this.update.clear();
      }

      CoStringConstantPoolService.this.events.post(UPDATED);
    }
  }
}
