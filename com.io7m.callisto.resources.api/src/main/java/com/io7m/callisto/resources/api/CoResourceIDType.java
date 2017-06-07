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

/**
 * The type of resource identifiers.
 */

@CoImmutableStyleType
@Value.Immutable
public interface CoResourceIDType extends Comparable<CoResourceIDType>
{
  /**
   * @return The package that exports the resource
   */

  @Value.Parameter
  String packageName();

  /**
   * @return The name of the exported resource
   */

  @Value.Parameter
  String name();

  @Override
  default int compareTo(
    final CoResourceIDType other)
  {
    NullCheck.notNull(other, "Other");
    final int cmp_r = this.packageName().compareTo(other.packageName());
    if (cmp_r != 0) {
      return cmp_r;
    }
    return this.name().compareTo(other.name());
  }

  /**
   * @return The fully qualified name of the resource
   */

  @Value.Auxiliary
  @Value.Derived
  default String qualifiedName()
  {
    return this.packageName() + "." + this.name();
  }
}
