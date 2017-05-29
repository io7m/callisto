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
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourcePackageParserContinue;
import com.io7m.callisto.resources.api.CoResourcePackageParserProviderType;
import com.io7m.callisto.resources.api.CoResourcePackageParserReceiverType;
import com.io7m.callisto.resources.api.CoResourcePackageParserType;
import com.io7m.jnull.NullCheck;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.io7m.callisto.resources.api.CoResourcePackageParserContinue.CONTINUE;

/**
 * The default implementation of the {@link CoResourcePackageParserProviderType}
 * interface.
 */

@Component(
  immediate = true,
  service = CoResourcePackageParserProviderType.class)
public final class CoResourcePackageParserProvider
  implements CoResourcePackageParserProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoResourcePackageParserProvider.class);
  }

  /**
   * Construct a parser provider.
   */

  public CoResourcePackageParserProvider()
  {

  }

  @Override
  public CoResourcePackageParserType createFromBufferedReader(
    final BufferedReader reader,
    final URI uri,
    final CoResourcePackageParserReceiverType receiver)
  {
    NullCheck.notNull(reader, "Reader");
    NullCheck.notNull(uri, "URI");
    NullCheck.notNull(receiver, "Receiver");

    return new Parser(reader, uri, receiver);
  }

  private static final class Parser implements CoResourcePackageParserType
  {
    private static final Pattern SPACE = Pattern.compile("\\s+");

    private final BufferedReader reader;
    private final URI uri;
    private final CoResourcePackageParserReceiverType receiver;
    private int line_count;
    private boolean closed;
    private String package_name;
    private String package_dir;

    Parser(
      final BufferedReader in_reader,
      final URI in_uri,
      final CoResourcePackageParserReceiverType in_receiver)
    {
      this.reader = NullCheck.notNull(in_reader, "Reader");
      this.uri = NullCheck.notNull(in_uri, "URI");
      this.receiver = NullCheck.notNull(in_receiver, "Receiver");
      this.line_count = 1;
    }

    @Override
    public void close()
      throws IOException
    {
      try {
        this.checkNotClosed();
        this.reader.close();
      } finally {
        this.closed = true;
      }
    }

    private void checkNotClosed()
    {
      if (this.closed) {
        throw new IllegalStateException("Parser already closed");
      }
    }

    @Override
    public void run()
    {
      this.checkNotClosed();
      this.runParser();
      this.finish();
    }

    private void finish()
    {
      if (this.package_name == null) {
        this.sendError("No package name was specified.");
      }
    }

    private void runParser()
    {
      while (true) {
        try {
          final String line_raw = this.reader.readLine();
          if (line_raw == null) {
            return;
          }

          final String line_trimmed = line_raw.trim();
          if (line_trimmed.isEmpty() || line_trimmed.startsWith("#")) {
            continue;
          }

          final String[] line_parts = SPACE.split(line_trimmed);
          final CoResourcePackageParserContinue c = this.processCommand(
            line_parts);
          switch (c) {
            case CONTINUE:
              continue;
            case FINISH:
              return;
          }

        } catch (final Exception e) {
          final CoResourcePackageParserContinue c = this.sendException(e);
          switch (c) {
            case CONTINUE:
              continue;
            case FINISH:
              return;
          }
        } finally {
          this.line_count = Math.addExact(this.line_count, 1);
        }
      }
    }

    private CoResourcePackageParserContinue processCommand(
      final String[] line_parts)
    {
      switch (line_parts[0]) {
        case "package": {
          return this.processCommandPackage(line_parts);
        }
        case "resource": {
          return this.processCommandResource(line_parts);
        }
        default: {
          return this.processCommandUnknown(line_parts);
        }
      }
    }

    private CoResourcePackageParserContinue processCommandUnknown(
      final String[] line_parts)
    {
      return this.sendWarning("Unrecognized command: " + line_parts[0]);
    }

    private CoResourcePackageParserContinue sendWarning(
      final String message)
    {
      try {
        return this.receiver.onWarning(this.uri, this.line_count, message);
      } catch (final Exception ex) {
        return this.receiver.onError(
          this.uri, this.line_count, ex.getMessage(), Optional.of(ex));
      }
    }

    private CoResourcePackageParserContinue processCommandResource(
      final String[] line_parts)
    {
      if (this.package_name == null) {
        return this.sendError("No package name has been specified.");
      }

      if (line_parts.length == 4) {
        final String name = line_parts[1];
        final String type = line_parts[2];
        final String file = line_parts[3];

        try {
          final URI file_uri =
            this.receiver.onResolveFile(this.package_dir, file);
          return this.receiver.onResource(CoResource.of(
            CoResourceID.of(this.package_name, name), type, file_uri));
        } catch (final IOException e) {
          return this.sendException(e);
        }
      }

      final StringBuilder sb = new StringBuilder(256);
      sb.append("Could not parse command.");
      sb.append(System.lineSeparator());
      sb.append("  Expected: resource <name> <type> <file>");
      sb.append(System.lineSeparator());
      sb.append("  Received: ");
      sb.append(Arrays.asList(line_parts).stream()
                  .collect(Collectors.joining(" ")));
      sb.append(System.lineSeparator());
      return this.sendError(sb.toString());
    }

    private CoResourcePackageParserContinue processCommandPackage(
      final String[] line_parts)
    {
      if (line_parts.length == 2) {
        if (this.package_name != null) {
          final StringBuilder sb = new StringBuilder(256);
          sb.append("Package name already provided.");
          sb.append(System.lineSeparator());
          sb.append("  Existing: ");
          sb.append(this.package_name);
          sb.append(System.lineSeparator());
          sb.append("  Received: ");
          sb.append(line_parts[1]);
          sb.append(System.lineSeparator());
          return this.sendError(sb.toString());
        }

        this.package_name = line_parts[1];
        this.package_dir =
          "/" + this.package_name.replace('.', '/');

        try {
          return this.receiver.onPackage(this.package_name);
        } catch (final Exception e) {
          return this.sendException(e);
        }
      }

      final StringBuilder sb = new StringBuilder(256);
      sb.append("Could not parse command.");
      sb.append(System.lineSeparator());
      sb.append("  Expected: package <qualified-name>");
      sb.append(System.lineSeparator());
      sb.append("  Received: ");
      sb.append(Arrays.asList(line_parts).stream()
                  .collect(Collectors.joining(" ")));
      sb.append(System.lineSeparator());
      return this.sendError(sb.toString());
    }

    private CoResourcePackageParserContinue sendError(
      final String message)
    {
      try {
        return this.receiver.onError(
          this.uri, this.line_count, message, Optional.empty());
      } catch (final Exception ex) {
        LOG.error("ignored exception raised by receiver: ", ex);
        return CONTINUE;
      }
    }

    private CoResourcePackageParserContinue sendException(
      final Exception e)
    {
      try {
        return this.receiver.onError(
          this.uri, this.line_count, e.getMessage(), Optional.of(e));
      } catch (final Exception ex) {
        LOG.error("ignored exception raised by receiver: ", ex);
        return CONTINUE;
      }
    }
  }
}
