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
import com.io7m.callisto.prototype0.network.CoNetworkPacketPeerType;
import com.io7m.callisto.prototype0.network.CoNetworkProviderType;
import com.io7m.callisto.prototype0.process.CoProcessAbstract;
import com.io7m.callisto.prototype0.ticks.CoTickDivisor;
import com.io7m.jnull.NullCheck;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyNonexistent;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Future;

public final class CoClientNetwork extends CoProcessAbstract
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoClientNetwork.class);

  private final CoNetworkProviderType network;
  private final Disposable sub_tick;
  private final CoTickDivisor tick_divisor;
  private CoClientServerHandler handler;

  public CoClientNetwork(
    final CoEventServiceType in_events,
    final CoNetworkProviderType in_network)
  {
    super(
      in_events,
      r -> {
        final Thread th = new Thread(r);
        th.setName("com.io7m.callisto.client.network." + th.getId());
        return th;
      });

    this.network =
      NullCheck.notNull(in_network, "Network");

    this.tick_divisor =
      new CoTickDivisor(60.0, 30.0);

    this.sub_tick =
      in_events.events()
        .ofType(CoClientTickEvent.class)
        .observeOn(this.scheduler())
        .subscribe(this::onTickEvent);
  }

  private void onTickEvent(
    final CoClientTickEvent e)
  {
    final CoClientServerHandler h = this.handler;
    if (h != null) {
      if (this.tick_divisor.tickNow()) {
        try {
          h.onTick();
        } catch (final IOException ex) {
          LOG.error("i/o error: ", ex);
        }
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
    this.sub_tick.dispose();

    final CoClientServerHandler h = this.handler;
    if (h != null) {
      try {
        h.close();
      } catch (final IOException e) {
        LOG.error("i/o error: ", e);
      }
    }
  }

  @Override
  protected void doDestroy()
  {
    LOG.debug("destroy");
  }

  public Future<Void> connect(
    final Properties props)
  {
    return this.executor().submit(() -> this.doConnect(props));
  }

  private Void doConnect(
    final Properties props)
    throws IOException, JPropertyNonexistent
  {
    LOG.debug("trying to connect to server");

    final String user =
      JProperties.getString(props, "user");
    final String pass =
      JProperties.getStringOptional(props, "password", "");

    final CoNetworkPacketPeerType peer =
      this.network.createPeer(props);

    this.handler =
      new CoClientServerHandler(
        this.events(),
        peer,
        peer.remote().get());

    this.handler.onStart(user, pass);
    return null;
  }
}
