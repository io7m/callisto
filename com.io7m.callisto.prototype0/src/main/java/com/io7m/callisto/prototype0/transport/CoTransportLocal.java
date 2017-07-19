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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class CoTransportLocal
{
  private final Peer peer0;
  private final Peer peer1;

  private CoTransportLocal(
    final Peer in_p0,
    final Peer in_p1)
  {
    this.peer0 = NullCheck.notNull(in_p0, "Peer 0");
    this.peer1 = NullCheck.notNull(in_p1, "Peer 1");
  }

  public CoTransportType peer0()
  {
    return this.peer0;
  }

  public CoTransportType peer1()
  {
    return this.peer1;
  }

  public static CoTransportLocal create()
  {
    final Peer p0 = new Peer();
    final Peer p1 = new Peer();
    p0.setRemote(p1);
    p1.setRemote(p0);
    return new CoTransportLocal(p0, p1);
  }

  private static final class Peer implements CoTransportType
  {
    private ConcurrentLinkedQueue<CoEventType> receive;
    private volatile Peer remote;

    Peer()
    {
      this.receive = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void sendEvent(
      final CoEventType e)
    {
      this.remote.receive.add(NullCheck.notNull(e, "Event"));
    }

    @Override
    public Queue<CoEventType> receiveEvents()
    {
      final ArrayDeque<CoEventType> xs = new ArrayDeque<>(this.receive.size());
      final Iterator<CoEventType> iter = this.receive.iterator();
      while (iter.hasNext()) {
        xs.add(iter.next());
        iter.remove();
      }
      return xs;
    }

    private void setRemote(
      final Peer in_remote)
    {
      this.remote = NullCheck.notNull(in_remote, "Remote");
    }
  }
}
