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

import com.io7m.callisto.prototype0.messages.CoPacket;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface CoTransportServerListenerType
{
  void onReceivePacketUnparseable(
    SocketAddress address,
    ByteBuffer data,
    Exception e);

  void onReceivePacketUnrecognized(
    SocketAddress address,
    CoPacket packet);

  void onReceivePacketUnexpected(
    SocketAddress address,
    CoPacket packet);

  void onClientConnectionCreated(
    CoTransportConnectionUsableType connection);

  void onClientConnectionClosed(
    CoTransportConnectionUsableType connection,
    String message);

  void onClientConnectionTimedOut(
    CoTransportConnectionUsableType connection);

  void onClientConnectionPacketSendReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketSendUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketSendReliableFragment(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketSendReceipt(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketDropUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionMessageReceived(
    CoTransportConnectionUsableType connection,
    int channel,
    String type_name,
    ByteBuffer data);

  void onClientConnectionPacketReceiveDeliverReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketReceiveDeliverUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketReceiveReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketReceiveUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketReceiveReliableFragment(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketReceiveReceipt(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onClientConnectionPacketSendPurgeReliable(
    CoTransportConnection connection,
    int channel,
    int sequence,
    int size);
}
