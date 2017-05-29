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

package com.io7m.callisto.resources.api;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The type of parser providers.
 */

public interface CoResourcePackageParserProviderType
{
  /**
   * Create a new parser from the given buffered reader.
   *
   * @param reader   The buffered reader
   * @param uri      The URI of the reader source, for diagnostic messages
   * @param receiver The receiver of parser events
   *
   * @return A new parser
   */

  CoResourcePackageParserType createFromBufferedReader(
    BufferedReader reader,
    URI uri,
    CoResourcePackageParserReceiverType receiver);

  /**
   * Create a new parser from the given input stream.
   *
   * @param stream   The input stream
   * @param uri      The URI of the stream source, for diagnostic messages
   * @param receiver The receiver of parser events
   *
   * @return A new parser
   */

  default CoResourcePackageParserType createFromInputStream(
    final InputStream stream,
    final URI uri,
    final CoResourcePackageParserReceiverType receiver)
  {
    return this.createFromBufferedReader(
      new BufferedReader(new InputStreamReader(stream, UTF_8)),
      uri,
      receiver);
  }

  /**
   * Create a new parser from the given reader.
   *
   * @param reader   The reader
   * @param uri      The URI of the reader source, for diagnostic messages
   * @param receiver The receiver of parser events
   *
   * @return A new parser
   */

  default CoResourcePackageParserType createFromReader(
    final Reader reader,
    final URI uri,
    final CoResourcePackageParserReceiverType receiver)
  {
    return this.createFromBufferedReader(
      new BufferedReader(reader),
      uri,
      receiver);
  }
}
