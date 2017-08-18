package com.io7m.callisto.prototype0.transport;

import it.unimi.dsi.fastutil.ints.IntSet;

import java.nio.ByteBuffer;

public interface CoTransportReceiverFragmentTrackerMessageReceiverType
  extends CoTransportReceiverFragmentTrackerErrorReceiverType
{
  enum FragmentKeep
  {
    KEEP,
    DISCARD
  }

  FragmentKeep onFragmentReady(
    int sequence_initial,
    IntSet sequences,
    int fragment_id,
    int message_type,
    int message_id,
    ByteBuffer data);
}
