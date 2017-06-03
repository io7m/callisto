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

package com.io7m.callisto.stringtables.api;

/**
 * The type of string tables.
 */

public interface CoStringTableType
{
  /**
   * The type of resources that can be loaded as string tables.
   */

  String RESOURCE_TYPE = "com.io7m.callisto.stringtable";

  /**
   * @param name The string name
   *
   * @return The localized string value for the given constant
   *
   * @throws CoStringTableExceptionNonexistent Iff the given constant does not
   *                                           appear in the string table
   */

  default String text(
    final String name)
    throws CoStringTableExceptionNonexistent
  {
    return this.string(name).value();
  }

  /**
   * @param name The string name
   *
   * @return The localized string value for the given constant
   *
   * @throws CoStringTableExceptionNonexistent Iff the given constant does not
   *                                           appear in the string table
   */

  CoString string(
    String name)
    throws CoStringTableExceptionNonexistent;

  /**
   * @return The size in octets of the contents of the string table
   */

  long size();
}
