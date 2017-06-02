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

package com.io7m.callisto.resources.schemas;

import com.io7m.junreachable.UnreachableCodeException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Access to schema files.
 */

public final class CoResourcesSchemas
{
  private static final List<CoResourcesSchema> BUNDLE_SCHEMAS;

  static {
    BUNDLE_SCHEMAS = new ArrayList<>(1);
    BUNDLE_SCHEMAS.add(CoResourcesSchema.of(
      1,
      0,
      URI.create("urn:com.io7m.callisto.resources:1.0"),
      CoResourcesSchemas.class.getResource("bundle-1.0.xsd")));
    BUNDLE_SCHEMAS.sort(CoResourcesSchema::compareTo);
  }

  private CoResourcesSchemas()
  {
    throw new UnreachableCodeException();
  }

  /**
   * @return A list of all of the available bundle schemas
   */

  public static List<CoResourcesSchema> bundleSchemas()
  {
    return Collections.unmodifiableList(BUNDLE_SCHEMAS);
  }
}
