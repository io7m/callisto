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
import com.io7m.callisto.resources.api.CoResourceBundleParserFileResolverType;
import com.io7m.callisto.resources.api.CoResourceBundleParserResult;
import com.io7m.callisto.resources.api.CoResourceBundleParserType;
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourcePackageDeclaration;
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
import java.util.Map;
import java.util.stream.Collectors;

public abstract class CoResourceBundleParserProviderContract
{
  protected abstract CoResourceBundleParserType createParserStream(
    InputStream stream,
    CoResourceBundleParserFileResolverType resolver)
    throws Exception;

  protected abstract URI uri();

  protected abstract Logger log();

  @Test
  public final void testCBDNotSupported(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    try (final CoResourceBundleParserType p =
           this.createParserStream(stream("version-unsupported.crbx"), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains("Unsupported format"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  private static InputStream stream(
    final String file)
  {
    return CoResourceBundleParserProviderContract.class.getResourceAsStream(file);
  }

  @Test
  public final void testPackageDuplicate(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    try (final CoResourceBundleParserType p =
           this.createParserStream(stream("package-duplicate.crbx"), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains("Duplicate unique value [a.b.c] declared for identity constraint of element \"bundle\""))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testUnparseable0(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    try (final CoResourceBundleParserType p =
           this.createParserStream(stream("ill-formed-0.crbx"), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains("terminated by the matching end-tag"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testInvalid0(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    try (final CoResourceBundleParserType p =
           this.createParserStream(stream("invalid-0.crbx"), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains("Invalid content was found starting with element"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testInvalid1(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    try (final CoResourceBundleParserType p =
           this.createParserStream(stream("invalid-1.crbx"), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains("No package has been defined"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testResourceFileMissing(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    new Expectations() {{
      resolver.resolve("/a/b/c/file.txt");
      this.result = new FileNotFoundException("/a/b/c/file.txt");
    }};

    try (final CoResourceBundleParserType p =
           this.createParserStream(stream("file-missing.crbx"), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains("An exported file is missing"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testResourceDuplicate(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    new Expectations() {{
      resolver.resolve("/a/b/c/file.txt");
      this.result = URI.create("urn:c.txt");
    }};

    try (final CoResourceBundleParserType p =
           this.createParserStream(stream("duplicate-resource.crbx"), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains("A resource is already exported"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testSimple(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
    new Expectations() {{
      resolver.resolve("/a/b/c/file0.txt");
      this.result = URI.create("urn:file0.txt");
      resolver.resolve("/a/b/c/file1.txt");
      this.result = URI.create("urn:file1.txt");
      resolver.resolve("/a/b/c/file2.txt");
      this.result = URI.create("urn:file2.txt");
    }};

    try (final CoResourceBundleParserType p =
           this.createParserStream(stream("simple.crbx"), resolver)) {
      final CoResourceBundleParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertTrue(result.errors().isEmpty());

      final Map<String, CoResourcePackageDeclaration> packs = result.packages();
      Assert.assertEquals(2L, (long) packs.size());
      Assert.assertTrue(packs.containsKey("a.b.c"));
      Assert.assertTrue(packs.containsKey("a.b.d"));

      {
        final CoResourcePackageDeclaration pack = packs.get("a.b.c");
        final Map<CoResourceID, CoResource> resources = pack.resources();
        Assert.assertEquals(3L, (long) resources.size());
        final CoResource r0 =
          resources.get(CoResourceID.of("a.b.c", "x"));
        final CoResource r1 =
          resources.get(CoResourceID.of("a.b.c", "y"));
        final CoResource r2 =
          resources.get(CoResourceID.of("a.b.c", "z"));

        Assert.assertEquals("a.b.c.x", r0.id().qualifiedName());
        Assert.assertEquals("text", r0.type());
        Assert.assertEquals("a.b.c.y", r1.id().qualifiedName());
        Assert.assertEquals("text", r1.type());
        Assert.assertEquals("a.b.c.z", r2.id().qualifiedName());
        Assert.assertEquals("text", r2.type());
      }

      {
        final CoResourcePackageDeclaration pack = packs.get("a.b.d");
        final Map<CoResourceID, CoResource> resources = pack.resources();
        Assert.assertEquals(3L, (long) resources.size());
        final CoResource r0 =
          resources.get(CoResourceID.of("a.b.d", "x"));
        final CoResource r1 =
          resources.get(CoResourceID.of("a.b.d", "y"));
        final CoResource r2 =
          resources.get(CoResourceID.of("a.b.d", "z"));

        Assert.assertEquals("a.b.d.x", r0.id().qualifiedName());
        Assert.assertEquals("text", r0.type());
        Assert.assertEquals("a.b.d.y", r1.id().qualifiedName());
        Assert.assertEquals("text", r1.type());
        Assert.assertEquals("a.b.d.z", r2.id().qualifiedName());
        Assert.assertEquals("text", r2.type());
      }
    }
  }

  @Test
  public final void testIOException(
    final @Mocked CoResourceBundleParserFileResolverType resolver)
    throws Exception
  {
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
