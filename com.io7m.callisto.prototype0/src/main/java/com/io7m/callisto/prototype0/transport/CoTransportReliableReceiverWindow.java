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
import com.io7m.jserial.core.SerialNumberIntType;
import com.io7m.junreachable.UnimplementedCodeException;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import it.unimi.dsi.fastutil.ints.IntSortedSets;

public final class CoTransportReliableReceiverWindow
{
  private int receive;
  private final IntRBTreeSet received;
  private final IntSortedSet received_view;
  private final IntRBTreeSet missed;
  private final IntSortedSet missed_view;
  private final int maximum_distance;
  private final SerialNumberIntType serial;
  private int receive_before_missing;

  public CoTransportReliableReceiverWindow(
    final SerialNumberIntType in_serial,
    final int in_receive,
    final int in_maximum_distance)
  {
    this.serial = NullCheck.notNull(in_serial, "Serial");
    this.maximum_distance = in_maximum_distance;
    this.receive = in_receive;
    this.receive_before_missing = in_receive;
    this.received = new IntRBTreeSet(this::compareSequenceNumbers);
    this.received_view = IntSortedSets.unmodifiable(this.received);
    this.missed = new IntRBTreeSet(this::compareSequenceNumbers);
    this.missed_view = IntSortedSets.unmodifiable(this.missed);
  }

  private int compareSequenceNumbers(
    final Integer x0,
    final Integer x1)
  {
    return this.serial.compare(x0.intValue(), x1.intValue());
  }

  public void receive(
    final int r)
  {
    if (this.serial.distanceUnsigned(this.receive, r) > this.maximum_distance) {
      throw new UnimplementedCodeException();
    }

    this.received.add(r);
    this.missed.remove(r);

    /*
     * Make a note of any missed packets between r and the previous received
     * packet.
     */

    for (int k = this.serial.add(this.receive_before_missing, 1);
         this.serial.compare(k, r) < 0;
         k = this.serial.add(k, 1)) {
      if (!this.received.contains(k)) {
        this.missed.add(k);
      }
    }

    /*
     * If there are any missed packets, make a note of the highest sequence
     * number that is less than any of the missed numbers. Otherwise, the
     * highest sequence number is equal to the highest received packet.
     */

    if (!this.missed.isEmpty()) {
      this.receive_before_missing =
        this.serial.add(this.missed.firstInt(), -1);
    } else {
      if (!this.received.isEmpty()) {
        this.receive_before_missing = this.received.lastInt();
      }
    }
  }

  public void reset()
  {
    if (!this.received.isEmpty()) {
      this.receive_before_missing = this.received.lastInt();
    }
    this.received.clear();
    this.missed.clear();
    this.receive = this.receive_before_missing;
  }

  public int receivedSequence() { return this.receive; }

  public int receivedSequenceBeforeMissing()
  {
    return this.receive_before_missing;
  }

  public IntSortedSet received()
  {
    return this.received_view;
  }

  public IntSortedSet missed()
  {
    return this.missed_view;
  }
}
