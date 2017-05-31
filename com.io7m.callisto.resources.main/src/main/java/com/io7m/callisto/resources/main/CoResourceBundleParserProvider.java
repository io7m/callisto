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

package com.io7m.callisto.resources.main;

import com.io7m.callisto.resources.api.CoResource;
import com.io7m.callisto.resources.api.CoResourceBundleParserError;
import com.io7m.callisto.resources.api.CoResourceBundleParserFileResolverType;
import com.io7m.callisto.resources.api.CoResourceBundleParserProviderType;
import com.io7m.callisto.resources.api.CoResourceBundleParserResult;
import com.io7m.callisto.resources.api.CoResourceBundleParserType;
import com.io7m.callisto.resources.api.CoResourceBundleParserWarning;
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourcePackageDeclaration;
import com.io7m.jnull.NullCheck;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The default implementation of the {@link CoResourceBundleParserProviderType}
 * interface.
 */

@Component(
  immediate = true,
  service = CoResourceBundleParserProviderType.class)
public final class CoResourceBundleParserProvider
  implements CoResourceBundleParserProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoResourceBundleParserProvider.class);
  }

  /**
   * Construct a parser provider.
   */

  public CoResourceBundleParserProvider()
  {

  }

  @Override
  public CoResourceBundleParserType createFromInputStream(
    final InputStream stream,
    final URI uri,
    final CoResourceBundleParserFileResolverType resolver)
  {
    NullCheck.notNull(stream, "Stream");
    NullCheck.notNull(uri, "URI");
    NullCheck.notNull(resolver, "Resolver");
    return new Parser(stream, uri, resolver);
  }

  private static final class Parser implements CoResourceBundleParserType
  {
    private static final Pattern SPACE = Pattern.compile("\\s+");

    private final InputStream stream;
    private final URI uri;
    private final CoResourceBundleParserFileResolverType resolver;
    private final CoResourceBundleParserResult.Builder result;
    private boolean done;
    private int line;
    private Object2ReferenceOpenHashMap<CoResourceID, CoResource> resources;
    private boolean closed;
    private CoResourcePackageDeclaration.Builder package_builder;
    private String package_name;
    private Object2IntOpenHashMap<String> packages;

    Parser(
      final InputStream in_stream,
      final URI in_uri,
      final CoResourceBundleParserFileResolverType in_resolver)
    {
      this.stream = NullCheck.notNull(in_stream, "Stream");
      this.uri = NullCheck.notNull(in_uri, "URI");
      this.resolver = NullCheck.notNull(in_resolver, "Resolver");
      this.resources = new Object2ReferenceOpenHashMap<>();
      this.packages = new Object2IntOpenHashMap<>();
      this.result = CoResourceBundleParserResult.builder();
      this.line = 1;
      this.done = false;
    }

    @Override
    public CoResourceBundleParserResult parse()
    {
      try (final BufferedReader reader =
             new BufferedReader(new InputStreamReader(this.stream, UTF_8))) {

        while (!this.done) {
          try {
            final String line_raw = reader.readLine();
            if (line_raw == null) {
              break;
            }

            final String line_trimmed = line_raw.trim();
            final String[] line_parts = SPACE.split(line_trimmed);
            if (this.line == 1) {
              this.parseCommandCBD(line_trimmed, line_parts);
              continue;
            }

            if (line_trimmed.isEmpty() || line_trimmed.startsWith("#")) {
              continue;
            }

            this.parseCommand(line_trimmed, line_parts);
          } finally {
            this.line = Math.addExact(this.line, 1);
          }
        }

        this.finishCurrentPackage();
      } catch (final IOException e) {
        this.result.addErrors(CoResourceBundleParserError.of(
          this.uri, this.line, e.getMessage(), Optional.of(e)));
      }

      return this.result.build();
    }

    private void parseCommand(
      final String text,
      final String[] parts)
    {
      switch (parts[0]) {
        case "package": {
          this.parseCommandPackage(text, parts);
          return;
        }
        case "resource": {
          this.parseCommandResource(text, parts);
          return;
        }
        default: {
          this.parseCommandUnknown(text, parts);
        }
      }
    }

    private void parseCommandCBD(
      final String text,
      final String[] parts)
    {
      if (parts.length == 3) {
        if (Objects.equals(parts[0], "cbd")) {
          try {
            final int major = Integer.parseUnsignedInt(parts[1]);
            final int minor = Integer.parseUnsignedInt(parts[2]);
            if (LOG.isDebugEnabled()) {
              LOG.debug(
                "cbd {} {}",
                Integer.valueOf(major),
                Integer.valueOf(minor));
            }

            if (major != 1) {
              final StringBuilder sb = new StringBuilder(128);
              sb.append("Format version not supported.");
              sb.append(System.lineSeparator());
              sb.append("  Expected: cbd 1 *");
              sb.append(System.lineSeparator());
              sb.append("  Received: ");
              sb.append(text);
              sb.append(System.lineSeparator());
              this.result.addErrors(CoResourceBundleParserError.of(
                this.uri, this.line, sb.toString(), Optional.empty()));
              this.done = true;
            }

            return;
          } catch (final NumberFormatException e) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("Error parsing cbd version.");
            sb.append(System.lineSeparator());
            sb.append("  Expected: cbd <integer> <integer>");
            sb.append(System.lineSeparator());
            sb.append("  Received: ");
            sb.append(text);
            sb.append(System.lineSeparator());
            this.result.addErrors(CoResourceBundleParserError.of(
              this.uri, this.line, sb.toString(), Optional.of(e)));
            this.done = true;
            return;
          }
        }
      }

      final StringBuilder sb = new StringBuilder(128);
      sb.append("The first command of the file must be the cbd command.");
      sb.append(System.lineSeparator());
      sb.append("  Expected: cbd <integer> <integer>");
      sb.append(System.lineSeparator());
      sb.append("  Received: ");
      sb.append(text);
      sb.append(System.lineSeparator());
      this.result.addErrors(CoResourceBundleParserError.of(
        this.uri, this.line, sb.toString(), Optional.empty()));
      this.done = true;
    }

    private void parseCommandUnknown(
      final String text,
      final String[] parts)
    {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Received an unrecognized command.");
      sb.append(System.lineSeparator());
      sb.append("  Received: ");
      sb.append(text);
      sb.append(System.lineSeparator());
      this.result.addWarnings(CoResourceBundleParserWarning.of(
        this.uri, this.line, sb.toString()));
    }

    private void parseCommandResource(
      final String text,
      final String[] parts)
    {
      if (this.package_builder == null) {
        this.result.addErrors(CoResourceBundleParserError.of(
          this.uri,
          this.line,
          "No package name has been specified.",
          Optional.empty()));
        return;
      }

      if (parts.length != 4) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Could not parse command.");
        sb.append(System.lineSeparator());
        sb.append("  Expected: resource <name> <type> <file>");
        sb.append(System.lineSeparator());
        sb.append("  Received: ");
        sb.append(text);
        sb.append(System.lineSeparator());
        this.result.addErrors(CoResourceBundleParserError.of(
          this.uri, this.line, sb.toString(), Optional.empty()));
        return;
      }

      final String name = parts[1];
      final String type = parts[2];
      final String file = parts[3];

      final URI file_uri;
      try {
        file_uri = this.resolver.resolve(file);
      } catch (final FileNotFoundException e) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("An exported file is missing.");
        sb.append(System.lineSeparator());
        sb.append("  File: ");
        sb.append(file);
        sb.append(System.lineSeparator());
        this.result.addErrors(CoResourceBundleParserError.of(
          this.uri, this.line, sb.toString(), Optional.of(e)));
        return;
      }

      final CoResourceID resource_id =
        CoResourceID.of(this.package_name, name);
      final CoResource resource =
        CoResource.of(resource_id, type, file_uri);

      if (this.resources.containsKey(resource_id)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("A resource is already exported.");
        sb.append(System.lineSeparator());
        sb.append("  Resource: ");
        sb.append(resource_id.qualifiedName());
        sb.append(System.lineSeparator());
        this.result.addErrors(CoResourceBundleParserError.of(
          this.uri, this.line, sb.toString(), Optional.empty()));
        return;
      }

      this.resources.put(resource_id, resource);
    }

    private void parseCommandPackage(
      final String text,
      final String[] parts)
    {
      if (parts.length == 2) {
        this.finishCurrentPackage();
        this.package_builder = CoResourcePackageDeclaration.builder();
        this.package_name = parts[1];
        this.package_builder.setName(this.package_name);
        if (this.packages.containsKey(this.package_name)) {
          final StringBuilder sb = new StringBuilder(256);
          sb.append("Duplicate package declaration.");
          sb.append(System.lineSeparator());
          sb.append("  Received: ");
          sb.append(this.package_name);
          sb.append(System.lineSeparator());
          sb.append("  Original declaration: ");
          sb.append(this.package_name);
          sb.append(" at line ");
          sb.append(this.packages.getInt(this.package_name));
          sb.append(System.lineSeparator());
          this.result.addErrors(CoResourceBundleParserError.of(
            this.uri, this.line, sb.toString(), Optional.empty()));
        }
        this.packages.put(this.package_name, this.line);
        return;
      }

      final StringBuilder sb = new StringBuilder(256);
      sb.append("Could not parse command.");
      sb.append(System.lineSeparator());
      sb.append("  Expected: package <qualified-name>");
      sb.append(System.lineSeparator());
      sb.append("  Received: ");
      sb.append(text);
      sb.append(System.lineSeparator());
      this.result.addErrors(CoResourceBundleParserError.of(
        this.uri, this.line, sb.toString(), Optional.empty()));
    }

    private void finishCurrentPackage()
    {
      if (this.package_builder != null) {
        this.package_builder.putAllResources(this.resources);
        this.result.putPackages(
          this.package_name,
          this.package_builder.build());
        this.resources = new Object2ReferenceOpenHashMap<>();
        this.package_builder = null;
        this.package_name = null;
      }
    }

    @Override
    public void close()
      throws IOException
    {
      try {
        if (!this.closed) {
          this.stream.close();
        }
      } finally {
        this.closed = true;
      }
    }
  }
}
