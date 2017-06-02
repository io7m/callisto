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

import com.io7m.callisto.resources.api.CoResourceResolverType;
import com.io7m.callisto.stringtables.api.CoStringTableParserProviderType;
import com.io7m.callisto.stringtables.api.CoStringTableProviderType;
import com.io7m.callisto.stringtables.main.CoStringTableProvider;
import com.io7m.callisto.tests.stringtables.api.CoStringTableProviderContract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoStringTableProviderTest
  extends CoStringTableProviderContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoStringTableProviderTest.class);
  }

  @Override
  protected CoStringTableProviderType provider(
    final CoStringTableParserProviderType parsers,
    final CoResourceResolverType resolver)
    throws Exception
  {
    final CoStringTableProvider p = new CoStringTableProvider();
    p.onResourceResolverSet(resolver);
    p.onStringTableParserProviderSet(parsers);
    p.onActivate();
    return p;
  }

  @Override
  protected Logger log()
  {
    return LOG;
  }
}
