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

package com.io7m.callisto.stringtables.main;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.io7m.callisto.resources.api.CoResource;
import com.io7m.callisto.resources.api.CoResourceException;
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
import com.io7m.jnull.NullCheck;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.callisto.stringtables.api.CoStringTableType.RESOURCE_TYPE;

/**
 * The default implementation of the {@link CoStringTableProviderType}
 * interface.
 */

@Component(
  configurationPolicy = ConfigurationPolicy.OPTIONAL,
  configurationPid = "com.io7m.callisto.stringtables.main.provider",
  service = CoStringTableProviderType.class)
public final class CoStringTableProvider implements CoStringTableProviderType
{
  /**
   * Use a default 8mb cache size.
   */

  private static final long DEFAULT_CACHE_SIZE = 8L * 1_000_000L;

  private volatile CoResourceResolverType resources;
  private volatile CoStringTableParserProviderType parsers;
  private volatile LoadingCache<CoStringTableRequest, CoStringTableType> cache;

  /**
   * Construct a provider.
   */

  public CoStringTableProvider()
  {

  }

  private static int weigh(
    final CoStringTableRequest request,
    final CoStringTableType table)
  {
    return Math.toIntExact(table.size());
  }

  /**
   * Introduce a resource resolver.
   *
   * @param in_resources The resolver
   */

  @Reference(
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY)
  public void onResourceResolverSet(
    final CoResourceResolverType in_resources)
  {
    this.resources = NullCheck.notNull(in_resources, "Resources");
  }

  /**
   * Introduce a parser provider.
   *
   * @param in_parsers The parser provider
   */

  @Reference(
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY)
  public void onStringTableParserProviderSet(
    final CoStringTableParserProviderType in_parsers)
  {
    this.parsers = NullCheck.notNull(in_parsers, "Parsers");
  }

  /**
   * Activate the component.
   *
   * @throws Exception On errors
   */

  @Activate
  public void onActivate()
    throws Exception
  {
    this.cache = this.newCache(DEFAULT_CACHE_SIZE);
  }

  private LoadingCache<CoStringTableRequest, CoStringTableType> newCache(
    final long size)
  {
    final LoadingCache<CoStringTableRequest, CoStringTableType> c = this.cache;
    if (c != null) {
      c.cleanUp();
    }

    return Caffeine.newBuilder()
      .maximumWeight(size)
      .weigher(CoStringTableProvider::weigh)
      .build(this::load);
  }

  /**
   * Deactivate the component.
   *
   * @throws Exception On errors
   */

  @Deactivate
  public void onDeactivate()
    throws Exception
  {
    this.cache.cleanUp();
  }

  /**
   * Change the configuration of the component.
   *
   * @param configuration The new component configuration
   *
   * @throws Exception On errors
   */

  @Modified
  public void onModified(
    final Map<String, Object> configuration)
    throws Exception
  {
    try {
      if (configuration.containsKey("cache_size")) {
        final long size =
          Long.parseUnsignedLong((String) configuration.get("cache_size"));
        this.cache = this.newCache(size);
      }
    } catch (final NumberFormatException e) {
      throw new ConfigurationException("cache_size", e.getMessage(), e);
    }
  }

  private CoStringTableType load(
    final CoStringTableRequest request)
    throws IOException
  {
    final CoResourceLookupResult result =
      this.resources.resolve(request.requester(), request.resourceID());

    final CoResource resource = result.resource();
    if (!Objects.equals(resource.type(), RESOURCE_TYPE)) {
      final StringBuilder sb =
        new StringBuilder(128)
          .append("Resource is not of the correct type.")
          .append(System.lineSeparator())
          .append("  Resource: ")
          .append(resource.id().qualifiedName())
          .append(System.lineSeparator())
          .append("  Expected type: ")
          .append(RESOURCE_TYPE)
          .append(System.lineSeparator())
          .append("  Received type: ")
          .append(resource.type())
          .append(System.lineSeparator());
      throw new CoStringTableExceptionUnparseable(sb.toString());
    }

    final URI uri = resource.uri();
    try (final InputStream stream = resource.open()) {
      final String language = request.language();
      try (final CoStringTableParserType parser =
             this.parsers.createParserFromStream(stream, uri, language)) {
        final CoStringTableParserResult parse_result = parser.parse();
        if (parse_result.errors().isEmpty()) {
          return parse_result.table();
        }

        final StringBuilder sb = new StringBuilder(256);
        sb.append("Errors were encountered when parsing the string table.");
        sb.append(System.lineSeparator());

        for (final CoStringTableParserError e : parse_result.errors()) {
          sb.append(e.uri());
          sb.append(":");
          sb.append(e.line());
          sb.append(": ");
          final Optional<Exception> e_opt = e.exception();
          if (e_opt.isPresent()) {
            final Exception ex = e_opt.get();
            sb.append(ex.getClass().getName());
            sb.append(": ");
            sb.append(ex.getMessage());
          } else {
            sb.append(e.message());
          }
          sb.append(System.lineSeparator());
        }

        throw new CoStringTableExceptionUnparseable(sb.toString());
      }
    }
  }

  @Override
  public CoStringTableType get(
    final Bundle requester,
    final CoResourceID resource_id,
    final String language)
    throws CoResourceException
  {
    NullCheck.notNull(requester, "Requester");
    NullCheck.notNull(resource_id, "Resource ID");
    NullCheck.notNull(language, "Language");

    return this.cache.get(
      CoStringTableRequest.of(requester, resource_id, language));
  }
}
