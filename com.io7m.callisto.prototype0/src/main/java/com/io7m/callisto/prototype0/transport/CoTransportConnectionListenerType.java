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

import com.io7m.callisto.prototype0.messages.CoMessage;

public interface CoTransportConnectionListenerType
{
  void onClosed(
    CoTransportConnectionUsableType connection,
    String message);

  void onTimedOut(
    CoTransportConnectionUsableType connection);

  void onEnqueuePacketReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onEnqueuePacketReliableRequeue(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onEnqueuePacketUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onEnqueuePacketReliableFragment(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onEnqueuePacketReceipt(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onSendPacketReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onSendPacketUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onSendPacketReliableFragment(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onSendPacketReceipt(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onReceiveDropPacketUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onMessageReceived(
    CoTransportConnectionUsableType connection,
    int channel,
    CoMessage message);

  void onReceivePacketDeliverReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onReceivePacketDeliverUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onReceivePacketReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onReceivePacketUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onReceivePacketReliableFragment(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onReceivePacketReceipt(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onSavedPacketReliableSave(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onSavedPacketReliableExpire(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);
}
