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

import com.io7m.callisto.core.CoImmutableStyleType;
import org.immutables.value.Value;

import java.net.URI;
import java.net.URL;

/**
 * The type of schemas.
 */

@CoImmutableStyleType
@Value.Immutable
public interface CoResourcesSchemaType
  extends Comparable<CoResourcesSchemaType>
{
  /**
   * @return The schema major version
   */

  @Value.Parameter
  int major();

  /**
   * @return The schema minor version
   */

  @Value.Parameter
  int minor();

  /**
   * @return The namespace URI for the schema
   */

  @Value.Parameter
  URI namespaceURI();

  /**
   * @return The schema URL
   */

  @Value.Auxiliary
  @Value.Parameter
  URL fileURL();

  @Override
  default int compareTo(
    final CoResourcesSchemaType other)
  {
    final int cmp_maj = Integer.compare(this.major(), other.major());
    if (cmp_maj == 0) {
      return Integer.compare(this.minor(), other.minor());
    }
    return cmp_maj;
  }
}
