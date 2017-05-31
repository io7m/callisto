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

package com.io7m.callisto.resources.main;

import com.io7m.callisto.resources.api.CoResource;
import com.io7m.callisto.resources.api.CoResourceBundleIdentifier;
import com.io7m.callisto.resources.api.CoResourceBundleParserFileResolverType;
import com.io7m.callisto.resources.api.CoResourceBundleParserProviderType;
import com.io7m.callisto.resources.api.CoResourceBundleParserResult;
import com.io7m.callisto.resources.api.CoResourceBundleParserType;
import com.io7m.callisto.resources.api.CoResourceException;
import com.io7m.callisto.resources.api.CoResourceExceptionBundleDuplicate;
import com.io7m.callisto.resources.api.CoResourceExceptionBundleMalformed;
import com.io7m.callisto.resources.api.CoResourceExceptionBundleParsingError;
import com.io7m.callisto.resources.api.CoResourceExceptionIO;
import com.io7m.callisto.resources.api.CoResourceExceptionNonexistent;
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourceLookupResult;
import com.io7m.callisto.resources.api.CoResourceModelType;
import com.io7m.callisto.resources.api.CoResourcePackageDeclaration;
import com.io7m.callisto.resources.api.CoResourcePackageNamespace;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import com.io7m.junreachable.UnreachableCodeException;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The default implementation of the {@link CoResourceModelType} interface.
 */

public final class CoResourceModel implements CoResourceModelType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoResourceResolver.class);
  }

  private final Object2ReferenceOpenHashMap<CoResourceBundleIdentifier, CBundle> bundles;
  private final CoResourceBundleParserProviderType parsers;

  /**
   * Construct an empty resource model.
   *
   * @param in_parsers A provider of parsers
   */

  public CoResourceModel(
    final CoResourceBundleParserProviderType in_parsers)
  {
    this.parsers = NullCheck.notNull(in_parsers, "Parsers");
    this.bundles = new Object2ReferenceOpenHashMap<>();
  }

  @SuppressWarnings("unchecked")
  private static <T> T checkAttribute(
    final Bundle bundle,
    final Object value,
    final Class<T> type,
    final String namespace,
    final String name)
  {
    final StringBuilder b = new StringBuilder(256);
    if (value != null) {
      if (Objects.equals(value.getClass(), type)) {
        return (T) value;
      }
      b.append("Error encountered when parsing provided capabilities.");
      b.append(System.lineSeparator());
      b.append("  Problem:       Declared attribute is of the wrong type.");
    } else {
      b.append("Error encountered when parsing provided capabilities.");
      b.append(System.lineSeparator());
      b.append("  Problem:       Required attribute is missing.");
    }

    b.append(System.lineSeparator());
    b.append("  Bundle:        ");
    b.append(bundle.getSymbolicName());
    b.append(" ");
    b.append(bundle.getVersion());
    b.append(System.lineSeparator());
    b.append("  Namespace:     ");
    b.append(namespace);
    b.append(System.lineSeparator());
    b.append("  Attribute:     ");
    b.append(name);
    b.append(System.lineSeparator());
    b.append("  Expected type: ");
    b.append(type.getName());
    b.append(System.lineSeparator());

    if (value != null) {
      b.append("  Received type: ");
      b.append(value.getClass().getName());
    } else {
      b.append("  Received:      Nothing");
    }
    b.append(System.lineSeparator());
    throw new CoResourceExceptionBundleMalformed(b.toString());
  }

  private static boolean hasHeader(
    final Bundle bundle)
  {
    final Dictionary<String, String> headers = bundle.getHeaders();
    return headers.get("Callisto-Resource-Bundle") != null;
  }

  private static boolean hasRequirements(
    final Bundle bundle)
  {
    final BundleWiring wiring = bundle.adapt(BundleWiring.class);
    if (wiring != null) {
      final List<BundleWire> requirements =
        wiring.getRequiredWires(CoResourcePackageNamespace.NAMESPACE);
      return !requirements.isEmpty();
    }
    return false;
  }

  private static boolean hasCapabilities(
    final Bundle bundle)
  {
    final BundleWiring wiring = bundle.adapt(BundleWiring.class);
    if (wiring != null) {
      final List<BundleCapability> capabilities =
        wiring.getCapabilities(CoResourcePackageNamespace.NAMESPACE);
      return !capabilities.isEmpty();
    }
    return false;
  }

  @Override
  public void bundleUnregister(
    final BundleRegisteredType bundle)
    throws CoResourceException
  {
    NullCheck.notNull(bundle, "Bundle");

    if (LOG.isDebugEnabled()) {
      LOG.debug(
        "bundleUnregister: {} {}",
        bundle.id().name(),
        bundle.id().version());
    }

    if (bundle instanceof CBundle) {
      synchronized (this.bundles) {
        this.bundles.remove(bundle.id());
      }
    }
  }

  @Override
  public Optional<BundleRegisteredType> bundleRegister(
    final Bundle exporter)
    throws CoResourceException
  {
    NullCheck.notNull(exporter, "Exporter");

    if (LOG.isDebugEnabled()) {
      LOG.debug(
        "bundleRegister: {} {}",
        exporter.getSymbolicName(),
        exporter.getVersion());
    }

    /*
     * Check for anything interesting in the exporting bundle.
     */

    if (!this.bundleIsRegisterable(exporter)) {
      return Optional.empty();
    }

    /*
     * Check for the presence of a Callisto-Resource-Bundle header identifying
     * the bundle's declaration file and parse it. If there isn't one, assume
     * that the bundle does not declare any resources.
     */

    final Map<String, CoResourcePackageDeclaration> packages_available;
    final Dictionary<String, String> headers = exporter.getHeaders();
    final String file = headers.get("Callisto-Resource-Bundle");
    if (file != null) {
      packages_available = this.parseResourceBundleDeclaration(exporter, file);
    } else {
      packages_available = new Object2ReferenceOpenHashMap<>();
    }

    final CoResourceBundleIdentifier bundle_id =
      CoResourceBundleIdentifier.of(
        exporter.getSymbolicName(),
        exporter.getVersion());

    /*
     * Assume that all packages are private by default.
     */

    final Object2ReferenceOpenHashMap<String, CPackageExported> packages_exported =
      new Object2ReferenceOpenHashMap<>();
    final Object2ReferenceOpenHashMap<String, CPackagePrivate> packages_private =
      new Object2ReferenceOpenHashMap<>();

    final CBundle c_bundle =
      new CBundle(exporter, bundle_id, packages_exported, packages_private);

    for (final String package_name : packages_available.keySet()) {
      final CoResourcePackageDeclaration package_available =
        packages_available.get(package_name);
      final CPackagePrivate package_private =
        new CPackagePrivate(
          package_name,
          new Object2ReferenceOpenHashMap<>(package_available.resources()));
      packages_private.put(package_name, package_private);
    }

    /*
     * Examine the bundle's wiring in order to determine which of the above
     * packages should be exported.
     */

    final BundleWiring wiring = exporter.adapt(BundleWiring.class);
    if (wiring != null) {
      final List<BundleCapability> exports =
        wiring.getCapabilities(CoResourcePackageNamespace.NAMESPACE);

      for (int index = 0; index < exports.size(); ++index) {
        final BundleCapability export = exports.get(index);
        final Map<String, Object> attributes = export.getAttributes();

        final Object raw_name =
          attributes.get(CoResourcePackageNamespace.NAME_ATTRIBUTE_NAME);
        final Object raw_version =
          attributes.get(CoResourcePackageNamespace.VERSION_ATTRIBUTE_NAME);

        final String package_name =
          checkAttribute(
            exporter,
            raw_name,
            CoResourcePackageNamespace.NAME_ATTRIBUTE_TYPE,
            CoResourcePackageNamespace.NAMESPACE,
            CoResourcePackageNamespace.NAME_ATTRIBUTE_NAME);

        final Version package_version =
          checkAttribute(
            exporter,
            raw_version,
            CoResourcePackageNamespace.VERSION_ATTRIBUTE_TYPE,
            CoResourcePackageNamespace.NAMESPACE,
            CoResourcePackageNamespace.VERSION_ATTRIBUTE_NAME);

        if (LOG.isDebugEnabled()) {
          LOG.debug(
            "{} {} exports {} {}",
            exporter.getSymbolicName(),
            exporter.getVersion(),
            package_name,
            package_version);
        }

        if (!packages_available.containsKey(package_name)) {
          final StringBuilder sb = new StringBuilder(128);
          sb.append(
            "The bundle's Provide-Capability header exports a nonexistent package.");
          sb.append(System.lineSeparator());
          sb.append("  Bundle: ");
          sb.append(exporter);
          sb.append(System.lineSeparator());
          sb.append("  Package: ");
          sb.append(package_name);
          sb.append(System.lineSeparator());
          throw new CoResourceExceptionBundleMalformed(sb.toString());
        }

        final CoResourcePackageDeclaration pack =
          packages_available.get(package_name);

        final CPackageExported exported =
          new CPackageExported(
            package_name,
            new Object2ReferenceOpenHashMap<>(pack.resources()));

        packages_private.remove(package_name);
        packages_exported.put(package_name, exported);
      }
    }

    /*
     * Register the bundle.
     */

    synchronized (this.bundles) {
      if (this.bundles.containsKey(bundle_id)) {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("The given bundle has already been registered.");
        sb.append(System.lineSeparator());
        sb.append("  Bundle: ");
        sb.append(bundle_id.name());
        sb.append(" ");
        sb.append(bundle_id.version());
        sb.append(System.lineSeparator());
        throw new CoResourceExceptionBundleDuplicate(sb.toString());
      }
      this.bundles.put(bundle_id, c_bundle);
    }

    return Optional.of(c_bundle);
  }

  private Map<String, CoResourcePackageDeclaration>
  parseResourceBundleDeclaration(
    final Bundle exporter,
    final String file)
  {
    final URL url = exporter.getResource(file);
    if (url == null) {
      final StringBuilder sb = new StringBuilder(128);
      sb.append("Resource bundle declaration file is missing.");
      sb.append(System.lineSeparator());
      sb.append("  Bundle: ");
      sb.append(exporter);
      sb.append(System.lineSeparator());
      sb.append("  File: ");
      sb.append(file);
      sb.append(System.lineSeparator());
      throw new CoResourceExceptionBundleMalformed(sb.toString());
    }

    try (final InputStream stream = url.openStream()) {
      final CoResourceBundleParserFileResolverType resolver = path -> {
        final URL path_url = exporter.getResource(path);
        if (path_url == null) {
          throw new FileNotFoundException(path);
        }
        try {
          return path_url.toURI();
        } catch (final URISyntaxException e) {
          throw new FileNotFoundException(e.getMessage());
        }
      };

      try (final CoResourceBundleParserType parser =
             this.parsers.createFromInputStream(
               stream, url.toURI(), resolver)) {
        final CoResourceBundleParserResult result = parser.parse();
        if (!result.errors().isEmpty()) {
          throw new CoResourceExceptionBundleParsingError(
            result.errors(),
            "Errors were encountered during parsing.");
        }
        return result.packages();
      } catch (final URISyntaxException e) {
        throw new UnreachableCodeException(e);
      }
    } catch (final IOException e) {
      throw new CoResourceExceptionIO(e);
    }
  }

  @Override
  public CoResourceLookupResult bundleResourceLookup(
    final Bundle requester,
    final CoResourceID resource_id)
    throws CoResourceException
  {
    NullCheck.notNull(requester, "Requester");
    NullCheck.notNull(resource_id, "Resource ID");

    final LookupResult result = this.lookupResource(requester, resource_id);
    switch (result.status) {
      case LOOKUP_OK: {
        return CoResourceLookupResult.of(result.owner, result.resource);
      }
      case LOOKUP_NO_BUNDLE_EXPORTS_PACKAGE: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append(
          "No bundle exports the given package to the requesting bundle.");
        sb.append(System.lineSeparator());
        sb.append("  Requesting bundle: ");
        sb.append(requester);
        sb.append(System.lineSeparator());
        sb.append("  Resource: ");
        sb.append(resource_id.qualifiedName());
        sb.append(System.lineSeparator());
        throw new CoResourceExceptionNonexistent(sb.toString());
      }
      case LOOKUP_RESOURCE_MISSING: {
        final StringBuilder sb = new StringBuilder(128);
        sb.append(
          "The requested resource is not present in the exporting bundle.");
        sb.append(System.lineSeparator());
        sb.append("  Requesting bundle: ");
        sb.append(requester);
        sb.append(System.lineSeparator());
        sb.append("  Exporting bundle: ");
        sb.append(result.owner);
        sb.append(System.lineSeparator());
        sb.append("  Resource: ");
        sb.append(resource_id.qualifiedName());
        sb.append(System.lineSeparator());
        throw new CoResourceExceptionNonexistent(sb.toString());
      }
    }

    throw new UnreachableCodeException();
  }

  @Override
  public boolean bundleIsRegistered(
    final Bundle bundle)
  {
    NullCheck.notNull(bundle, "Bundle");

    synchronized (this.bundles) {
      return this.bundles.containsKey(
        CoResourceBundleIdentifier.of(
          bundle.getSymbolicName(),
          bundle.getVersion()));
    }
  }

  @Override
  public boolean bundleIsRegisterable(
    final Bundle b)
  {
    NullCheck.notNull(b, "Bundle");
    return hasHeader(b) || hasRequirements(b) || hasCapabilities(b);
  }

  private LookupResult lookupResource(
    final Bundle bundle_requester,
    final CoResourceID resource_id)
  {
    /*
     * First, attempt to look up the resource in the bundle itself.
     */

    final CoResourceBundleIdentifier bundle_requester_id =
      CoResourceBundleIdentifier.of(
        bundle_requester.getSymbolicName(),
        bundle_requester.getVersion());

    synchronized (this.bundles) {
      if (this.bundles.containsKey(bundle_requester_id)) {
        final CBundle cb_target = this.bundles.get(bundle_requester_id);
        final CoResource resource =
          cb_target.lookupResource(true, resource_id);
        if (resource != null) {
          return new LookupResult(
            LookupStatus.LOOKUP_OK,
            resource,
            bundle_requester,
            cb_target.bundle);
        }
      }
    }

    /*
     * If the bundle didn't contain the resource, check the requester's
     * wiring and examine the first bundle that exports the containing package
     * to the requester. The OSGi resolver should guarantee that at most one
     * bundle will be wired the the requester for any given package.
     */

    final BundleWiring wiring = bundle_requester.adapt(BundleWiring.class);
    if (wiring == null) {
      return new LookupResult(
        LookupStatus.LOOKUP_NO_BUNDLE_EXPORTS_PACKAGE,
        null,
        bundle_requester,
        null);
    }

    final List<BundleWire> wires =
      wiring.getRequiredWires(CoResourcePackageNamespace.NAMESPACE);

    for (int index = 0; index < wires.size(); ++index) {
      final BundleWire wire = wires.get(index);

      final BundleCapability target_capability =
        wire.getCapability();
      final Bundle target_bundle =
        target_capability.getRevision().getBundle();

      final Map<String, Object> target_attributes =
        target_capability.getAttributes();
      final Object target_object =
        target_attributes.get(CoResourcePackageNamespace.NAME_ATTRIBUTE_NAME);

      final String target_package =
        checkAttribute(
          target_bundle,
          target_object,
          String.class,
          CoResourcePackageNamespace.NAMESPACE,
          CoResourcePackageNamespace.NAME_ATTRIBUTE_NAME);

      if (!Objects.equals(resource_id.packageName(), target_package)) {
        continue;
      }

      /*
       * A wired bundle exported the target package.
       */

      final CoResourceBundleIdentifier bundle_target_id =
        CoResourceBundleIdentifier.of(
          target_bundle.getSymbolicName(),
          target_bundle.getVersion());

      synchronized (this.bundles) {
        if (!this.bundles.containsKey(bundle_target_id)) {
          return new LookupResult(
            LookupStatus.LOOKUP_NO_BUNDLE_EXPORTS_PACKAGE,
            null,
            bundle_requester,
            null);
        }

        final CBundle cb_target =
          this.bundles.get(bundle_target_id);
        final CoResource resource =
          cb_target.lookupResource(false, resource_id);

        if (resource != null) {
          return new LookupResult(
            LookupStatus.LOOKUP_OK,
            resource,
            bundle_requester,
            cb_target.bundle);
        }

        return new LookupResult(
          LookupStatus.LOOKUP_RESOURCE_MISSING,
          null,
          bundle_requester,
          cb_target.bundle);
      }
    }

    /*
     * None of the examined bundles exported the resource.
     */

    return new LookupResult(
      LookupStatus.LOOKUP_NO_BUNDLE_EXPORTS_PACKAGE,
      null,
      bundle_requester,
      null);
  }

  private enum LookupStatus
  {
    LOOKUP_OK,
    LOOKUP_NO_BUNDLE_EXPORTS_PACKAGE,
    LOOKUP_RESOURCE_MISSING
  }

  private static final class LookupResult
  {
    private final LookupStatus status;
    private final @Nullable CoResource resource;
    private final @Nullable Bundle owner;
    private final Bundle requester;

    LookupResult(
      final LookupStatus in_status,
      final CoResource in_resource,
      final Bundle in_requester,
      final Bundle in_owner)
    {
      this.status = NullCheck.notNull(in_status, "Status");
      this.resource = in_resource;
      this.requester = NullCheck.notNull(in_requester, "Requester");
      this.owner = in_owner;
    }
  }

  private static final class CBundle implements BundleRegisteredType
  {
    private final Object2ReferenceOpenHashMap<String, CPackageExported> packages_exported;
    private final Map<String, PackageExportedType> packages_exported_view;
    private final Object2ReferenceOpenHashMap<String, CPackagePrivate> packages_private;
    private final Map<String, PackagePrivateType> packages_private_view;
    private final CoResourceBundleIdentifier id;
    private final Bundle bundle;

    CBundle(
      final Bundle in_bundle,
      final CoResourceBundleIdentifier in_id,
      final Object2ReferenceOpenHashMap<String, CPackageExported> in_packages_exported,
      final Object2ReferenceOpenHashMap<String, CPackagePrivate> in_packages_private)
    {
      this.bundle =
        NullCheck.notNull(in_bundle, "Bundle");
      this.id =
        NullCheck.notNull(in_id, "ID");
      this.packages_exported =
        NullCheck.notNull(in_packages_exported, "Exported Packages");
      this.packages_exported_view =
        Collections.unmodifiableMap(this.packages_exported);
      this.packages_private =
        NullCheck.notNull(in_packages_private, "Private Packages");
      this.packages_private_view =
        Collections.unmodifiableMap(this.packages_private);
    }

    @Override
    public CoResourceBundleIdentifier id()
    {
      return this.id;
    }

    @Override
    public Map<String, PackageExportedType> packagesExported()
    {
      return this.packages_exported_view;
    }

    @Override
    public Map<String, PackagePrivateType> packagesPrivate()
    {
      return this.packages_private_view;
    }

    @Nullable
    CoResource lookupResource(
      final boolean include_private,
      final CoResourceID resource_id)
    {
      if (include_private) {
        final CPackagePrivate pack =
          this.packages_private.get(resource_id.packageName());
        if (pack != null) {
          final CoResource resource = pack.resources.get(resource_id);
          if (resource != null) {
            return resource;
          }
        }
      }

      final CPackageExported pack =
        this.packages_exported.get(resource_id.packageName());
      if (pack != null) {
        final CoResource resource = pack.resources.get(resource_id);
        if (resource != null) {
          return resource;
        }
      }

      return null;
    }
  }

  private static final class CPackageExported implements PackageExportedType
  {
    private final Object2ReferenceOpenHashMap<CoResourceID, CoResource> resources;
    private final Map<CoResourceID, CoResource> resources_view;
    private final String name;

    CPackageExported(
      final String package_name,
      final Object2ReferenceOpenHashMap<CoResourceID, CoResource> in_resources)
    {
      this.name = NullCheck.notNull(package_name, "Name");
      this.resources = NullCheck.notNull(in_resources, "Resources");
      this.resources_view = Collections.unmodifiableMap(this.resources);
    }

    @Override
    public String name()
    {
      return this.name;
    }

    @Override
    public Map<CoResourceID, CoResource> exportedResources()
    {
      return this.resources_view;
    }
  }

  private static final class CPackagePrivate implements PackagePrivateType
  {
    private final Object2ReferenceOpenHashMap<CoResourceID, CoResource> resources;
    private final Map<CoResourceID, CoResource> resources_view;
    private final String name;

    CPackagePrivate(
      final String package_name,
      final Object2ReferenceOpenHashMap<CoResourceID, CoResource> in_resources)
    {
      this.name = NullCheck.notNull(package_name, "Name");
      this.resources = NullCheck.notNull(in_resources, "Resources");
      this.resources_view = Collections.unmodifiableMap(this.resources);
    }

    @Override
    public String name()
    {
      return this.name;
    }

    @Override
    public Map<CoResourceID, CoResource> privateResources()
    {
      return this.resources_view;
    }
  }
}
