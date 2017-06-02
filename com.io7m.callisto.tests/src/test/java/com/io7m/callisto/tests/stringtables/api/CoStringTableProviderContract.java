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

import com.io7m.callisto.resources.api.CoResource;
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourceLookupResult;
import com.io7m.callisto.resources.api.CoResourceResolverType;
import com.io7m.callisto.stringtables.api.CoStringTableExceptionUnparseable;
import com.io7m.callisto.stringtables.api.CoStringTableParserError;
import com.io7m.callisto.stringtables.api.CoStringTableParserProviderType;
import com.io7m.callisto.stringtables.api.CoStringTableParserResult;
import com.io7m.callisto.stringtables.api.CoStringTableParserType;
import com.io7m.callisto.stringtables.api.CoStringTableProviderType;
import com.io7m.callisto.stringtables.api.CoStringTableType;
import mockit.Expectations;
import mockit.Mocked;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class CoStringTableProviderContract
{
  protected abstract CoStringTableProviderType provider(
    final CoStringTableParserProviderType parsers,
    final CoResourceResolverType resolver)
    throws Exception;

  protected abstract Logger log();

  @Rule public ExpectedException expected = ExpectedException.none();

  @Test
  public final void testResourceWrongType(
    final @Mocked Bundle bundle,
    final @Mocked CoStringTableParserProviderType parsers,
    final @Mocked CoResourceResolverType resolver)
    throws Exception
  {
    final CoResourceID resource_id =
      CoResourceID.of("a.b.c", "x");
    final CoResource resource =
      CoResource.of(
        resource_id, "com.io7m.callisto.text", URI.create("urn:1"));

    new Expectations()
    {{
      resolver.resolve(bundle, resource_id);
      this.result = CoResourceLookupResult.of(bundle, resource);
    }};

    final CoStringTableProviderType provider =
      this.provider(parsers, resolver);

    this.expected.expect(CoStringTableExceptionUnparseable.class);
    this.expected.expectMessage(StringContains.containsString(
      "Resource is not of the correct type"));
    provider.get(bundle, resource_id, "eng");
  }

  @Test
  public final void testResourceFailsToParse(
    final @Mocked Bundle bundle,
    final @Mocked CoStringTableType table,
    final @Mocked CoStringTableParserType parser,
    final @Mocked CoStringTableParserProviderType parsers,
    final @Mocked CoResourceResolverType resolver)
    throws Exception
  {
    final CoResourceID resource_id =
      CoResourceID.of("a.b.c", "x");
    final CoResource resource =
      CoResource.of(
        resource_id,
        "com.io7m.callisto.stringtable",
        CoStringTableProviderContract.class.getResource("invalid-0.cstx").toURI());

    new Expectations()
    {{
      final List<CoStringTableParserError> errors = new ArrayList<>();
      errors.add(CoStringTableParserError.of(
        resource.uri(), 0, "Failure", Optional.empty()));

      parser.parse();
      this.result =
        CoStringTableParserResult.of(table, errors, new ArrayList<>());

      parsers.createParserFromStream(
        (InputStream) this.any,
        resource.uri(),
        "eng");

      this.result = parser;

      resolver.resolve(bundle, resource_id);
      this.result = CoResourceLookupResult.of(bundle, resource);
    }};

    final CoStringTableProviderType provider =
      this.provider(parsers, resolver);

    this.expected.expect(CoStringTableExceptionUnparseable.class);
    this.expected.expectMessage(StringContains.containsString("Failure"));
    provider.get(bundle, resource_id, "eng");
  }

  @Test
  public final void testResourceOK(
    final @Mocked Bundle bundle,
    final @Mocked CoStringTableType table,
    final @Mocked CoStringTableParserType parser,
    final @Mocked CoStringTableParserProviderType parsers,
    final @Mocked CoResourceResolverType resolver)
    throws Exception
  {
    final CoResourceID resource_id =
      CoResourceID.of("a.b.c", "x");
    final CoResource resource =
      CoResource.of(
        resource_id,
        "com.io7m.callisto.stringtable",
        CoStringTableProviderContract.class.getResource("simple.cstx").toURI());

    new Expectations()
    {{
      parser.parse();
      this.result =
        CoStringTableParserResult.of(
          table, new ArrayList<>(), new ArrayList<>());

      parsers.createParserFromStream(
        (InputStream) this.any,
        resource.uri(),
        "eng");

      this.result = parser;

      resolver.resolve(bundle, resource_id);
      this.result = CoResourceLookupResult.of(bundle, resource);
    }};

    final CoStringTableProviderType provider =
      this.provider(parsers, resolver);

    final CoStringTableType tt =
      provider.get(bundle, resource_id, "eng");
    Assert.assertSame(table, tt);
  }
}
