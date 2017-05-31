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

import net.jcip.annotations.ThreadSafe;
import org.osgi.framework.Bundle;

import java.util.Map;
import java.util.Optional;

/**
 * <p>The set of available resource packages.</p>
 *
 * <p>A bundle may participate in resource handling if one or more of the
 * following conditions hold:</p>
 *
 * <ul>
 *
 * <li>The bundle declares some resources via the Callisto-Resource-Bundle
 * manifest header.</li>
 *
 * <li>The bundle exports some packages via the Provide-Capability manifest
 * header using the Callisto resource package {@link CoResourcePackageNamespace#NAMESPACE}.
 * This implies that the bundle has a Callisto-Resource-Bundle header.</li>
 *
 * <li>The bundle imports some packages via the Require-Capability manifest
 * header using the Callisto resource package {@link CoResourcePackageNamespace#NAMESPACE}.</li>
 *
 * </ul>
 *
 * <p>Implementations are required to be safe for access from multiple
 * threads.</p>
 */

@ThreadSafe
public interface CoResourceModelType
{
  /**
   * The type of registered bundles.
   */

  @ThreadSafe
  interface BundleRegisteredType
  {
    /**
     * @return The bundle identifier
     */

    CoResourceBundleIdentifier id();

    /**
     * @return A read-only view of the exported packages
     */

    Map<String, PackageExportedType> packagesExported();

    /**
     * @return A read-only view of the private packages
     */

    Map<String, PackagePrivateType> packagesPrivate();
  }

  /**
   * The type of packages exported from bundles.
   */

  @ThreadSafe
  interface PackageExportedType
  {
    /**
     * @return The name of the package
     */

    String name();

    /**
     * @return A read-only view of the exported resources
     */

    Map<CoResourceID, CoResource> exportedResources();
  }

  /**
   * The type of private packages inside bundles.
   */

  @ThreadSafe
  interface PackagePrivateType
  {
    /**
     * @return The name of the package
     */

    String name();

    /**
     * @return A read-only view of the exported resources
     */

    Map<CoResourceID, CoResource> privateResources();
  }

  /**
   * Unregister a bundle. If the bundle is not registered, the call has no
   * effect.
   *
   * @param registered The registered bundle
   *
   * @throws CoResourceException On errors
   */

  void bundleUnregister(
    BundleRegisteredType registered)
    throws CoResourceException;

  /**
   * Attempt to register a bundle.
   *
   * @param exporter The exporting bundle
   *
   * @return A registered bundle, or nothing if the bundle cannot participate in
   * resource resolution.
   *
   * @throws CoResourceExceptionBundleDuplicate If the bundle has already been
   *                                            registered with this model
   * @throws CoResourceExceptionBundleMalformed If the bundle is malformed in
   *                                            some manner (such as a missing
   *                                            package declaration file)
   * @throws CoResourceExceptionIO              On I/O errors
   * @throws CoResourceException                On errors
   */

  Optional<BundleRegisteredType> bundleRegister(
    Bundle exporter)
    throws CoResourceException;

  /**
   * Look up a resource.
   *
   * @param requester   The requesting bundle
   * @param resource_id The ID of the resource
   *
   * @return The resource lookup result
   *
   * @throws CoResourceExceptionNonexistent If the resource does not exist, or
   *                                        is not exported to the requesting
   *                                        bundle
   * @throws CoResourceException            On errors
   */

  CoResourceLookupResult bundleResourceLookup(
    Bundle requester,
    CoResourceID resource_id)
    throws CoResourceException;

  /**
   * @param bundle The bundle
   *
   * @return {@code true} if the given bundle is registered
   */

  boolean bundleIsRegistered(
    Bundle bundle);

  /**
   * @param bundle The bundle
   *
   * @return {@code true} if the given bundle is registerable
   */

  boolean bundleIsRegisterable(
    Bundle bundle);
}
