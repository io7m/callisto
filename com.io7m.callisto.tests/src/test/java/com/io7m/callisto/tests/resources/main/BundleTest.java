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

package com.io7m.callisto.tests.resources.main;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.osgi.framework.namespace.PackageNamespace.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public final class BundleTest
{
  @Configuration
  public Option[] config()
  {
    return CoreOptions.options(
      CoreOptions.url("link:classpath:biz.aQute.bndlib.link"),
      CoreOptions.url("link:classpath:org.ops4j.pax.tinybundles.link"),
      CoreOptions.url("link:classpath:org.apache.felix.scr.link"),
      CoreOptions.url("link:classpath:org.apache.felix.eventadmin.link"),
      CoreOptions.junitBundles());
  }

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Inject
  public BundleContext context;

  @Test
  public void testOK()
    throws Exception
  {
    final TinyBundle bdef0 =
      TinyBundles
        .bundle()
        .set("Bundle-SymbolicName", "b0")
        .set("Bundle-Version", "1.0.0")
        .set("Export-Package", "a;version=1, b;version=1");

    final File b0_file = new File(this.folder.getRoot(), "b0.jar");
    Files.copy(bdef0.build(), b0_file.toPath());

    final TinyBundle bdef1 =
      TinyBundles
        .bundle()
        .set("Bundle-SymbolicName", "b1")
        .set("Bundle-Version", "1.0.0")
        .set("Export-Package", "b;version=2");

    final File b1_file = new File(this.folder.getRoot(), "b1.jar");
    Files.copy(bdef1.build(), b1_file.toPath());

    final TinyBundle bdef2 =
      TinyBundles
        .bundle()
        .set("Bundle-SymbolicName", "b2")
        .set("Bundle-Version", "1.0.0")
        .set("Import-Package", "a;version=1, b;version=2");

    final File b2_file = new File(this.folder.getRoot(), "b2.jar");
    Files.copy(bdef2.build(), b2_file.toPath());

    this.context.addBundleListener(
      event -> System.out.println("BUNDLE CHANGED: " + event));

    final Bundle b0 =
      this.context.installBundle(b0_file.toURI().toString());
    final Bundle b1 =
      this.context.installBundle(b1_file.toURI().toString());
    final Bundle b2 =
      this.context.installBundle(b2_file.toURI().toString());

    b0.start();
    b1.start();
    b2.start();

    {
      final BundleWiring wiring = b2.adapt(BundleWiring.class);
      final List<BundleWire> wires = wiring.getRequiredWires(
        PACKAGE_NAMESPACE);

      wires.forEach(wire -> {
        final BundleRequirement req =
          wire.getRequirement();
        final BundleCapability cap =
          wire.getCapability();
        final String bundle_source =
          req.getResource().getSymbolicName();
        final String bundle_target =
          cap.getResource().getSymbolicName();
        final String package_target =
          (String) cap.getAttributes().get(PACKAGE_NAMESPACE);

        System.out.println("Source bundle:  " + bundle_source);
        System.out.println("Target bundle:  " + bundle_target);
        System.out.println("Target package: " + package_target);
        System.out.println("--");
      });
    }
  }
}
