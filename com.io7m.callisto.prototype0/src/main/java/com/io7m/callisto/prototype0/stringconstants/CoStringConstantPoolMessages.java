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

import com.google.protobuf.ByteString;
import com.io7m.callisto.prototype0.messages.CoStringConstantCompression;
import com.io7m.callisto.prototype0.messages.CoStringConstantPoolUpdate;
import com.io7m.callisto.prototype0.messages.CoStringConstantPoolUpdateCompressed;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public final class CoStringConstantPoolMessages
{
  private CoStringConstantPoolMessages()
  {

  }

  public static String eventCompressedUpdateTypeName()
  {
    return "event:com.io7m.callisto.stringconstants.compressed_update";
  }

  public static CoStringConstantPoolUpdate eventUpdate(
    final Map<Integer, String> view)
  {
    return CoStringConstantPoolUpdate.newBuilder()
      .putAllStrings(view)
      .build();
  }

  public static ByteBuffer eventUpdateSerialized(
    final Map<Integer, String> view)
  {
    return ByteBuffer.wrap(eventUpdate(view).toByteArray());
  }

  public static CoStringConstantPoolUpdateCompressed eventCompressedUpdate(
    final Map<Integer, String> view)
  {
    final CoStringConstantPoolUpdate update = eventUpdate(view);
    final Deflater def = new Deflater(9);
    try (final ByteArrayOutputStream b_out =
           new ByteArrayOutputStream(1024)) {
      try (final DeflaterOutputStream out =
             new DeflaterOutputStream(b_out, def)) {
        update.writeTo(out);

        final CoStringConstantPoolUpdateCompressed outer =
          CoStringConstantPoolUpdateCompressed.newBuilder()
            .setAlgorithm(CoStringConstantCompression.COMPRESSION_DEFLATE)
            .setData(ByteString.copyFrom(b_out.toByteArray()))
            .build();

        return outer;
      }
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }

  public static ByteBuffer eventCompressedUpdateSerialized(
    final Map<Integer, String> view)
  {
    return ByteBuffer.wrap(eventCompressedUpdate(view).toByteArray());
  }
}
