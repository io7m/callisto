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

package com.io7m.callisto.prototype0.bytebuffers;

import com.io7m.jnull.NullCheck;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class ByteBufferInputStream extends InputStream
{
  private final ByteBuffer buffer;

  public ByteBufferInputStream(
    final ByteBuffer in_buffer)
  {
    this.buffer = NullCheck.notNull(in_buffer, "Buffer");
  }

  @Override
  public int read()
    throws IOException
  {
    if (this.buffer.hasRemaining()) {
      return (int) this.buffer.get() & 0xFF;
    }

    return -1;
  }

  @Override
  public int read(
    final byte[] data,
    final int offset,
    final int length)
    throws IOException
  {
    if (!this.buffer.hasRemaining()) {
      return -1;
    }

    final int r_length = Math.min(length, this.buffer.remaining());
    this.buffer.get(data, offset, r_length);
    return r_length;
  }
}