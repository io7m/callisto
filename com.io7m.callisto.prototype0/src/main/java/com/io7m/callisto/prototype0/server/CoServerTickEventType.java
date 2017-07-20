package com.io7m.callisto.prototype0.server;

import com.io7m.callisto.core.CoImmutableStyleType;
import com.io7m.callisto.prototype0.events.CoEventType;
import org.immutables.value.Value;

@CoImmutableStyleType
@Value.Immutable
public interface CoServerTickEventType extends CoEventType
{
  @Value.Parameter
  int tickRate();
}
