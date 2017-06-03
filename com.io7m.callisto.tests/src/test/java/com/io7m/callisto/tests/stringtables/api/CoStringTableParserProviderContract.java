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

package com.io7m.callisto.tests.stringtables.api;

import com.io7m.callisto.stringtables.api.CoStringTableExceptionNonexistent;
import com.io7m.callisto.stringtables.api.CoStringTableParserResult;
import com.io7m.callisto.stringtables.api.CoStringTableParserType;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.stream.Collectors;

public abstract class CoStringTableParserProviderContract
{
  protected abstract CoStringTableParserType createParserStream(
    InputStream stream,
    Locale locale)
    throws Exception;

  protected abstract CoStringTableParserType createParserStream(
    InputStream stream,
    String language)
    throws Exception;

  protected abstract URI uri();

  protected abstract Logger log();

  private static InputStream stream(
    final String file)
  {
    return CoStringTableParserProviderContract.class.getResourceAsStream(file);
  }

  @Rule public ExpectedException expected = ExpectedException.none();

  @Test
  public final void testSimpleNoString()
    throws Exception
  {
    final Locale locale = Locale.ENGLISH;

    try (final CoStringTableParserType p =
           this.createParserStream(stream("simple.cstx"), locale)) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertTrue(result.errors().isEmpty());

      this.expected.expect(CoStringTableExceptionNonexistent.class);
      this.expected.expectMessage(
        StringContains.containsString("nonexistent"));
      result.table().text("nonexistent");
    }
  }

  @Test
  public final void testDuplicateString()
    throws Exception
  {
    try (final CoStringTableParserType p =
           this.createParserStream(stream("duplicate.cstx"), "eng")) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains("Duplicate unique value"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testIllFormed0()
    throws Exception
  {
    try (final CoStringTableParserType p =
           this.createParserStream(stream("ill-formed-0.cstx"), "eng")) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains(
            "must be terminated by the matching end-tag"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testInvalid0()
    throws Exception
  {
    try (final CoStringTableParserType p =
           this.createParserStream(stream("invalid-0.cstx"), "eng")) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains(
            "String declared outside of string table"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testInvalid1()
    throws Exception
  {
    try (final CoStringTableParserType p =
           this.createParserStream(stream("invalid-1.cstx"), "eng")) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);

      Assert.assertFalse(result.errors().isEmpty());
      Assert.assertFalse(
        result.errors()
          .stream()
          .filter(e -> e.message().contains(
            "Text specified outside of string declaration"))
          .collect(Collectors.toList())
          .isEmpty());
    }
  }

  @Test
  public final void testSimpleUnknown()
    throws Exception
  {
    /*
     * A request for an unknown language will result in the strings for the
     * default language instead.
     */

    try (final CoStringTableParserType p =
           this.createParserStream(stream("simple.cstx"), "alien")) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertTrue(result.errors().isEmpty());
      Assert.assertEquals("Hello world.", result.table().text("hello"));
    }
  }

  @Test
  public final void testSimpleEnglish()
    throws Exception
  {
    final Locale locale = Locale.ENGLISH;

    try (final CoStringTableParserType p =
           this.createParserStream(stream("simple.cstx"), locale)) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertTrue(result.errors().isEmpty());
      Assert.assertEquals("Hello world.", result.table().text("hello"));
    }
  }

  @Test
  public final void testSimpleGerman()
    throws Exception
  {
    final Locale locale = Locale.GERMAN;

    try (final CoStringTableParserType p =
           this.createParserStream(stream("simple.cstx"), locale)) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertTrue(result.errors().isEmpty());
      Assert.assertEquals("Hallo welt.", result.table().text("hello"));
    }
  }

  @Test
  public final void testSimpleFrench()
    throws Exception
  {
    final Locale locale = Locale.FRENCH;

    try (final CoStringTableParserType p =
           this.createParserStream(stream("simple.cstx"), locale)) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertTrue(result.errors().isEmpty());
      Assert.assertEquals("Bonjour le monde.", result.table().text("hello"));
    }
  }

  @Test
  public final void testSimpleRussian()
    throws Exception
  {
    try (final CoStringTableParserType p =
           this.createParserStream(stream("simple.cstx"), "rus")) {
      final CoStringTableParserResult result = p.parse();

      this.dumpErrors(result);
      Assert.assertTrue(result.errors().isEmpty());
      Assert.assertEquals("Привет мир.", result.table().text("hello"));
    }
  }

  private void dumpErrors(
    final CoStringTableParserResult result)
  {
    result.errors().forEach(e -> {
      this.log().error(
        "{}:{}: {}",
        e.uri(),
        Integer.valueOf(e.line()),
        e.message());
      e.exception().ifPresent(ex -> this.log().error("", ex));
    });
  }
}
