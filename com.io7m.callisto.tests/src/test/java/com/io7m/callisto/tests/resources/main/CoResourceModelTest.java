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

import com.io7m.callisto.resources.api.CoResourceModelType;
import com.io7m.callisto.resources.main.CoResourceModel;
import com.io7m.callisto.resources.main.CoResourceBundleParserProvider;
import com.io7m.callisto.tests.resources.api.CoResourceModelContract;

public final class CoResourceModelTest extends CoResourceModelContract
{
  @Override
  protected CoResourceModelType createEmptyModel()
    throws Exception
  {
    final CoResourceBundleParserProvider parsers =
      new CoResourceBundleParserProvider();

    parsers.onActivate();
    return new CoResourceModel(parsers);
  }
}
