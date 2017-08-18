package com.io7m.callisto.prototype0.transport;

public interface CoTransportReceiverFragmentTrackerErrorReceiverType
{
  void onFragmentError(
    int id,
    Error e,
    String message);

  enum Error
  {
    FRAGMENT_ERROR_NONEXISTENT,
    FRAGMENT_ERROR_ALREADY_STARTED,
    FRAGMENT_ERROR_SIZE_MISMATCH,
    FRAGMENT_ERROR_SEGMENT_ALREADY_PROVIDED
  }
}
