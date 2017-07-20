package com.io7m.callisto.prototype0.client;

import com.io7m.callisto.core.CoImmutableStyleType;
import com.io7m.callisto.prototype0.events.CoEventType;
import org.immutables.value.Value;

@CoImmutableStyleType
@Value.Immutable
public interface CoClientTickEventType extends CoEventType
{
  @Value.Parameter
  int tickRate();
}
