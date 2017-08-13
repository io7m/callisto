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

import com.io7m.callisto.prototype0.transport.messages.CoPacket;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

public interface CoTransportClientListenerType
{
  void onPacketReceiveUnparseable(
    SocketAddress address,
    ByteBuffer data,
    Exception e);

  void onPacketReceiveUnrecognized(
    SocketAddress address,
    CoPacket packet);

  void onPacketReceiveUnexpected(
    SocketAddress address,
    CoPacket packet);

  void onHelloTimedOut(
    SocketAddress address,
    String message);

  void onHelloSend(
    SocketAddress address,
    int attempt,
    int max_attempts);

  void onHelloRefused(
    SocketAddress address,
    String message);

  void onConnectionCreated(
    CoTransportConnectionUsableType connection);

  void onConnectionMessageReceived(
    CoTransportConnectionUsableType connection,
    int channel,
    String type_name,
    ByteBuffer data);

  void onConnectionSendReceipt(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionSendReliableFragment(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionSendUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionSendReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionClosed(
    CoTransportConnectionUsableType connection,
    String message);

  void onConnectionTimedOut(
    CoTransportConnectionUsableType connection);

  void onConnectionReceiveDeliverReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionReceiveDeliverUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionReceiveReliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionReceiveUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionReceiveReliableFragment(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionReceiveDropUnreliable(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionReceiveAck(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionSendReliableSaved(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionSendReliableExpired(
    CoTransportConnectionUsableType connection,
    int channel,
    int sequence,
    int size);

  void onConnectionReceivePing(
    CoTransportConnectionUsableType connection);

  void onConnectionSendPong(
    CoTransportConnectionUsableType connection);

  void onConnectionReceivePong(
    CoTransportConnectionUsableType connection);

  void onConnectionSendPing(
    CoTransportConnectionUsableType connection);

  void onChannelCreated(
    CoTransportConnectionUsableType connection,
    int channel);
}
