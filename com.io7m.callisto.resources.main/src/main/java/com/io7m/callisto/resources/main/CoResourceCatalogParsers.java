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
import com.io7m.callisto.resources.api.CoResourceCatalogParserProviderType;
import com.io7m.callisto.resources.api.CoResourceCatalogParserReceiverType;
import com.io7m.callisto.resources.api.CoResourceCatalogParserType;
import com.io7m.callisto.resources.api.CoResourceParserContinue;
import com.io7m.jnull.NullCheck;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Component
public final class CoResourceCatalogParsers
  implements CoResourceCatalogParserProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoResourceCatalogParsers.class);
  }

  public CoResourceCatalogParsers()
  {

  }

  @Override
  public CoResourceCatalogParserType createFromBufferedReader(
    final URI uri,
    final BufferedReader reader,
    final CoResourceCatalogParserReceiverType receiver)
  {
    NullCheck.notNull(uri, "URI");
    NullCheck.notNull(reader, "Reader");
    NullCheck.notNull(receiver, "Receiver");
    return new Parser(uri, reader, receiver);
  }

  private static final class Parser implements CoResourceCatalogParserType
  {
    private final BufferedReader reader;
    private final URI url;
    private final CoResourceCatalogParserReceiverType receiver;
    private int line_number;

    private Parser(
      final URI in_url,
      final BufferedReader in_reader,
      final CoResourceCatalogParserReceiverType in_receiver)
    {
      this.url = NullCheck.notNull(in_url, "URL");
      this.reader = NullCheck.notNull(in_reader, "Reader");
      this.receiver = NullCheck.notNull(in_receiver, "Receiver");
      this.line_number = 1;
    }

    private CoResourceParserContinue sendErrorMessage(
      final String message)
    {
      try {
        return this.receiver.onError(
          this.url, this.line_number, message, Optional.empty());
      } catch (final Exception ex) {
        LOG.error("ignored exception raised by receiver: ", ex);
        return CoResourceParserContinue.CONTINUE;
      }
    }

    private CoResourceParserContinue sendWarningMessage(
      final String message)
    {
      try {
        return this.receiver.onWarning(this.url, this.line_number, message);
      } catch (final Exception ex) {
        return this.sendException(ex);
      }
    }

    private CoResourceParserContinue sendException(
      final Exception e)
    {
      try {
        return this.receiver.onError(
          this.url, this.line_number, e.getMessage(), Optional.of(e));
      } catch (final Exception ex) {
        LOG.error("ignored exception raised by receiver: ", ex);
        return CoResourceParserContinue.CONTINUE;
      }
    }

    private CoResourceParserContinue parseLine(
      final String text,
      final String[] line_parts)
    {
      try {
        final String type = line_parts[0];
        switch (type) {
          case "R": {
            if (line_parts.length != 2) {
              final StringBuilder sb = new StringBuilder(128);
              sb.append("Incorrect R type syntax.");
              sb.append(System.lineSeparator());
              sb.append("  Expected: R <file>");
              sb.append(System.lineSeparator());
              sb.append("  Received: ");
              sb.append(text);
              sb.append(System.lineSeparator());
              return this.sendErrorMessage(sb.toString());
            }

            final String file = line_parts[1];
            final URI uri = this.receiver.onResourceResolve(file);
            return this.receiver.onResource(CoResource.of(uri));
          }

          default: {
            return this.sendWarningMessage(
              "Unrecognized catalog entry type: " + type);
          }
        }
      } catch (final Exception e) {
        return this.sendException(e);
      }
    }

    @Override
    public void run()
    {
      NullCheck.notNull(this.receiver, "Receiver");

      while (true) {
        try {
          final String line = this.reader.readLine();
          if (line == null) {
            break;
          }

          try {
            final String line_trimmed = line.trim();
            if (line_trimmed.isEmpty()) {
              continue;
            }

            if (line_trimmed.startsWith("#")) {
              continue;
            }

            final String[] line_parts = line_trimmed.split("\\s+");
            if (line_parts.length == 0) {
              continue;
            }

            final CoResourceParserContinue c =
              this.parseLine(line_trimmed, line_parts);
            switch (c) {
              case CONTINUE:
                continue;
              case FINISH:
                return;
            }

          } finally {
            this.line_number = Math.addExact(this.line_number, 1);
          }
        } catch (final Exception e) {
          final CoResourceParserContinue c = this.sendException(e);
          switch (c) {
            case CONTINUE:
              continue;
            case FINISH:
              return;
          }
        }
      }
    }

    @Override
    public void close()
      throws IOException
    {
      this.reader.close();
    }
  }
}
