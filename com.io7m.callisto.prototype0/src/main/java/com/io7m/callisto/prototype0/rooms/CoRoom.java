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

package com.io7m.callisto.prototype0.rooms;

import com.io7m.callisto.prototype0.entities.CoEntityID;
import com.io7m.callisto.prototype0.entities.CoEntityType;
import com.io7m.jnull.NullCheck;
import io.vavr.collection.HashMap;

public final class CoRoom implements CoRoomType
{
  private final CoRoomID id;
  private final HashMap<CoEntityID, CoEntityType> entities;

  private CoRoom(
    final CoRoomID in_id)
  {
    this.id = NullCheck.notNull(in_id, "ID");
    this.entities = HashMap.empty();
  }

  @Override
  public CoRoomID id()
  {
    return this.id;
  }
}
