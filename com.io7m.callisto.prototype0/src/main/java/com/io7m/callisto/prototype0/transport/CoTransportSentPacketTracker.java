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

import com.io7m.jnull.NullCheck;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;

import java.util.Optional;

public final class CoTransportSentPacketTracker<P>
{
  private final Int2ReferenceOpenHashMap<P> packets;
  private final Int2IntOpenHashMap packet_ttls;
  private final int time_to_live;

  CoTransportSentPacketTracker(final int in_ttl)
  {
    this.time_to_live = in_ttl;
    this.packets = new Int2ReferenceOpenHashMap<>();
    this.packet_ttls = new Int2IntOpenHashMap();
  }

  public void sent(
    final int sequence,
    final P p)
  {
    this.packets.put(sequence, p);
    this.packet_ttls.put(sequence, this.time_to_live);
  }

  public Optional<P> get(
    final int sequence)
  {
    return Optional.ofNullable(this.packets.get(sequence));
  }

  interface ListenerType<P, T>
  {
    void onPacketExpired(
      T context,
      int sequence,
      P packet);
  }

  public <T> void expire(
    final T context,
    final ListenerType<P, T> listener)
  {
    NullCheck.notNull(context, "Context");
    NullCheck.notNull(listener, "Listener");

    final IntIterator iter = this.packet_ttls.keySet().iterator();
    while (iter.hasNext()) {
      final int sequence = iter.nextInt();
      final int time = this.packet_ttls.get(sequence);

      final P p = this.packets.get(sequence);
      final int time_next = time - 1;
      if (time_next <= 0) {
        this.packets.remove(sequence);
        iter.remove();
        listener.onPacketExpired(context, sequence, p);
        continue;
      }

      this.packet_ttls.put(sequence, time_next);
    }
  }

}
