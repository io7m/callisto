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

package com.io7m.callisto.tests.prototype0;

import com.io7m.callisto.prototype0.messages.CoStringConstantPoolUpdate;
import com.io7m.callisto.prototype0.messages.CoStringConstantPoolUpdateCompressed;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolEventUpdateReceived;
import com.io7m.callisto.prototype0.stringconstants.CoStringConstantPoolMessages;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class CoStringConstantPoolMessagesTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(CoStringConstantPoolMessagesTest.class);

  @Test
  public void testIdentity()
  {
    final Map<Integer, String> values = new HashMap<>();
    values.put(
      Integer.valueOf(0),
      CoStringConstantPoolMessages.eventCompressedUpdateTypeName());

    for (int index = 0; index < 300; ++index) {
      values.put(Integer.valueOf(index), "hello." + index);
    }

    final CoStringConstantPoolUpdate update =
      CoStringConstantPoolMessages.createEventUpdate(values);

    LOG.debug("uncompressed: {}",
              Integer.valueOf(update.getSerializedSize()));

    final CoStringConstantPoolUpdateCompressed update_compressed =
      CoStringConstantPoolMessages.createEventUpdateCompressed(update);

    LOG.debug("compressed: algorithm {} data size {} (size {})",
              update_compressed.getAlgorithm(),
              Integer.valueOf(update_compressed.getData().size()),
              Integer.valueOf(update_compressed.getSerializedSize()));

    final ByteBuffer bytes =
      update_compressed.toByteString().asReadOnlyByteBuffer();

    final CoStringConstantPoolUpdateCompressed update_compressed_parsed =
      CoStringConstantPoolMessages.parseEventUpdateCompressed(bytes);

    LOG.debug("compressed parsed: algorithm {} data size {}",
              update_compressed_parsed.getAlgorithm(),
              Integer.valueOf(update_compressed_parsed.getData().size()));

    final CoStringConstantPoolUpdate decompressed =
      CoStringConstantPoolMessages.parseEventUpdateCompressedDecompress(
        update_compressed_parsed);

    Assert.assertEquals(update, decompressed);
  }
}
