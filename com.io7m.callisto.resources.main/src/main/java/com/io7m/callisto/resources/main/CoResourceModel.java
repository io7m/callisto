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
import com.io7m.callisto.resources.api.CoResourceException;
import com.io7m.callisto.resources.api.CoResourceExceptionBundleDuplicate;
import com.io7m.callisto.resources.api.CoResourceExceptionBundleMalformed;
import com.io7m.callisto.resources.api.CoResourceExceptionIO;
import com.io7m.callisto.resources.api.CoResourceExceptionPackageError;
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourceModelType;
import com.io7m.callisto.resources.api.CoResourcePackageNamespace;
import com.io7m.callisto.resources.api.CoResourcePackageParserContinue;
import com.io7m.callisto.resources.api.CoResourcePackageParserError;
import com.io7m.callisto.resources.api.CoResourcePackageParserProviderType;
import com.io7m.callisto.resources.api.CoResourcePackageParserReceiverType;
import com.io7m.callisto.resources.api.CoResourcePackageParserType;
import com.io7m.jnull.NullCheck;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.io7m.callisto.resources.api.CoResourceExceptions.resourceDoesNotExist;
import static com.io7m.callisto.resources.api.CoResourceExceptions.resourcePackageDoesNotExist;
import static com.io7m.callisto.resources.api.CoResourcePackageParserContinue.CONTINUE;

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
  private final CoResourcePackageParserProviderType parsers;

  /**
   * Construct an empty resource model.
   *
   * @param in_parsers A provider of parsers
   */

  public CoResourceModel(
    final CoResourcePackageParserProviderType in_parsers)
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
      b.append("  Received: Nothing");
    }
    b.append(System.lineSeparator());
    throw new CoResourceExceptionBundleMalformed(b.toString());
  }

  private static final class ParserReceiver
    implements CoResourcePackageParserReceiverType
  {
    private final Bundle bundle;
    private final Object2ReferenceOpenHashMap<CoResourceID, CoResource> resources;
    private final ObjectArrayList<CoResourcePackageParserError> errors;

    ParserReceiver(
      final Bundle in_bundle)
    {
      this.bundle = NullCheck.notNull(in_bundle, "Bundle");
      this.resources = new Object2ReferenceOpenHashMap<>();
      this.errors = new ObjectArrayList<>();
    }

    @Override
    public CoResourcePackageParserContinue onWarning(
      final URI uri,
      final int line,
      final String message)
    {
      LOG.warn("{}:{}: {}", uri, Integer.valueOf(line), message);
      return CONTINUE;
    }

    @Override
    public CoResourcePackageParserContinue onError(
      final URI uri,
      final int line,
      final String message,
      final Optional<Exception> exception_opt)
    {
      if (exception_opt.isPresent()) {
        final Exception ex = exception_opt.get();
        LOG.error("{}:{}: {}: ", uri, Integer.valueOf(line), message, ex);
      } else {
        LOG.error("{}:{}: {}", uri, Integer.valueOf(line), message);
      }

      this.errors.add(CoResourcePackageParserError.of(
        uri, line, message, exception_opt));
      return CONTINUE;
    }

    @Override
    public URI onResolveFile(
      final String directory,
      final String file)
      throws IOException
    {
      final String absolute_file = directory + "/" + file;
      final URL url = this.bundle.getResource(absolute_file);
      if (url != null) {
        try {
          return url.toURI();
        } catch (final URISyntaxException e) {
          throw new IOException(e);
        }
      }
      throw new FileNotFoundException(absolute_file);
    }

    @Override
    public CoResourcePackageParserContinue onPackage(
      final String name)
    {
      return CONTINUE;
    }

    @Override
    public CoResourcePackageParserContinue onResource(
      final CoResource resource)
    {
      if (this.resources.containsKey(resource.id())) {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("Duplicate resource ID.");
        sb.append(System.lineSeparator());
        sb.append("  Resource: ");
        sb.append(resource.id().qualifiedName());
        sb.append(System.lineSeparator());
        throw new CoResourceExceptionBundleMalformed(sb.toString());
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug(
          "bundle {} {} package {} exports {}",
          this.bundle.getSymbolicName(),
          this.bundle.getVersion(),
          resource.id().packageName(),
          resource.id().qualifiedName());
      }

      this.resources.put(resource.id(), resource);
      return CONTINUE;
    }
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

    final CoResourceBundleIdentifier bundle_id =
      CoResourceBundleIdentifier.of(
        exporter.getSymbolicName(),
        exporter.getVersion());

    final Object2ReferenceOpenHashMap<String, CPackage> packages =
      new Object2ReferenceOpenHashMap<>();
    final CBundle c_bundle =
      new CBundle(bundle_id, packages);

    final BundleWiring wiring = exporter.adapt(BundleWiring.class);
    if (wiring == null) {
      return Optional.empty();
    }

    final List<BundleCapability> exports =
      wiring.getCapabilities(CoResourcePackageNamespace.NAMESPACE);
    if (exports.isEmpty()) {
      return Optional.empty();
    }

    for (int index = 0; index < exports.size(); ++index) {
      final BundleCapability export = exports.get(index);
      final CPackage c_package = this.processPackage(exporter, export);
      packages.put(c_package.name, c_package);
    }

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

  @Override
  public CoResource bundleResourceLookup(
    final Bundle requester,
    final CoResourceID resource_id)
    throws CoResourceException
  {
    NullCheck.notNull(requester, "Requester");
    NullCheck.notNull(resource_id, "Resource ID");

    final BundleWiring wiring = requester.adapt(BundleWiring.class);
    if (wiring == null) {
      throw resourcePackageDoesNotExist(requester, resource_id);
    }

    final List<BundleWire> wires =
      wiring.getRequiredWires(CoResourcePackageNamespace.NAMESPACE);

    for (int index = 0; index < wires.size(); ++index) {
      final BundleWire wire = wires.get(index);

      final BundleCapability provides =
        wire.getCapability();
      final String target_package =
        (String) provides.getAttributes().get(
          CoResourcePackageNamespace.NAME_ATTRIBUTE_NAME);

      if (Objects.equals(resource_id.packageName(), target_package)) {
        final Bundle target_bundle =
          provides.getResource().getBundle();
        final CoResourceBundleIdentifier target_bundle_id =
          CoResourceBundleIdentifier.of(
            target_bundle.getSymbolicName(),
            target_bundle.getVersion());

        return this.lookupResourceInBundle(
          requester,
          resource_id,
          target_package,
          target_bundle_id);
      }
    }

    throw resourcePackageDoesNotExist(requester, resource_id);
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

  private CoResource lookupResourceInBundle(
    final Bundle requester,
    final CoResourceID resource_id,
    final String package_target,
    final CoResourceBundleIdentifier bundle_target)
  {
    synchronized (this.bundles) {
      if (this.bundles.containsKey(bundle_target)) {
        final CBundle cb_target =
          this.bundles.get(bundle_target);

        if (cb_target.packages.containsKey(package_target)) {
          final CPackage cb_package =
            cb_target.packages.get(package_target);

          if (cb_package.resources.containsKey(resource_id)) {
            return cb_package.resources.get(resource_id);
          }

          throw resourceDoesNotExist(requester, cb_target.id, resource_id);
        }
      }

      throw resourcePackageDoesNotExist(requester, resource_id);
    }
  }

  private CPackage processPackage(
    final Bundle bundle,
    final BundleCapability export)
  {
    final Map<String, Object> attributes = export.getAttributes();

    final Object raw_name =
      attributes.get(CoResourcePackageNamespace.NAME_ATTRIBUTE_NAME);
    final Object raw_version =
      attributes.get(CoResourcePackageNamespace.VERSION_ATTRIBUTE_NAME);

    final String package_name =
      checkAttribute(
        bundle,
        raw_name,
        CoResourcePackageNamespace.NAME_ATTRIBUTE_TYPE,
        CoResourcePackageNamespace.NAMESPACE,
        CoResourcePackageNamespace.NAME_ATTRIBUTE_NAME);

    final Version package_version =
      checkAttribute(
        bundle,
        raw_version,
        CoResourcePackageNamespace.VERSION_ATTRIBUTE_TYPE,
        CoResourcePackageNamespace.NAMESPACE,
        CoResourcePackageNamespace.VERSION_ATTRIBUTE_NAME);

    if (LOG.isDebugEnabled()) {
      LOG.debug(
        "{} {} exports {} {}",
        bundle.getSymbolicName(),
        bundle.getVersion(),
        package_name,
        package_version);
    }

    final StringBuilder file_b = new StringBuilder(128);
    file_b.append("/");
    file_b.append(package_name.replace('.', '/'));
    file_b.append("/package-info.cpd");

    final String declaration_file = file_b.toString();
    final URL declaration_url = bundle.getResource(declaration_file);
    if (declaration_url == null) {
      final StringBuilder sb = new StringBuilder(256);
      sb.append("Package declaration file is missing.");
      sb.append(System.lineSeparator());
      sb.append("  Bundle: ");
      sb.append(bundle.getSymbolicName());
      sb.append(" ");
      sb.append(bundle.getVersion());
      sb.append(System.lineSeparator());
      sb.append("  File: ");
      sb.append(declaration_file);
      sb.append(System.lineSeparator());
      throw new CoResourceExceptionBundleMalformed(sb.toString());
    }

    try (final InputStream stream = declaration_url.openStream()) {
      final ParserReceiver receiver = new ParserReceiver(bundle);
      final URI uri = declaration_url.toURI();
      try (final CoResourcePackageParserType parser =
             this.parsers.createFromInputStream(stream, uri, receiver)) {
        parser.run();
        if (!receiver.errors.isEmpty()) {
          throw new CoResourceExceptionPackageError(
            receiver.errors,
            "Errors were encountered during parsing.");
        }
      }

      return new CPackage(package_name, receiver.resources);
    } catch (final IOException e) {
      throw new CoResourceExceptionIO(e);
    } catch (final URISyntaxException e) {
      throw new CoResourceExceptionBundleMalformed(e);
    }
  }

  private static final class CBundle implements BundleRegisteredType
  {
    private final Object2ReferenceOpenHashMap<String, CPackage> packages;
    private final Map<String, ExportedPackageType> packages_view;
    private final CoResourceBundleIdentifier id;

    CBundle(
      final CoResourceBundleIdentifier in_id,
      final Object2ReferenceOpenHashMap<String, CPackage> in_packages)
    {
      this.id = NullCheck.notNull(in_id, "ID");
      this.packages = NullCheck.notNull(in_packages, "Packages");
      this.packages_view = Collections.unmodifiableMap(this.packages);
    }

    @Override
    public CoResourceBundleIdentifier id()
    {
      return this.id;
    }

    @Override
    public Map<String, ExportedPackageType> exportedPackages()
    {
      return this.packages_view;
    }
  }

  private static final class CPackage implements ExportedPackageType
  {
    private final Object2ReferenceOpenHashMap<CoResourceID, CoResource> resources;
    private final Map<CoResourceID, CoResource> resources_view;
    private final String name;

    CPackage(
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
}
