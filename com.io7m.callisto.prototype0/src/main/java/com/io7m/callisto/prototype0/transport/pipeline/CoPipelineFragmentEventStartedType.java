package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.core.CoImmutableStyleType;
import org.immutables.value.Value;

@CoImmutableStyleType
@Value.Immutable
public interface CoPipelineFragmentEventStartedType
  extends CoPipelineFragmentEventType
{
  @Override
  default Type type()
  {
    return Type.FRAGMENT_STARTED;
  }

  @Override
  @Value.Parameter
  int fragmentID();
}
