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

import com.io7m.callisto.resources.api.CoResourceBundleIdentifier;
import com.io7m.callisto.resources.api.CoResourceBundleParserProviderType;
import com.io7m.callisto.resources.api.CoResourceException;
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourceLookupResult;
import com.io7m.callisto.resources.api.CoResourceModelType;
import com.io7m.callisto.resources.api.CoResourceResolverType;
import com.io7m.jnull.NullCheck;
import com.io7m.jnull.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.SortedMap;

/**
 * The default implementation of the {@link CoResourceResolverType} interface.
 */

@Component(service = CoResourceResolverType.class)
public final class CoResourceResolver implements CoResourceResolverType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoResourceResolver.class);
  }

  private BundleTracker<TrackedBundle> tracker;
  private CoResourceBundleParserProviderType parsers;
  private CoResourceModel model;

  /**
   * Construct a resource resolver.
   */

  public CoResourceResolver()
  {

  }

  /**
   * Activate the component.
   *
   * @param context The bundle context
   */

  @Activate
  public void onActivate(
    final BundleContext context)
  {
    NullCheck.notNull(context, "Context");

    this.model = new CoResourceModel(this.parsers);
    this.tracker = new BundleTracker<>(
      context, ~Bundle.UNINSTALLED, new Tracker(this.model));
    this.tracker.open();
  }

  /**
   * Deactivate the component.
   */

  @Deactivate
  public void onDeactivate()
  {
    this.tracker.close();
  }

  /**
   * Introduce a resource package parser provider.
   *
   * @param in_parsers The parser provider
   */

  @Reference(
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY)
  public void onResourcePackageParserProviderSet(
    final CoResourceBundleParserProviderType in_parsers)
  {
    this.parsers = NullCheck.notNull(in_parsers, "Parsers");
  }

  @Override
  public CoResourceLookupResult resolve(
    final Bundle requester,
    final CoResourceID resource_id)
    throws CoResourceException
  {
    NullCheck.notNull(requester, "Requester");
    NullCheck.notNull(resource_id, "ID");
    return this.model.bundleResourceLookup(requester, resource_id);
  }

  @Override
  public SortedMap<CoResourceBundleIdentifier, CoResourceModelType.BundleRegisteredType> bundlesRegistered()
  {
    return this.model.bundlesRegistered();
  }

  private static final class TrackedBundle
  {
    private @Nullable CoResourceModelType.BundleRegisteredType registered;

    TrackedBundle(
      final CoResourceModelType.BundleRegisteredType in_registered)
    {
      this.registered = in_registered;
    }

    @Override
    public String toString()
    {
      final CoResourceModelType.BundleRegisteredType r = this.registered;
      if (r != null) {
        return r.id().name() + " " + r.id().version();
      }
      return "not registered";
    }
  }

  private static final class Tracker
    implements BundleTrackerCustomizer<TrackedBundle>
  {
    private final CoResourceModelType model;

    Tracker(
      final CoResourceModelType in_model)
    {
      this.model = NullCheck.notNull(in_model, "in_model");
    }

    @Override
    public TrackedBundle addingBundle(
      final Bundle bundle,
      final BundleEvent event)
    {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
          "addingBundle: {} ({}) {}",
          bundle,
          CoBundleStateStrings.stateName(bundle.getState()),
          event);
      }

      TrackedBundle result;
      try {
        final Optional<CoResourceModelType.BundleRegisteredType> r_opt =
          this.model.bundleRegister(bundle);

        if (r_opt.isPresent()) {
          result = new TrackedBundle(r_opt.get());
        } else {
          if (LOG.isDebugEnabled()) {
            LOG.debug("addingBundle: ignored {}", bundle);
          }
          result = new TrackedBundle(null);
        }
      } catch (final CoResourceException e) {
        LOG.error("addingBundle: error adding bundle: ", e);
        result = new TrackedBundle(null);
      }

      return result;
    }

    @Override
    public void modifiedBundle(
      final Bundle bundle,
      final BundleEvent event,
      final TrackedBundle tracked_bundle)
    {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
          "modifiedBundle: {} ({}) {} ({})",
          bundle,
          CoBundleStateStrings.stateName(bundle.getState()),
          event,
          tracked_bundle);
      }
    }

    @Override
    public void removedBundle(
      final Bundle bundle,
      final BundleEvent event,
      final TrackedBundle tracked_bundle)
    {
      if (LOG.isDebugEnabled()) {
        LOG.debug(
          "removedBundle: {} ({}) {} ({})",
          bundle,
          CoBundleStateStrings.stateName(bundle.getState()),
          event,
          tracked_bundle);
      }

      final CoResourceModelType.BundleRegisteredType r =
        tracked_bundle.registered;
      if (r != null) {
        this.model.bundleUnregister(r);
      }
    }
  }
}
