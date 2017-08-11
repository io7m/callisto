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

package com.io7m.callisto.tests.prototype0;

import com.io7m.callisto.prototype0.ticks.CoTickDivisor;
import org.junit.Assert;
import org.junit.Test;

public final class CoTickDivisorTest
{
  @Test
  public void testDivisor60_30()
  {
    final CoTickDivisor div =
      new CoTickDivisor(60.0, 30.0);

    Assert.assertTrue(div.tickNow());

    for (int index = 0; index < 60; ++index) {
      Assert.assertFalse(div.tickNow());
      Assert.assertTrue(div.tickNow());
    }
  }

  @Test
  public void testDivisor60_60()
  {
    final CoTickDivisor div =
      new CoTickDivisor(60.0, 60.0);

    Assert.assertTrue(div.tickNow());

    for (int index = 0; index < 60; ++index) {
      Assert.assertTrue(div.tickNow());
      Assert.assertTrue(div.tickNow());
    }
  }

  @Test
  public void testDivisor60_15()
  {
    final CoTickDivisor div =
      new CoTickDivisor(60.0, 15.0);

    Assert.assertTrue(div.tickNow());

    for (int index = 0; index < 60; ++index) {
      Assert.assertFalse(div.tickNow());
      Assert.assertFalse(div.tickNow());
      Assert.assertFalse(div.tickNow());
      Assert.assertTrue(div.tickNow());
    }
  }
}
