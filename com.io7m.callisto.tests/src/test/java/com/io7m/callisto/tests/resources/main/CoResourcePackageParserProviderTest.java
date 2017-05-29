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

import com.io7m.callisto.resources.api.CoResourcePackageParserReceiverType;
import com.io7m.callisto.resources.api.CoResourcePackageParserType;
import com.io7m.callisto.resources.main.CoResourcePackageParserProvider;
import com.io7m.callisto.tests.resources.api.CoResourcePackageParserProviderContract;

import java.io.StringReader;
import java.net.URI;

public final class CoResourcePackageParserProviderTest
  extends CoResourcePackageParserProviderContract
{
  @Override
  protected CoResourcePackageParserType createParser(
    final String text,
    final CoResourcePackageParserReceiverType receiver)
  {
    return new CoResourcePackageParserProvider().createFromReader(
      new StringReader(text),
      this.uri(),
      receiver);
  }

  @Override
  protected URI uri()
  {
    return URI.create("urn:test");
  }
}
