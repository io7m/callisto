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

package com.io7m.callisto.prototype0.example;

import com.io7m.callisto.prototype0.client.CoClient;
import com.io7m.callisto.prototype0.events.CoEventService;
import com.io7m.callisto.prototype0.server.CoServer;
import com.io7m.timehack6435126.TimeHack6435126;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class ExampleMain1
{
  private static final Logger LOG =
    LoggerFactory.getLogger(ExampleMain1.class);

  private ExampleMain1()
  {

  }

  public static void main(
    final String[] args)
    throws TimeoutException, ExecutionException, InterruptedException
  {
    TimeHack6435126.enableHighResolutionTimer();

    final CoEventService client_events = new CoEventService();
    client_events.onActivate();

    final CoEventService server_events = new CoEventService();
    server_events.onActivate();

    final CoClient client = new CoClient(client_events);
    client.startSynchronously(3L, TimeUnit.SECONDS);

    //final CoServer server = new CoServer(server_events);
    //server.startSynchronously(3L, TimeUnit.SECONDS);

    final Thread th = new Thread(() -> {
      try {
        System.in.read();
        client.shutDownSynchronously(3L, TimeUnit.SECONDS);
        // server.shutDownSynchronously(3L, TimeUnit.SECONDS);
      } catch (final Exception e) {
        LOG.error("failed: ", e);
      }
    });
    th.start();
    th.join();
  }
}
