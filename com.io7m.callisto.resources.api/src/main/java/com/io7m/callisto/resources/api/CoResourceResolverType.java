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

import org.osgi.framework.Bundle;

import java.util.SortedMap;

/**
 * The type of resource resolvers.
 */

public interface CoResourceResolverType
{
  /**
   * Resolve the given resource.
   *
   * @param caller The requesting bundle
   * @param id     The resource ID
   *
   * @return The resolved resource
   *
   * @throws CoResourceExceptionNonexistent If the resource does not exist, or
   *                                        if no bundle exports the package
   *                                        containing the resource to the
   *                                        requesting bundle
   * @throws CoResourceException            On errors
   */

  CoResourceLookupResult resolve(
    Bundle caller,
    CoResourceID id)
    throws CoResourceException;

  /**
   * @return A read-only snapshot of the registered bundles
   */

  SortedMap<CoResourceBundleIdentifier, CoResourceModelType.BundleRegisteredType> bundlesRegistered();
}
