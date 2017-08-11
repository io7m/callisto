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

import com.io7m.callisto.prototype0.events.CoEventNetworkSerializerType;
import com.io7m.callisto.prototype0.events.CoEventNetworkType;
import com.io7m.callisto.prototype0.events.CoEventSerializationException;
import com.io7m.callisto.prototype0.messages.CoStringConstantPoolUpdate;
import com.io7m.jnull.NullCheck;

import java.nio.ByteBuffer;

public final class CoStringConstantPoolEventUpdateReceivedSerializer
  implements CoEventNetworkSerializerType
{
  public CoStringConstantPoolEventUpdateReceivedSerializer()
  {

  }

  @Override
  public String toString()
  {
    return this.getClass().getCanonicalName();
  }

  @Override
  public String eventTypeName()
  {
    return CoStringConstantPoolMessages.eventCompressedUpdateTypeName();
  }

  @Override
  public Class<CoStringConstantPoolEventUpdateReceived> eventType()
  {
    return CoStringConstantPoolEventUpdateReceived.class;
  }

  @Override
  public CoStringConstantPoolEventUpdateReceived eventDeserialize(
    final ByteBuffer data)
    throws CoEventSerializationException
  {
    NullCheck.notNull(data, "Data");

    final CoStringConstantPoolUpdate update =
      CoStringConstantPoolMessages.parseEventUpdateCompressedDecompressDirectly(
        data);

    return CoStringConstantPoolEventUpdateReceived.builder()
      .putAllValues(update.getStringsMap())
      .build();
  }

  @Override
  public <T extends CoEventNetworkType> ByteBuffer eventSerialize(
    final T event)
    throws CoEventSerializationException
  {
    NullCheck.notNull(event, "Event");

    if (event instanceof CoStringConstantPoolEventUpdateReceived) {
      return CoStringConstantPoolMessages.createEventUpdateCompressedSerialized(
        ((CoStringConstantPoolEventUpdateReceived) event).values());
    }

    throw new UnsupportedOperationException(
      "Unsupported event type: " + event.getClass().getCanonicalName());
  }
}
