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

import com.io7m.callisto.prototype0.entities.CoEntityType;
import com.io7m.callisto.prototype0.services.CoAbstractService;
import com.io7m.jnull.NullCheck;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class CoRoomService
  extends CoAbstractService implements CoRoomServiceType
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoRoomService.class);

  public CoRoomService()
  {

  }

  @Activate
  public void onActivate()
  {
    this.onActivateActual();
  }

  @Override
  public void shutDown()
  {

  }

  @Override
  protected Logger log()
  {
    return LOG;
  }

  private static final class Room implements CoRoomType
  {
    private final CoRoomID id;
    private final Int2ReferenceOpenHashMap<CoEntityType> entities;

    private Room(
      final CoRoomID in_id)
    {
      this.id = NullCheck.notNull(in_id, "ID");
      this.entities = new Int2ReferenceOpenHashMap<>();
    }

    @Override
    public CoRoomID id()
    {
      return this.id;
    }
  }
}
