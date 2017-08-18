package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.core.CoImmutableStyleType;
import com.io7m.callisto.prototype0.transport.messages.CoMessage;
import org.immutables.value.Value;

@CoImmutableStyleType
@Value.Immutable
public interface CoPipelineMessageUnreliableType extends CoPipelineMessageType
{
  @Override
  default Type type()
  {
    return Type.UNRELIABLE;
  }

  @Value.Parameter
  CoMessage message();
}
