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

package com.io7m.callisto.schemas;

import com.io7m.junreachable.UnreachableCodeException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Access to schema files.
 */

public final class CoSchemas
{
  private static final List<CoSchema> BUNDLE_SCHEMAS;
  private static final List<CoSchema> STRINGTABLE_SCHEMAS;

  static {
    BUNDLE_SCHEMAS = new ArrayList<>(1);
    BUNDLE_SCHEMAS.add(CoSchema.of(
      1,
      0,
      URI.create("urn:com.io7m.callisto.resources:1.0"),
      CoSchemas.class.getResource("bundle-1.0.xsd")));
    BUNDLE_SCHEMAS.sort(CoSchema::compareTo);

    STRINGTABLE_SCHEMAS = new ArrayList<>(1);
    STRINGTABLE_SCHEMAS.add(CoSchema.of(
      1,
      0,
      URI.create("urn:com.io7m.callisto.stringtables:1.0"),
      CoSchemas.class.getResource("stringtable-1.0.xsd")));
    STRINGTABLE_SCHEMAS.sort(CoSchema::compareTo);
  }

  private CoSchemas()
  {
    throw new UnreachableCodeException();
  }

  /**
   * @return A list of all of the available bundle schemas
   */

  public static List<CoSchema> bundleSchemas()
  {
    return Collections.unmodifiableList(BUNDLE_SCHEMAS);
  }

  /**
   * @return A list of all of the available string table schemas
   */

  public static List<CoSchema> stringTableSchemas()
  {
    return Collections.unmodifiableList(STRINGTABLE_SCHEMAS);
  }
}
