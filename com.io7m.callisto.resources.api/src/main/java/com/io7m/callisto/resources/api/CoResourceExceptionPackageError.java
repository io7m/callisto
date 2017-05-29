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

import com.io7m.jnull.NullCheck;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * An exception raised by a failure to parse a package file.
 */

public final class CoResourceExceptionPackageError
  extends CoResourceException
{
  private final List<CoResourcePackageParserError> errors;

  /**
   * Construct an exception.
   *
   * @param in_errors The list of parse errors
   * @param message   The message
   */

  public CoResourceExceptionPackageError(
    final List<CoResourcePackageParserError> in_errors,
    final String message)
  {
    super(makeMessage(in_errors, message));
    this.errors = Collections.unmodifiableList(
      NullCheck.notNull(in_errors, "Errors"));
  }

  private static String makeMessage(
    final List<CoResourcePackageParserError> in_errors,
    final String message)
  {
    final StringBuilder sb = new StringBuilder(128);
    sb.append(message);
    sb.append(System.lineSeparator());
    for (final CoResourcePackageParserError e : in_errors) {
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
    }
    return sb.toString();
  }

  /**
   * @return A read-only view of the list of errors
   */

  public List<CoResourcePackageParserError> errors()
  {
    return this.errors;
  }
}
