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
import com.google.protobuf.InvalidProtocolBufferException;
import com.io7m.callisto.prototype0.bytebuffers.ByteBufferInputStream;
import com.io7m.callisto.prototype0.events.CoEventSerializationIOException;
import com.io7m.callisto.prototype0.events.CoEventSerializationMalformedException;
import com.io7m.callisto.prototype0.messages.CoStringConstantCompression;
import com.io7m.callisto.prototype0.messages.CoStringConstantPoolUpdate;
import com.io7m.callisto.prototype0.messages.CoStringConstantPoolUpdateCompressed;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public final class CoStringConstantPoolMessages
{
  private CoStringConstantPoolMessages()
  {
    throw new UnreachableCodeException();
  }

  public static String eventCompressedUpdateTypeName()
  {
    return "event:com.io7m.callisto.stringconstants.compressed_update";
  }

  public static CoStringConstantPoolUpdate createEventUpdate(
    final Map<Integer, String> strings)
  {
    NullCheck.notNull(strings, "Strings");

    return CoStringConstantPoolUpdate.newBuilder()
      .putAllStrings(strings)
      .build();
  }

  public static CoStringConstantPoolUpdateCompressed createEventUpdateCompressed(
    final CoStringConstantPoolUpdate update)
  {
    NullCheck.notNull(update, "Update");

    final Deflater def = new Deflater(9);
    try (final ByteArrayOutputStream b_out =
           new ByteArrayOutputStream(1024)) {
      try (final DeflaterOutputStream out =
             new DeflaterOutputStream(b_out, def)) {
        update.writeTo(out);
        out.finish();
        out.flush();
      }

      final byte[] bytes = b_out.toByteArray();
      return CoStringConstantPoolUpdateCompressed.newBuilder()
        .setAlgorithm(CoStringConstantCompression.COMPRESSION_DEFLATE)
        .setData(ByteString.copyFrom(bytes))
        .build();
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }

  public static CoStringConstantPoolUpdateCompressed createEventUpdateCompressedDirectly(
    final Map<Integer, String> xs)
  {
    return createEventUpdateCompressed(createEventUpdate(xs));
  }

  public static ByteBuffer createEventUpdateCompressedSerialized(
    final Map<Integer, String> xs)
  {
    return createEventUpdateCompressed(createEventUpdate(xs)).toByteString().asReadOnlyByteBuffer();
  }

  public static CoStringConstantPoolUpdateCompressed parseEventUpdateCompressed(
    final ByteBuffer data)
  {
    try {
      return CoStringConstantPoolUpdateCompressed.parseFrom(data);
    } catch (final InvalidProtocolBufferException ex) {
      throw new CoEventSerializationMalformedException(ex);
    }
  }

  public static CoStringConstantPoolUpdate parseEventUpdateCompressedDecompress(
    final CoStringConstantPoolUpdateCompressed update)
  {
    switch (update.getAlgorithm()) {
      case COMPRESSION_DEFLATE: {
        final ByteBuffer compressed = update.getData().asReadOnlyByteBuffer();
        return parseEventUpdateCompressedDecompressDeflate(compressed);
      }
      case UNRECOGNIZED: {
        throw new CoEventSerializationMalformedException(
          "Unrecognized compression algorithm: " + update.getAlgorithmValue());
      }
    }
    throw new UnreachableCodeException();
  }

  private static CoStringConstantPoolUpdate parseEventUpdateCompressedDecompressDeflate(
    final ByteBuffer data)
  {
    final Inflater def = new Inflater();
    try (final ByteBufferInputStream b_in = new ByteBufferInputStream(data)) {
      try (final InflaterInputStream in = new InflaterInputStream(b_in, def)) {
        return CoStringConstantPoolUpdate.parseFrom(in);
      }
    } catch (final IOException e) {
      throw new CoEventSerializationIOException(e);
    }
  }

  public static CoStringConstantPoolUpdate parseEventUpdateCompressedDecompressDirectly(
    final ByteBuffer data)
  {
    return parseEventUpdateCompressedDecompress(parseEventUpdateCompressed(data));
  }
}
