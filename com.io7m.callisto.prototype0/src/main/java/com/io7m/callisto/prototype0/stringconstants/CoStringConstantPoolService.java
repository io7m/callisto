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

public final class CoStringConstantPoolService
  implements CoStringConstantPoolServiceType
{
  private static final CoStringConstantPoolEventUpdated UPDATED =
    CoStringConstantPoolEventUpdated.builder().build();
  private final CoEventServiceType events;
  private final CoStringConstantPool pool;

  public CoStringConstantPoolService(
    final CoEventServiceType in_events)
  {
    this.events = NullCheck.notNull(in_events, "Events");
    this.pool = new CoStringConstantPool(() -> this.events.post(UPDATED));
    this.pool.newUpdate()
      .set(0, CoStringConstantPoolMessages.eventCompressedUpdateTypeName())
      .execute();
  }

  @Override
  public String lookupString(
    final CoStringConstantReference r)
  {
    return this.pool.lookupString(r);
  }

  @Override
  public CoStringConstantReference lookupReference(
    final String text)
  {
    return this.pool.lookupReference(text);
  }

  @Override
  public Int2ReferenceMap<String> view()
  {
    return this.pool.view();
  }

  @Override
  public CoStringConstantPoolUpdateType newUpdate()
  {
    return this.pool.newUpdate();
  }

  @Override
  public CoStringConstantReference add(
    final String text)
  {
    return this.pool.add(text);
  }

  @Override
  public void shutDown()
  {

  }
}
