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
import com.io7m.callisto.resources.api.CoResourceCatalogParserProviderType;
import com.io7m.callisto.resources.api.CoResourceCatalogParserReceiverType;
import com.io7m.callisto.resources.api.CoResourceCatalogParserType;
import com.io7m.callisto.resources.api.CoResourceParserContinue;
import com.io7m.callisto.resources.api.CoResourceType;
import com.io7m.jnull.NullCheck;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.io7m.callisto.resources.api.CoResourceParserContinue.CONTINUE;
import static com.io7m.callisto.resources.api.CoResourcePublicationType.TOPIC_ERRORS;
import static com.io7m.callisto.resources.api.CoResourcePublicationType.TOPIC_WARNINGS;
import static com.io7m.callisto.resources.api.CoResourceType.RESOURCES_MANIFEST_HEADER;

@Component
public final class CoResourcePublisher
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoResourcePublisher.class);
  }

  private BundleTracker<Tracker.TrackedBundle> tracker;
  private CoResourceCatalogParserProviderType parsers;
  private AtomicReference<EventAdmin> events;

  public CoResourcePublisher()
  {
    this.events = new AtomicReference<>();
  }

  private static final class Tracker
    implements BundleTrackerCustomizer<Tracker.TrackedBundle>
  {
    private final BundleContext context;
    private final CoResourceCatalogParserProviderType parsers;
    private final AtomicReference<EventAdmin> events;

    Tracker(
      final BundleContext in_context,
      final CoResourceCatalogParserProviderType in_parsers,
      final AtomicReference<EventAdmin> in_events)
    {
      this.context = NullCheck.notNull(in_context, "Context");
      this.parsers = NullCheck.notNull(in_parsers, "Parsers");
      this.events = NullCheck.notNull(in_events, "Events");
    }

    private static final class Receiver
      implements CoResourceCatalogParserReceiverType
    {
      private final Bundle bundle;
      private final BundleContext context;
      private final AtomicReference<EventAdmin> events;

      Receiver(
        final BundleContext in_context,
        final Bundle in_bundle,
        final AtomicReference<EventAdmin> in_events)
      {
        this.context = NullCheck.notNull(in_context, "Context");
        this.bundle = NullCheck.notNull(in_bundle, "Bundle");
        this.events = NullCheck.notNull(in_events, "Events");
      }

      @Override
      public URI onResourceResolve(
        final String file)
        throws FileNotFoundException
      {
        final URL url = this.bundle.getResource(file);
        if (url == null) {
          throw new FileNotFoundException("File not found: " + file);
        }

        try {
          return url.toURI();
        } catch (final URISyntaxException e) {
          throw new IllegalArgumentException(e);
        }
      }

      @Override
      public CoResourceParserContinue onResource(
        final CoResource resource)
      {
        this.context.registerService(
          CoResourceType.class, resource, new Hashtable<>());
        return CONTINUE;
      }

      @Override
      public CoResourceParserContinue onWarning(
        final URI uri,
        final int line,
        final String message)
      {
        LOG.warn("{}:{}: {}", uri, Integer.valueOf(line), message);

        final EventAdmin event_admin = this.events.get();
        if (event_admin != null) {
          final HashMap<String, Object> props = new HashMap<>(3);
          props.put("uri", uri.toString());
          props.put("line", Integer.valueOf(line));
          props.put("message", message);
          event_admin.postEvent(new Event(TOPIC_WARNINGS, props));
        }

        return CONTINUE;
      }

      @Override
      public CoResourceParserContinue onError(
        final URI uri,
        final int line,
        final String message,
        final Optional<Exception> exception_opt)
      {
        final Integer bline = Integer.valueOf(line);
        if (exception_opt.isPresent()) {
          final Exception exception = exception_opt.get();
          LOG.error("{}:{}: {}: ", uri, bline, message, exception);
        } else {
          LOG.error("{}:{}: {}", uri, bline, message);
        }

        final EventAdmin event_admin = this.events.get();
        if (event_admin != null) {
          final HashMap<String, Object> props = new HashMap<>(3);
          props.put("uri", uri.toString());
          props.put("line", Integer.valueOf(line));
          props.put("message", message);
          event_admin.postEvent(new Event(TOPIC_ERRORS, props));
        }

        return CONTINUE;
      }
    }

    @Override
    public TrackedBundle addingBundle(
      final Bundle bundle,
      final BundleEvent event)
    {
      LOG.debug("addingBundle: {}", bundle.getSymbolicName());

      final Dictionary<String, String> headers = bundle.getHeaders();
      final String file = headers.get(RESOURCES_MANIFEST_HEADER);

      if (file != null) {
        try {
          final URL url = bundle.getResource(file);
          if (url == null) {
            throw new FileNotFoundException("File not found: " + file);
          }

          final URI uri = url.toURI();
          final Receiver receiver =
            new Receiver(this.context, bundle, this.events);
          try (final CoResourceCatalogParserType parser =
                 this.parsers.createFromURI(uri, receiver)) {
            parser.run();
            return new TrackedBundle();
          }
        } catch (final Exception e) {

          final EventAdmin event_admin = this.events.get();
          if (event_admin != null) {
            final HashMap<String, Object> props = new HashMap<>(3);
            props.put("uri", "urn:missing");
            props.put("line", Integer.valueOf(0));
            props.put("message", e.getMessage());
            event_admin.postEvent(new Event(TOPIC_ERRORS, props));
          }

          LOG.error("error processing bundle: ", e);
          return null;
        }
      }

      return null;
    }

    @Override
    public void modifiedBundle(
      final Bundle bundle,
      final BundleEvent event,
      final TrackedBundle object)
    {

    }

    @Override
    public void removedBundle(
      final Bundle bundle,
      final BundleEvent event,
      final TrackedBundle object)
    {

    }

    static final class TrackedBundle
    {
      TrackedBundle()
      {

      }
    }
  }

  @Reference(
    cardinality = ReferenceCardinality.MANDATORY,
    policy = ReferencePolicy.STATIC)
  public void onCatalogParsersSet(
    final CoResourceCatalogParserProviderType in_parsers)
  {
    this.parsers = NullCheck.notNull(in_parsers, "Parsers");
  }

  @Reference(
    cardinality = ReferenceCardinality.OPTIONAL,
    policy = ReferencePolicy.DYNAMIC,
    unbind = "onEventAdminUnset")
  public void onEventAdminSet(
    final EventAdmin in_events)
  {
    this.events.compareAndSet(
      null, NullCheck.notNull(in_events, "Events"));
  }

  public void onEventAdminUnset(
    final EventAdmin in_events)
  {
    this.events.compareAndSet(
      NullCheck.notNull(in_events, "Events"), null);
  }

  @Activate
  public void onActivate(
    final BundleContext context)
  {
    this.tracker =
      new BundleTracker<>(
        context,
        0xffff_ffff,
        new Tracker(context, this.parsers, this.events));
    this.tracker.open();
  }

  @Deactivate
  public void onDeactivate()
  {
    this.tracker.close();
  }
}
