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

import com.io7m.callisto.resources.api.CoResourceBundleParserFileResolverType;
import com.io7m.callisto.resources.api.CoResourceBundleParserResult;
import com.io7m.callisto.resources.api.CoResourceBundleParserType;
import mockit.Expectations;
import mockit.Mocked;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public abstract class CoResourceBundleParserProviderContract
{
  protected abstract CoResourceBundleParserType createParser(
    String text,
    CoResourceBundleParserFileResolverType resolver);

  protected abstract CoResourceBundleParserType createParserStream(
    InputStream stream,
    CoResourceBundleParserFileResolverType resolver);

  protected abstract URI uri();

  protected abstract Logger log();

  @Test
  public void testCBDNotFirst(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder(256);
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "The first command of the file must be the cbd command"));
    }
  }

  @Test
  public void testCBDNotSupported(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 9999 9999");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "Format version not supported"));
    }
  }

  @Test
  public void testCBDNotInteger(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd x y");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "Error parsing cbd version"));
    }
  }

  @Test
  public void testPackageDuplicate(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());
    sb.append("package a.b.c");
    sb.append(System.lineSeparator());
    sb.append("package a.b.c");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "Duplicate package declaration"));
    }
  }

  @Test
  public void testPackageBad(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());
    sb.append("package");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "Could not parse command"));
    }
  }

  @Test
  public void testResourceWithoutPackage(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());
    sb.append("resource x com.io7m.callisto.text /a/b/c.txt");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "No package name has been specified"));
    }
  }

  @Test
  public void testResourceFileMissing(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    new Expectations() {{
      resolver.resolve("/a/b/c.txt");
      this.result = new FileNotFoundException("/a/b/c.txt");
    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());
    sb.append("package a.b.c");
    sb.append(System.lineSeparator());
    sb.append("resource x com.io7m.callisto.text /a/b/c.txt");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "An exported file is missing"));
    }
  }

  @Test
  public void testResourceUnparseable0(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    new Expectations() {{

    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());
    sb.append("package a.b.c");
    sb.append(System.lineSeparator());
    sb.append("resource x");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "Could not parse command"));
    }
  }

  @Test
  public void testResourceUnparseable1(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    new Expectations() {{

    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());
    sb.append("package a.b.c");
    sb.append(System.lineSeparator());
    sb.append("resource x y");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "Could not parse command"));
    }
  }

  @Test
  public void testResourceUnparseable2(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    new Expectations() {{

    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());
    sb.append("package a.b.c");
    sb.append(System.lineSeparator());
    sb.append("resource x y z w");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "Could not parse command"));
    }
  }

  @Test
  public void testResourceDuplicate(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    new Expectations() {{
      resolver.resolve("/a/b/c.txt");
      this.result = URI.create("urn:c.txt");
    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());
    sb.append("package a.b.c");
    sb.append(System.lineSeparator());
    sb.append("resource x com.io7m.callisto.text /a/b/c.txt");
    sb.append(System.lineSeparator());
    sb.append("resource x com.io7m.callisto.text /a/b/c.txt");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString(
          "A resource is already exported"));
    }
  }

  @Test
  public void testUnrecognizedCommand(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    new Expectations() {{

    }};

    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());
    sb.append("unknown");
    sb.append(System.lineSeparator());

    try (final CoResourceBundleParserType p =
           this.createParser(sb.toString(), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(0L, (long) result.errors().size());
      Assert.assertEquals(1L, (long) result.warnings().size());
      Assert.assertThat(
        result.warnings().get(0).message(),
        StringContains.containsString(
          "Received an unrecognized command"));
    }
  }

  @Test
  public void testIOException(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    final StringBuilder sb = new StringBuilder(256);
    sb.append("cbd 1 0");
    sb.append(System.lineSeparator());

    final InputStream stream = new InputStream()
    {
      @Override
      public int read()
        throws IOException
      {
        throw new IOException("Broken");
      }
    };

    try (final CoResourceBundleParserType p =
           this.createParserStream(stream, resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertEquals(1L, (long) result.errors().size());
      Assert.assertThat(
        result.errors().get(0).message(),
        StringContains.containsString("Broken"));
    }
  }

  private void dumpErrors(
    final CoResourceBundleParserResult result)
  {
    result.errors().forEach(e -> {
      this.log().error(
        "{}:{}: {}",
        e.uri(),
        Integer.valueOf(e.line()),
        e.message());
      e.exception().ifPresent(ex -> {
        this.log().error("", ex);
      });
    });
  }
}
