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

package com.io7m.callisto.container;

import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyNonexistent;
import com.io7m.junreachable.UnreachableCodeException;
import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Main OSGi container.
 */

public final class CoMain
{
  private CoMain()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Main program.
   *
   * @param args Command line arguments
   *
   * @throws Exception On errors
   */

  public static void main(
    final String[] args)
    throws Exception
  {
    final Logger log = LoggerFactory.getLogger(CoMain.class);
    log.debug("starting");

    log.debug("loading container configuration");

    final Properties base_properties =
      JProperties.fromFile(new File("etc/container.conf"));
    final Properties system_properties =
      System.getProperties();

    final Properties properties = new Properties();
    properties.putAll(base_properties);
    properties.putAll(system_properties);

    final Map config_props = new TreeMap<>(properties);
    config_props.putAll(properties);
    config_props.put(FelixConstants.LOG_LOGGER_PROP, new CoFelixLogger());
    config_props.put("gosh.args", "--nointeractive");
    propertiesSetExports(log, config_props);
    config_props.forEach(
      (key, value) -> log.debug("container property: {} -> {}", key, value));

    log.debug("initializing container");
    final Felix felix = new Felix(config_props);
    felix.init();

    configureLogService(log, felix);
    configureAutoProperties(log, config_props, felix);

    log.debug("starting container");
    felix.start();

    showBundles(log, felix);
    boolean done = false;
    while (!done) {
      final FrameworkEvent event = felix.waitForStop(0L);
      switch (event.getType()) {
        case FrameworkEvent.ERROR: {
          log.error(
            "{}: ",
            event.getBundle().getSymbolicName(),
            event.getThrowable());
          break;
        }
        case FrameworkEvent.INFO: {
          log.info("info event");
          break;
        }
        case FrameworkEvent.PACKAGES_REFRESHED: {
          log.debug("packages refreshed");
          break;
        }
        case FrameworkEvent.STARTED: {
          log.debug("container started");
          break;
        }
        case FrameworkEvent.STARTLEVEL_CHANGED: {
          log.debug("container startlevel changed");
          break;
        }
        case FrameworkEvent.STOPPED: {
          log.debug("container stopped");
          done = true;
          break;
        }
        case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED: {
          log.debug("container stopped (bootclasspath modified)");
          done = true;
          break;
        }
        case FrameworkEvent.STOPPED_UPDATE: {
          log.debug("container stopped (update)");
          done = true;
          break;
        }
        case FrameworkEvent.WAIT_TIMEDOUT: {
          log.debug("wait timed out");
          break;
        }
        case FrameworkEvent.WARNING: {
          log.warn("warning event");
          break;
        }
        default: {
          log.error("unknown event: {}", event);
        }
      }
    }

    log.debug("exiting");
  }

  private static void showBundles(
    final Logger log,
    final Felix felix)
  {
    final BundleContext bundle_context = felix.getBundleContext();
    final Bundle[] bundles = bundle_context.getBundles();
    for (int index = 0; index < bundles.length; ++index) {
      final Bundle bundle = bundles[index];
      log.debug(
        "bundle [{}]: {} {} ({})",
        Long.valueOf(bundle.getBundleId()),
        bundle.getSymbolicName(),
        bundle.getVersion(),
        bundleStateText(bundle));
    }
  }

  private static void configureAutoProperties(
    final Logger log,
    final Map config_props,
    final Felix felix)
  {
    log.debug("processing auto properties");
    CoAutoProcessor.process(config_props, felix.getBundleContext());
  }

  private static void configureLogService(
    final Logger log,
    final Felix felix)
  {
    final BundleContext context = felix.getBundleContext();
    final Bundle system_bundle = context.getBundle();
    log.debug("system bundle: {}", system_bundle.getSymbolicName());

    log.debug("registering log service");
    context.registerService(
      LogService.class,
      new CoLogServiceFactory(),
      new Hashtable<>(0));
  }

  private static void propertiesSetExports(
    final Logger log,
    final Map config_props)
    throws IOException, JPropertyNonexistent
  {
    final String version_slf4j;
    final String version_osgi_log;

    try (final InputStream stream =
           CoMain.class.getResourceAsStream("versions.properties")) {

      final Properties p = new Properties();
      p.load(stream);

      version_slf4j =
        JProperties.getString(p, "com.io7m.callisto.slf4j.version");
      version_osgi_log =
        JProperties.getString(p, "com.io7m.callisto.osgi.log.version");
    }

    final Collection<String> exports = new ArrayList<>(8);
    exports.add(export("org.slf4j", version_slf4j));
    exports.add(export("org.slf4j.event", version_slf4j));
    exports.add(export("org.slf4j.helpers", version_slf4j));
    exports.add(export("org.slf4j.spi", version_slf4j));
    exports.add(export("org.osgi.service.log", version_osgi_log));
    exports.add(export("sun.misc", "0.0.0"));
    exports.add(export("sun.misc.resources", "0.0.0"));

    exports.forEach(e -> log.debug("export: {}", e));
    final String joined = String.join(",", exports);
    log.debug("exports joined: {}", joined);
    config_props.put(FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, joined);
  }

  private static String bundleStateText(
    final Bundle bundle)
  {
    switch (bundle.getState()) {
      case Bundle.ACTIVE: {
        return "ACTIVE";
      }
      case Bundle.INSTALLED: {
        return "INSTALLED";
      }
      case Bundle.RESOLVED: {
        return "RESOLVED";
      }
      case Bundle.UNINSTALLED: {
        return "UNINSTALLED";
      }
    }

    throw new UnreachableCodeException();
  }

  private static String export(
    final String package_name,
    final String version)
  {
    return String.format("%s;version=\"%s\"", package_name, version);
  }
}
