package com.io7m.callisto.prototype0.transport.pipeline;

public interface CoPipelineReliableSequenceTrackerEventType
  extends CoPipelineEventType
{
  enum Type
  {
    DROPPED
  }

  Type type();
}
