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

package com.io7m.callisto.resources.api;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

/**
 * A receiver of parser events.
 */

public interface CoResourcePackageParserReceiverType
{
  /**
   * A warning was encountered when parsing the package file.
   *
   * @param uri     The URI of the package file
   * @param line    The current line number
   * @param message The warning message
   *
   * @return A value that indicates whether or not parsing should continue
   */

  CoResourcePackageParserContinue onWarning(
    URI uri,
    int line,
    String message);

  /**
   * An error was encountered when parsing the package file.
   *
   * @param uri       The URI of the package file
   * @param line      The current line number
   * @param message   The warning message
   * @param exception The exception, if any
   *
   * @return A value that indicates whether or not parsing should continue
   */

  CoResourcePackageParserContinue onError(
    URI uri,
    int line,
    String message,
    Optional<Exception> exception);

  /**
   * A file must be resolved.
   *
   * @param directory The directory containing the file
   * @param file      The file name
   *
   * @return The URI of the file
   *
   * @throws java.io.FileNotFoundException If the file does not exist
   * @throws IOException                   On other I/O errors
   */

  URI onResolveFile(
    String directory,
    String file)
    throws IOException;

  /**
   * A package name was received.
   *
   * @param name The package name
   *
   * @return A value that indicates whether or not parsing should continue
   */

  CoResourcePackageParserContinue onPackage(
    String name);

  /**
   * A resource was exported.
   *
   * @param resource The resource
   *
   * @return A value that indicates whether or not parsing should continue
   */

  CoResourcePackageParserContinue onResource(
    CoResource resource);
}
