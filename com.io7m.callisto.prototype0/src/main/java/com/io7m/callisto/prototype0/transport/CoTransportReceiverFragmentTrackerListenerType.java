package com.io7m.callisto.prototype0.transport;

import java.nio.ByteBuffer;

public interface CoTransportReceiverFragmentTrackerListenerType
  extends CoTransportReceiverFragmentTrackerMessageReceiverType
{
  void onFragmentStart(
    int id,
    int count,
    int message_type,
    int message_id,
    ByteBuffer data);

  void onFragmentSegment(
    int id,
    int index,
    ByteBuffer data);

  void onFragmentNotReady(
    int id);

  void onFragmentDiscarded(
    int id);
}
