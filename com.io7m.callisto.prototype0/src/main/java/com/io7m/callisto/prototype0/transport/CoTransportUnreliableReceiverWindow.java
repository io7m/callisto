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

public final class CoTransportUnreliableReceiverWindow
{
  private int receive_start;
  private final SerialNumberIntType serial;
  private int receive_current;

  public CoTransportUnreliableReceiverWindow(
    final SerialNumberIntType in_serial,
    final int in_receive)
  {
    this.serial = NullCheck.notNull(in_serial, "Serial");
    this.receive_start = in_receive;
    this.receive_current = in_receive;
  }

  public void receive(
    final int incoming)
  {
    if (this.serial.compare(incoming, this.receive_current) >= 0) {
      this.receive_current = incoming;
    }
  }

  public void reset()
  {
    this.receive_start = this.receive_current;
  }

  public int receivedSequenceAtStartOfTick()
  {
    return this.receive_start;
  }

  public int receivedSequenceCurrent()
  {
    return this.receive_current;
  }
}
