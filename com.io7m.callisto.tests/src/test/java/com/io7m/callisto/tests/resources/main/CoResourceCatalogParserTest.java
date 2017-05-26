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

package com.io7m.callisto.tests.resources.main;

import com.io7m.callisto.resources.api.CoResourceCatalogParserReceiverType;
import com.io7m.callisto.resources.api.CoResourceCatalogParserType;
import com.io7m.callisto.resources.main.CoResourceCatalogParsers;
import com.io7m.callisto.tests.resources.api.CoResourceCatalogParserContract;

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.URI;

public final class CoResourceCatalogParserTest
  extends CoResourceCatalogParserContract
{
  @Override
  protected CoResourceCatalogParserType createParser(
    final String text,
    final CoResourceCatalogParserReceiverType receiver)
  {
    return new CoResourceCatalogParsers().createFromBufferedReader(
      this.uri(),
      new BufferedReader(new StringReader(text)),
      receiver);
  }

  @Override
  protected URI uri()
  {
    return URI.create("uurn:0");
  }
}
