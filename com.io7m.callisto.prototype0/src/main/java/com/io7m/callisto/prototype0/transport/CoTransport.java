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

package com.io7m.callisto.prototype0.transport;

import com.io7m.callisto.prototype0.events.CoEventType;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnimplementedCodeException;

public final class CoTransport implements CoTransportType
{
  private final int id;

  private CoTransport(
    final int in_id)
  {
    this.id = in_id;
  }

  @Override
  public void enqueue(
    final CoEventType e)
  {
    NullCheck.notNull(e, "Event");

    // TODO: Generated method stub
    throw new UnimplementedCodeException();
  }

  @Override
  public void poll()
  {
    // TODO: Generated method stub
    throw new UnimplementedCodeException();
  }
}
