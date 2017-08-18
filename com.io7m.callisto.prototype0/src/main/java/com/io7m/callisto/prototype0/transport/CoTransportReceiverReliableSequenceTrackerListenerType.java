package com.io7m.callisto.prototype0.transport;

import com.io7m.callisto.prototype0.transport.messages.CoPacket;

import java.nio.ByteBuffer;

public interface CoTransportReceiverReliableSequenceTrackerListenerType
{
  void onReceiveError(
    int sequence,
    Error error,
    String message);

  void onReceivePacketArrived(
    int sequence,
    CoPacket packet);

  void onReceivePacketRangeMissing(
    int lower,
    int upper);

  void onReceivePacketDiscarded(
    int sequence,
    CoPacket packet);

  void onReceivePacketFragmentNotReady(
    int sequence,
    int fragment_id);

  Keep onReceivePacketReassembledFragmentReadyForDelivery(
    int sequence,
    int fragment_id,
    int message_id,
    int message_type,
    ByteBuffer message_data);

  enum Keep {
    KEEP,
    DISCARD
  }

  Keep onReceivePacketReadyForDelivery(
    int sequence,
    CoPacket packet);

  enum Error {
    RECEIVER_DUPLICATE,
    RECEIVER_TOO_OLD,
    RECEIVER_FRAGMENT_PROBLEM,
  }
}
