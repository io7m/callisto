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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface CoResourceCatalogParserProviderType
{
  CoResourceCatalogParserType
  createFromBufferedReader(
    URI uri,
    BufferedReader reader,
    CoResourceCatalogParserReceiverType receiver)
    throws IOException;

  default CoResourceCatalogParserType
  createFromStream(
    final URI uri,
    final InputStream stream,
    final CoResourceCatalogParserReceiverType receiver)
    throws IOException
  {
    return this.createFromBufferedReader(
      uri,
      new BufferedReader(new InputStreamReader(stream, UTF_8)),
      receiver);
  }

  default CoResourceCatalogParserType
  createFromURI(
    final URI uri,
    final CoResourceCatalogParserReceiverType receiver)
    throws IOException
  {
    return this.createFromStream(
      uri,
      uri.toURL().openStream(),
      receiver);
  }
}
