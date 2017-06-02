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

package com.io7m.callisto.stringtables.api;

import java.io.InputStream;
import java.net.URI;
import java.util.Locale;

/**
 * The type of parser providers.
 */

public interface CoStringTableParserProviderType
{
  /**
   * Create a new parser from the given input stream.
   *
   * @param stream The input stream
   * @param uri    The URI of the stream source, for diagnostic messages
   * @param locale The locale
   *
   * @return A new parser
   */

  default CoStringTableParserType createParserFromStream(
    final InputStream stream,
    final URI uri,
    final Locale locale)
  {
    return this.createParserFromStream(stream, uri, locale.getISO3Language());
  }

  /**
   * Create a new parser from the given input stream.
   *
   * @param stream The input stream
   * @param uri    The URI of the stream source, for diagnostic messages
   * @param language The language
   *
   * @return A new parser
   */

  CoStringTableParserType createParserFromStream(
    InputStream stream,
    URI uri,
    String language);
}
