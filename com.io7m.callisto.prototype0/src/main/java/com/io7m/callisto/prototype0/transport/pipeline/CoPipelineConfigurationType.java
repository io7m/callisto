package com.io7m.callisto.prototype0.transport.pipeline;

import com.io7m.callisto.core.CoImmutableStyleType;
import com.io7m.jranges.RangeCheck;
import com.io7m.jranges.RangeInclusiveI;
import org.immutables.value.Value;

@CoImmutableStyleType
@Value.Immutable
public interface CoPipelineConfigurationType
{
  @Value.Parameter
  int connectionID();

  @Value.Parameter
  int ticksPerSecond();

  @Value.Parameter
  @Value.Default
  default int ticksTimeout()
  {
    return this.ticksPerSecond() * 10;
  }

  @Value.Parameter
  @Value.Default
  default int ticksReliableTTL()
  {
    return this.ticksPerSecond() * 2;
  }

  @Value.Parameter
  @Value.Default
  default int ticksPingRate()
  {
    return this.ticksPerSecond();
  }

  @Value.Parameter
  @Value.Default
  default int ticksAckRate()
  {
    return 3;
  }

  @Value.Parameter
  @Value.Default
  default int ackMaximumRetries()
  {
    return 10;
  }

  @Value.Check
  default void checkPreconditions()
  {
    RangeCheck.checkIncludedInInteger(
      this.ticksPerSecond(),
      "Ticks per second",
      new RangeInclusiveI(1, 60),
      "Valid ticks per second");

    RangeCheck.checkIncludedInInteger(
      this.ticksTimeout(),
      "Timeout in ticks",
      new RangeInclusiveI(1, this.ticksPerSecond() * 60),
      "Valid timeout values");

    RangeCheck.checkIncludedInInteger(
      this.ticksReliableTTL(),
      "Reliable packet TTL in ticks",
      new RangeInclusiveI(1, this.ticksPerSecond() * 60),
      "Valid TTL values");

    RangeCheck.checkIncludedInInteger(
      this.ticksPingRate(),
      "Ticks ping rate",
      new RangeInclusiveI(this.ticksPerSecond(), this.ticksPerSecond() * 60),
      "Valid ping rate");

    RangeCheck.checkIncludedInInteger(
      this.ticksAckRate(),
      "Ack rate in ticks",
      new RangeInclusiveI(1, this.ticksPerSecond() * 60),
      "Valid ack rate");

    RangeCheck.checkIncludedInInteger(
      this.ackMaximumRetries(),
      "Ack maximum retries",
      new RangeInclusiveI(1, 100),
      "Valid ack retry count");
  }
}
