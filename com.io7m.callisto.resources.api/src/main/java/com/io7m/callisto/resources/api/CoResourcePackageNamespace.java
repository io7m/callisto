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

import com.io7m.junreachable.UnreachableCodeException;
import org.osgi.framework.Version;

/**
 * Constants for the Callisto resource package namespace.
 */

public final class CoResourcePackageNamespace
{
  /**
   * The Callisto resource package namespace.
   */

  public static final String NAMESPACE = "com.io7m.callisto.resources.package";

  /**
   * The name of the attribute that contains a package name.
   */

  public static final String NAME_ATTRIBUTE_NAME = "name";

  /**
   * The type of values held in name attributes.
   */

  public static final Class<String> NAME_ATTRIBUTE_TYPE = String.class;

  /**
   * The name of the attribute that contains a package version.
   */

  public static final String VERSION_ATTRIBUTE_NAME = "version";

  /**
   * The type of values held in version attributes.
   */

  public static final Class<Version> VERSION_ATTRIBUTE_TYPE = Version.class;

  private CoResourcePackageNamespace()
  {
    throw new UnreachableCodeException();
  }
}
