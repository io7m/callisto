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

package com.io7m.callisto.tests.resources.api;

import com.io7m.callisto.resources.api.CoResource;
import com.io7m.callisto.resources.api.CoResourceCatalogParserReceiverType;
import com.io7m.callisto.resources.api.CoResourceCatalogParserType;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;

public abstract class CoResourceCatalogParserContract
{
  protected abstract CoResourceCatalogParserType createParser(
    String text,
    CoResourceCatalogParserReceiverType receiver);

  protected abstract URI uri();

  @Test
  public final void testEmpty(
    final @Mocked CoResourceCatalogParserReceiverType receiver)
  {
    final CoResourceCatalogParserType p = this.createParser("", receiver);

    new StrictExpectations()
    {{

    }};

    p.run();
  }

  @Test
  public final void testCommented(
    final @Mocked CoResourceCatalogParserReceiverType receiver)
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("#");
    sb.append(System.lineSeparator());
    sb.append(" ");
    sb.append(System.lineSeparator());
    sb.append("");
    sb.append(System.lineSeparator());

    final CoResourceCatalogParserType p =
      this.createParser(sb.toString(), receiver);

    new StrictExpectations()
    {{

    }};

    p.run();
  }

  @Test
  public final void testR_OK(
    final @Mocked CoResourceCatalogParserReceiverType receiver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("R /xyz.txt");
    sb.append(System.lineSeparator());

    final CoResourceCatalogParserType p =
      this.createParser(sb.toString(), receiver);

    new StrictExpectations()
    {{
      receiver.onResourceResolve("/xyz.txt");
      this.result = URI.create("urn:/xyz.dat");
      receiver.onResource(CoResource.of(URI.create("urn:/xyz.dat")));
    }};

    p.run();
  }

  @Test
  public final void testR_Failed(
    final @Mocked CoResourceCatalogParserReceiverType receiver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("R");
    sb.append(System.lineSeparator());

    final CoResourceCatalogParserType p =
      this.createParser(sb.toString(), receiver);

    new StrictExpectations()
    {{
      receiver.onError(
        CoResourceCatalogParserContract.this.uri(),
        1,
        this.anyString,
        Optional.empty());
    }};

    p.run();
  }

  @Test
  public final void testUnknownType(
    final @Mocked CoResourceCatalogParserReceiverType receiver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("UNKNOWN");
    sb.append(System.lineSeparator());

    final CoResourceCatalogParserType p =
      this.createParser(sb.toString(), receiver);

    new StrictExpectations()
    {{
      receiver.onWarning(
        CoResourceCatalogParserContract.this.uri(),
        1,
        "Unrecognized catalog entry type: UNKNOWN");
    }};

    p.run();
  }
}
