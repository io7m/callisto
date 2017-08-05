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

package com.io7m.callisto.prototype0.server;

import com.io7m.callisto.prototype0.events.CoEventServiceType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderType;
import com.io7m.callisto.prototype0.process.CoProcessAbstract;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolServiceType;
import com.io7m.callisto.prototype0.ticks.CoTickDivisor;
import com.io7m.jnull.NullCheck;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public final class CoServerNetwork extends CoProcessAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoServerNetwork.class);

  private final CoNetworkProviderType network;
  private final CoTickDivisor tick_divisor;
  private final Disposable sub_tick;
  private final CoStringConstantPoolServiceType strings;
  private CoServerNetworkHandler handler;

  public CoServerNetwork(
    final CoEventServiceType in_events,
    final CoStringConstantPoolServiceType in_strings,
    final CoNetworkProviderType in_network)
  {
    super(
      in_events,
      r -> {
        final Thread th = new Thread(r);
        th.setName("com.io7m.callisto.server.network." + th.getId());
        return th;
      });

    this.network =
      NullCheck.notNull(in_network, "Network");
    this.strings =
      NullCheck.notNull(in_strings, "Strings");
    this.tick_divisor =
      new CoTickDivisor(60.0, 30.0);

    this.sub_tick =
      in_events.events()
        .ofType(CoServerTickEvent.class)
        .observeOn(this.scheduler())
        .subscribe(this::onTickEvent);
  }

  private void onTickEvent(
    final CoServerTickEvent e)
  {
    final CoServerNetworkHandler h = this.handler;
    if (h != null) {
      if (this.tick_divisor.tickNow()) {
        h.tick();
      }
    }
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
    LOG.trace("initialize");

    final Properties props = new Properties();
    props.setProperty("local_address", "::1");
    props.setProperty("local_port", "9999");

    final byte[] password = new byte[0];
    this.handler = new CoServerNetworkHandler(
      this.network,
      this.strings,
      password,
      props);
  }

  @Override
  protected void doStart()
  {
    LOG.trace("start");
  }

  @Override
  protected void doStop()
  {
    LOG.trace("stop");

    final CoServerNetworkHandler h = this.handler;
    if (h != null) {
      try {
        h.close();
      } catch (final Exception e) {
        LOG.error("error shutting down server handler: ", e);
      }
    }

    this.sub_tick.dispose();
  }

  @Override
  protected void doDestroy()
  {
    LOG.trace("destroy");
  }

}
