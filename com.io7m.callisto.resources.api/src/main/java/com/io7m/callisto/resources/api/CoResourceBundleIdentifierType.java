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

import com.io7m.callisto.core.CoImmutableStyleType;
import com.io7m.jnull.NullCheck;
import org.immutables.value.Value;
import org.osgi.framework.Version;

/**
 * The type of resource bundle identifiers.
 */

@CoImmutableStyleType
@Value.Immutable
public interface CoResourceBundleIdentifierType
  extends Comparable<CoResourceBundleIdentifierType>
{
  /**
   * @return The bundle name
   */

  @Value.Parameter
  String name();

  /**
   * @return The bundle version
   */

  @Value.Parameter
  Version version();

  @Override
  default int compareTo(
    final CoResourceBundleIdentifierType other)
  {
    NullCheck.notNull(other, "Other");

    final int cmp_r = this.name().compareTo(other.name());
    if (cmp_r == 0) {
      return this.version().compareTo(other.version());
    }
    return cmp_r;
  }
}
