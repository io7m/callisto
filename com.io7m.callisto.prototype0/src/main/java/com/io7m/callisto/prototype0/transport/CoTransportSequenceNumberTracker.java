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

import com.io7m.jserial.core.SerialNumber24;
import com.io7m.jserial.core.SerialNumberIntType;

public final class CoTransportSequenceNumberTracker
{
  private final SerialNumberIntType serial;
  private final CoTransportReliableReceiverWindow reliable_window;
  private final CoTransportUnreliableReceiverWindow unreliable_window;
  private int reliable_send_next;
  private int message_send_next;
  private int unreliable_send_next;
  private int ack_send_next;

  public CoTransportSequenceNumberTracker()
  {
    this.serial = SerialNumber24.get();
    this.reliable_window =
      new CoTransportReliableReceiverWindow(this.serial, 0, 180);
    this.unreliable_window =
      new CoTransportUnreliableReceiverWindow(this.serial, 0);

    this.reliable_send_next = 0;
    this.unreliable_send_next = 0;
    this.message_send_next = 0;
    this.reliable_send_next = 0;
  }

  public SerialNumberIntType serial()
  {
    return this.serial;
  }

  public int messageToSendNext()
  {
    return this.message_send_next;
  }

  public void messageSend()
  {
    this.message_send_next = this.serial.add(this.message_send_next, 1);
  }

  public void unreliableSend()
  {
    this.unreliable_send_next = this.serial.add(this.unreliable_send_next, 1);
  }

  public int unreliableToSendNext()
  {
    return this.unreliable_send_next;
  }

  public CoTransportReliableReceiverWindow reliableReceiverWindow()
  {
    return this.reliable_window;
  }

  public void reliableSend()
  {
    this.reliable_send_next = this.serial.add(this.reliable_send_next, 1);
  }

  public int reliableToSendNext()
  {
    return this.reliable_send_next;
  }

  public void ackSend()
  {
    this.ack_send_next = this.serial.add(this.ack_send_next, 1);
  }

  public int ackToSendNext()
  {
    return this.ack_send_next;
  }

  public boolean reliableShouldBeQueued(
    final int sequence_incoming)
  {
    final int sequence_current = this.reliable_window.receivedSequence();
    return this.serial().compare(sequence_incoming, sequence_current) >= 0;
  }

  public boolean unreliableShouldBeQueued(
    final int sequence_incoming)
  {
    final int sequence_current =
      this.unreliable_window.receivedSequenceAtStartOfTick();
    return this.serial().compare(sequence_incoming, sequence_current) >= 0;
  }

  public CoTransportUnreliableReceiverWindow unreliableReceiverWindow()
  {
    return this.unreliable_window;
  }
}
