/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.io7m.callisto.container;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

// CHECKSTYLE:OFF

final class CoAutoProcessor
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(CoAutoProcessor.class);
  }

  /**
   * The property name used for the bundle directory.
   **/

  public static final String AUTO_DEPLOY_DIR_PROPERTY = "felix.auto.deploy.dir";

  /**
   * The default name used for the bundle directory.
   **/

  public static final String AUTO_DEPLOY_DIR_VALUE = "bundle";

  /**
   * The property name used to specify auto-deploy actions.
   **/

  public static final String AUTO_DEPLOY_ACTION_PROPERTY = "felix.auto.deploy.action";

  /**
   * The property name used to specify auto-deploy start level.
   **/
  public static final String AUTO_DEPLOY_STARTLEVEL_PROPERTY = "felix.auto.deploy.startlevel";

  /**
   * The name used for the auto-deploy install action.
   **/

  public static final String AUTO_DEPLOY_INSTALL_VALUE = "install";

  /**
   * The name used for the auto-deploy start action.
   **/

  public static final String AUTO_DEPLOY_START_VALUE = "start";

  /**
   * The name used for the auto-deploy update action.
   **/

  public static final String AUTO_DEPLOY_UPDATE_VALUE = "update";

  /**
   * The name used for the auto-deploy uninstall action.
   **/

  public static final String AUTO_DEPLOY_UNINSTALL_VALUE = "uninstall";

  /**
   * The property name prefix for the launcher's auto-install property.
   **/

  public static final String AUTO_INSTALL_PROP = "felix.auto.install";

  /**
   * The property name prefix for the launcher's auto-start property.
   **/

  public static final String AUTO_START_PROP = "felix.auto.start";

  private CoAutoProcessor()
  {

  }

  /**
   * Used to instigate auto-deploy directory process and auto-install/auto-start
   * configuration property processing during.
   *
   * @param configMap Map of configuration properties.
   * @param context   The system bundle context.
   **/

  public static void process(
    Map configMap,
    final BundleContext context)
  {
    configMap = (configMap == null) ? new HashMap() : configMap;
    processAutoDeploy(configMap, context);
    processAutoProperties(configMap, context);
  }

  /**
   * <p>
   * Processes bundles in the auto-deploy directory, performing the
   * specified deploy actions.
   * </p>
   */

  private static void processAutoDeploy(
    final Map configMap,
    final BundleContext context)
  {
    // Determine if auto deploy actions to perform.
    String action = (String) configMap.get(AUTO_DEPLOY_ACTION_PROPERTY);
    action = (action == null) ? "" : action;
    final List actionList = new ArrayList();
    final StringTokenizer st = new StringTokenizer(action, ",");
    while (st.hasMoreTokens()) {
      final String s = st.nextToken().trim().toLowerCase();
      if (s.equals(AUTO_DEPLOY_INSTALL_VALUE)
        || s.equals(AUTO_DEPLOY_START_VALUE)
        || s.equals(AUTO_DEPLOY_UPDATE_VALUE)
        || s.equals(AUTO_DEPLOY_UNINSTALL_VALUE)) {
        actionList.add(s);
      }
    }

    LOG.debug("auto deploy actions: {}", Integer.valueOf(actionList.size()));

    // Perform auto-deploy actions.
    if (actionList.size() > 0) {
      // Retrieve the Start Level service, since it will be needed
      // to set the start level of the installed bundles.
      final StartLevel sl = (StartLevel) context.getService(
        context.getServiceReference(StartLevel.class.getName()));

      // Get start level for auto-deploy bundles.
      int startLevel = sl.getInitialBundleStartLevel();
      if (configMap.get(AUTO_DEPLOY_STARTLEVEL_PROPERTY) != null) {
        try {
          startLevel = Integer.parseInt(
            configMap.get(AUTO_DEPLOY_STARTLEVEL_PROPERTY).toString());
        } catch (final NumberFormatException ex) {
          // Ignore and keep default level.
        }
      }

      // Get list of already installed bundles as a map.
      final Map installedBundleMap = new HashMap();
      final Bundle[] bundles = context.getBundles();
      for (int i = 0; i < bundles.length; i++) {
        installedBundleMap.put(bundles[i].getLocation(), bundles[i]);
      }

      // Get the auto deploy directory.
      String autoDir = (String) configMap.get(AUTO_DEPLOY_DIR_PROPERTY);
      autoDir = (autoDir == null) ? AUTO_DEPLOY_DIR_VALUE : autoDir;
      // Look in the specified bundle directory to create a list
      // of all JAR files to install.
      final File[] files = new File(autoDir).listFiles();
      final List jarList = new ArrayList();
      if (files != null) {
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
          if (files[i].getName().endsWith(".jar")) {
            jarList.add(files[i]);
          }
        }
      }

      // Install bundle JAR files and remember the bundle objects.
      final List startBundleList = new ArrayList();
      for (int i = 0; i < jarList.size(); i++) {
        // Look up the bundle by location, removing it from
        // the map of installed bundles so the remaining bundles
        // indicate which bundles may need to be uninstalled.
        final String bundle_location = ((File) jarList.get(i)).toURI().toString();
        Bundle b = (Bundle) installedBundleMap.remove(bundle_location);

        try {
          // If the bundle is not already installed, then install it
          // if the 'install' action is present.
          if ((b == null) && actionList.contains(AUTO_DEPLOY_INSTALL_VALUE)) {
            b = installBundle(context, bundle_location);
          }
          // If the bundle is already installed, then update it
          // if the 'update' action is present.
          else if ((b != null) && actionList.contains(AUTO_DEPLOY_UPDATE_VALUE)) {
            b.update();
          }

          // If we have found and/or successfully installed a bundle,
          // then add it to the list of bundles to potentially start
          // and also set its start level accordingly.
          if ((b != null) && !isFragment(b)) {
            startBundleList.add(b);
            sl.setBundleStartLevel(b, startLevel);
          }
        } catch (final BundleException ex) {
          LOG.error("auto-deploy install failure: ", ex);
        }
      }

      // Uninstall all bundles not in the auto-deploy directory if
      // the 'uninstall' action is present.
      if (actionList.contains(AUTO_DEPLOY_UNINSTALL_VALUE)) {
        for (final Iterator it = installedBundleMap.entrySet().iterator(); it.hasNext(); ) {
          final Map.Entry entry = (Map.Entry) it.next();
          final Bundle b = (Bundle) entry.getValue();
          if (b.getBundleId() != 0L) {
            try {
              b.uninstall();
            } catch (final BundleException ex) {
              LOG.error("auto-deploy uninstall failure: ", ex);
            }
          }
        }
      }

      // Start all installed and/or updated bundles if the 'start'
      // action is present.
      if (actionList.contains(AUTO_DEPLOY_START_VALUE)) {
        for (int i = 0; i < startBundleList.size(); i++) {
          try {
            ((Bundle) startBundleList.get(i)).start();
          } catch (final BundleException ex) {
            LOG.error("auto-deploy start failure: ", ex);
          }
        }
      }
    }
  }

  /**
   * <p>
   * Processes the auto-install and auto-start properties from the
   * specified configuration properties.
   * </p>
   */
  private static void processAutoProperties(
    final Map configMap,
    final BundleContext context)
  {
    // Retrieve the Start Level service, since it will be needed
    // to set the start level of the installed bundles.
    final StartLevel sl = (StartLevel) context.getService(
      context.getServiceReference(StartLevel.class.getName()));

    // Retrieve all auto-install and auto-start properties and install
    // their associated bundles. The auto-install property specifies a
    // space-delimited list of bundle URLs to be automatically installed
    // into each new profile, while the auto-start property specifies
    // bundles to be installed and started. The start level to which the
    // bundles are assigned is specified by appending a ".n" to the
    // property name, where "n" is the desired start level for the list
    // of bundles. If no start level is specified, the default start
    // level is assumed.
    for (final Iterator i = configMap.keySet().iterator(); i.hasNext(); ) {
      final String key = ((String) i.next()).toLowerCase();

      // Ignore all keys that are not an auto property.
      if (!key.startsWith(AUTO_INSTALL_PROP) && !key.startsWith(
        AUTO_START_PROP)) {
        continue;
      }

      // If the auto property does not have a start level,
      // then assume it is the default bundle start level, otherwise
      // parse the specified start level.
      int startLevel = sl.getInitialBundleStartLevel();
      if (!key.equals(AUTO_INSTALL_PROP) && !key.equals(
        AUTO_START_PROP)) {
        try {
          startLevel = Integer.parseInt(key.substring(key.lastIndexOf('.') + 1));
        } catch (final NumberFormatException ex) {
          LOG.error("invalid property: {}: ", key, ex);
        }
      }

      // Parse and install the bundles associated with the key.
      final StringTokenizer st = new StringTokenizer(
        (String) configMap.get(key),
        "\" ",
        true);
      for (String location = nextLocation(st); location != null; location = nextLocation(
        st)) {
        try {
          final Bundle b = installBundle(context, location);
          sl.setBundleStartLevel(b, startLevel);
        } catch (final Exception ex) {
          LOG.error("auto property install: {}: ", location, ex);
        }
      }
    }

    // Now loop through the auto-start bundles and start them.
    for (final Iterator i = configMap.keySet().iterator(); i.hasNext(); ) {
      final String key = ((String) i.next()).toLowerCase();
      if (key.startsWith(AUTO_START_PROP)) {
        final StringTokenizer st = new StringTokenizer(
          (String) configMap.get(key),
          "\" ",
          true);
        for (String location = nextLocation(st);
             location != null;
             location = nextLocation(st)) {
          // Installing twice just returns the same bundle.
          try {
            final Bundle b =
              installBundle(context, location);
            if (b != null) {
              LOG.debug("starting {}", b.getSymbolicName());
              b.start();
            }
          } catch (final Exception ex) {
            LOG.error(
              "auto property start: {}: ", location, ex);
          }
        }
      }
    }
  }

  private static Bundle installBundle(
    final BundleContext context,
    final String location)
    throws BundleException
  {
    LOG.debug("install: {}", location);
    return context.installBundle(location, null);
  }

  private static String nextLocation(final StringTokenizer st)
  {
    String retVal = null;

    if (st.countTokens() > 0) {
      String tokenList = "\" ";
      StringBuffer tokBuf = new StringBuffer(10);
      String tok = null;
      boolean inQuote = false;
      boolean tokStarted = false;
      boolean exit = false;
      while ((st.hasMoreTokens()) && (!exit)) {
        tok = st.nextToken(tokenList);
        if (tok.equals("\"")) {
          inQuote = !inQuote;
          if (inQuote) {
            tokenList = "\"";
          } else {
            tokenList = "\" ";
          }

        } else if (tok.equals(" ")) {
          if (tokStarted) {
            retVal = tokBuf.toString();
            tokStarted = false;
            tokBuf = new StringBuffer(10);
            exit = true;
          }
        } else {
          tokStarted = true;
          tokBuf.append(tok.trim());
        }
      }

      // Handle case where end of token stream and
      // still got data
      if ((!exit) && (tokStarted)) {
        retVal = tokBuf.toString();
      }
    }

    return retVal;
  }

  private static boolean isFragment(final Bundle bundle)
  {
    return bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
  }
}
