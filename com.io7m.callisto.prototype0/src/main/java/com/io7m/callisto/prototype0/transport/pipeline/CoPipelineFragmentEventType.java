package com.io7m.callisto.prototype0.transport.pipeline;

public interface CoPipelineFragmentEventType
{
  enum Type
  {
    FRAGMENT_STARTED,
    FRAGMENT_COMPLETED
  }

  Type type();

  int fragmentID();
}
