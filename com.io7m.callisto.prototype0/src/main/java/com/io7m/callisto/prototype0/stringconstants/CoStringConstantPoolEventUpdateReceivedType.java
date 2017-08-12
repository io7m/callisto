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

package com.io7m.callisto.prototype0.stringconstants;

import com.io7m.callisto.core.CoImmutableStyleType;
import com.io7m.callisto.prototype0.events.CoEventNetworkType;
import com.io7m.callisto.prototype0.transport.CoTransportConnectionUsableType;
import org.immutables.value.Value;

import java.util.Map;

@CoImmutableStyleType
@Value.Immutable
public interface CoStringConstantPoolEventUpdateReceivedType
  extends CoStringConstantPoolEventType, CoEventNetworkType
{
  @Value.Parameter
  Map<Integer, String> values();

  @Override
  default Type type()
  {
    return Type.STRING_CONSTANT_POOL_UPDATE_RECEIVED;
  }

  @Override
  default CoTransportConnectionUsableType.Reliability reliability()
  {
    return CoTransportConnectionUsableType.Reliability.MESSAGE_RELIABLE;
  }
}
