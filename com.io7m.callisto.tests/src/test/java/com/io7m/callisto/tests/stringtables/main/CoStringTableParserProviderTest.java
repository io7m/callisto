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

package com.io7m.callisto.tests.stringtables.main;

import com.io7m.callisto.stringtables.api.CoStringTableParserType;
import com.io7m.callisto.stringtables.main.CoStringTableParserProvider;
import com.io7m.callisto.tests.stringtables.api.CoStringTableParserProviderContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.Locale;

public final class CoStringTableParserProviderTest
  extends CoStringTableParserProviderContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoStringTableParserProviderTest.class);
  }

  @Override
  protected CoStringTableParserType createParserStream(
    final InputStream stream,
    final Locale locale)
    throws Exception
  {
    final CoStringTableParserProvider provider =
      new CoStringTableParserProvider();
    provider.onActivate();
    return provider.createParserFromStream(stream, this.uri(), locale);
  }

  @Override
  protected CoStringTableParserType createParserStream(
    final InputStream stream,
    final String language)
    throws Exception
  {
    final CoStringTableParserProvider provider =
      new CoStringTableParserProvider();
    provider.onActivate();
    return provider.createParserFromStream(stream, this.uri(), language);
  }

  @Override
  protected URI uri()
  {
    return URI.create("urn:test");
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }
}
