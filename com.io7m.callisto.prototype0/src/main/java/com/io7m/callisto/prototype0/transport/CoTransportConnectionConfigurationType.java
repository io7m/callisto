/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.callisto.prototype0.transport;

import com.io7m.callisto.core.CoImmutableStyleType;
import com.io7m.jranges.RangeCheck;
import com.io7m.jranges.RangeInclusiveI;
import org.immutables.value.Value;

@CoImmutableStyleType
@Value.Immutable
public interface CoTransportConnectionConfigurationType
{
  @Value.Parameter
  int ticksPerSecond();

  @Value.Parameter
  @Value.Default
  default int ticksTimeout()
  {
    return this.ticksPerSecond() * 10;
  }

  @Value.Parameter
  default int ticksReliableTTL()
  {
    return this.ticksPerSecond() * 2;
  }

  @Value.Parameter
  default int ticksPingRate()
  {
    return this.ticksPerSecond();
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
      this.ticksPerSecond(),
      "Ticks ping rate",
      new RangeInclusiveI(this.ticksPerSecond(), this.ticksPerSecond() * 60),
      "Valid ping rate");
  }
}
