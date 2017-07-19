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

package com.io7m.callisto.prototype0.client;

import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.process.CoProcessAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoClientNetwork extends CoProcessAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoClientNetwork.class);

  public CoClientNetwork(
    final CoEventServiceType in_events)
  {
    super(
      in_events,
      r -> {
        final Thread th = new Thread(r);
        th.setName("com.io7m.callisto.client.network." + th.getId());
        return th;
      });
  }

  @Override
  public String name()
  {
    return "network";
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }

  @Override
  protected void doInitialize()
  {
    LOG.debug("initialize");
  }

  @Override
  protected void doStart()
  {
    LOG.debug("start");
  }

  @Override
  protected void doStop()
  {
    LOG.debug("stop");
  }

  @Override
  protected void doDestroy()
  {
    LOG.debug("destroy");
  }
}
