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

import com.io7m.callisto.resources.api.CoResourcePackageParserReceiverType;
import com.io7m.callisto.resources.api.CoResourcePackageParserType;
import com.io7m.callisto.resources.api.CoResource;
import com.io7m.callisto.resources.api.CoResourceID;
import mockit.Mocked;
import mockit.StrictExpectations;
import org.junit.Test;

import java.net.URI;
import java.util.Optional;

public abstract class CoResourcePackageParserProviderContract
{
  protected abstract CoResourcePackageParserType createParser(
    String text,
    CoResourcePackageParserReceiverType receiver);

  protected abstract URI uri();

  @Test
  public void testEmpty(
    final @Mocked CoResourcePackageParserReceiverType receiver)
    throws Exception
  {
    new StrictExpectations()
    {{
      receiver.onError(
        (URI) this.any,
        4,
        "No package name was specified.",
        Optional.empty());
    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("#");
    sb.append(System.lineSeparator());
    sb.append("  ");
    sb.append(System.lineSeparator());

    try (final CoResourcePackageParserType p =
           this.createParser(sb.toString(), receiver)) {
      p.run();
    }
  }

  @Test
  public void testNoPackageBeforeResource(
    final @Mocked CoResourcePackageParserReceiverType receiver)
    throws Exception
  {
    new StrictExpectations()
    {{
      receiver.onError(
        (URI) this.any,
        1,
        "No package name has been specified.",
        Optional.empty());

      receiver.onError(
        (URI) this.any,
        3,
        "No package name was specified.",
        Optional.empty());
    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("resource x y f.txt");
    sb.append(System.lineSeparator());

    try (final CoResourcePackageParserType p =
           this.createParser(sb.toString(), receiver)) {
      p.run();
    }
  }

  @Test
  public void testResource(
    final @Mocked CoResourcePackageParserReceiverType receiver)
    throws Exception
  {
    new StrictExpectations()
    {{
      receiver.onPackage("a.b.c");
      receiver.onResolveFile("/a/b/c", "f.txt");
      this.result = URI.create("urn:fake");

      receiver.onResource(CoResource.of(
        CoResourceID.of("a.b.c", "x"),
        "y",
        URI.create("urn:fake")
      ));
    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("package a.b.c");
    sb.append(System.lineSeparator());
    sb.append("resource x y f.txt");
    sb.append(System.lineSeparator());

    try (final CoResourcePackageParserType p =
           this.createParser(sb.toString(), receiver)) {
      p.run();
    }
  }

  @Test
  public void testUnknown(
    final @Mocked CoResourcePackageParserReceiverType receiver)
    throws Exception
  {
    new StrictExpectations()
    {{
      receiver.onWarning(
        CoResourcePackageParserProviderContract.this.uri(),
        1,
        "Unrecognized command: unknown");

      receiver.onError(
        (URI) this.any,
        3,
        "No package name was specified.",
        Optional.empty());
    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("unknown");
    sb.append(System.lineSeparator());

    try (final CoResourcePackageParserType p =
           this.createParser(sb.toString(), receiver)) {
      p.run();
    }
  }
}
