package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.core.CoImmutableStyleType;
import org.immutables.value.Value;

@CoImmutableStyleType
@Value.Immutable
public interface CoPipelineReliableSequenceTrackerEventDroppedType
  extends CoPipelineReliableSequenceTrackerEventType
{
  @Override
  default Type type()
  {
    return Type.DROPPED;
  }

  @Value.Parameter
  int sequenceDropped();
}
