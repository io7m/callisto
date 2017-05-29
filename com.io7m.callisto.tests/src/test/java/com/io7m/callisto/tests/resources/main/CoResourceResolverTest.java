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

import com.io7m.callisto.resources.api.CoResource;
import com.io7m.callisto.resources.api.CoResourceExceptionNonexistent;
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;

import static com.io7m.callisto.resources.api.CoResourcePackageNamespace.NAMESPACE;
import static java.nio.charset.StandardCharsets.UTF_8;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public final class CoResourceResolverTest
{
  @Configuration
  public Option[] config()
  {
    return CoreOptions.options(
      CoreOptions.url("link:classpath:com.io7m.jnull.core.link"),
      CoreOptions.url("link:classpath:com.io7m.junreachable.core.link"),
      CoreOptions.url("link:classpath:it.unimi.dsi.fastutil.link"),
      CoreOptions.url("link:classpath:com.io7m.jcip.annotations.link"),
      CoreOptions.url("link:classpath:com.io7m.callisto.core.link"),
      CoreOptions.url("link:classpath:com.io7m.callisto.resources.api.link"),
      CoreOptions.url("link:classpath:com.io7m.callisto.resources.main.link"),
      CoreOptions.url("link:classpath:biz.aQute.bndlib.link"),
      CoreOptions.url("link:classpath:org.ops4j.pax.tinybundles.link"),
      CoreOptions.url("link:classpath:org.apache.felix.scr.link"),
      CoreOptions.url("link:classpath:org.apache.felix.eventadmin.link"),
      CoreOptions.junitBundles());
  }

  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Inject
  public BundleContext context;

  @Inject
  public CoResourceResolverType resolver;

  @Test
  public void testResourceResolution()
    throws Exception
  {
    final StringBuilder caps = new StringBuilder(128);
    caps.append(NAMESPACE);
    caps.append(";");
    caps.append("name:String=a.b.c;");
    caps.append("version:Version=1");

    final StringBuilder info = new StringBuilder(256);
    info.append("package a.b.c");
    info.append(System.lineSeparator());

    for (int index = 0; index < 32; ++index) {
      info.append("resource hello");
      info.append(index);
      info.append(" com.io7m.callisto.text file");
      info.append(index);
      info.append(".txt");
      info.append(System.lineSeparator());
    }

    final ByteArrayInputStream pinfo =
      new ByteArrayInputStream(info.toString().getBytes(UTF_8));

    final StringBuilder data = new StringBuilder(256);
    data.append("Hello.");
    data.append(System.lineSeparator());

    final TinyBundle bdef0 =
      TinyBundles
        .bundle()
        .set("Bundle-SymbolicName", "b0")
        .set("Provide-Capability", caps.toString())
        .set("Bundle-Version", "1.0.0")
        .add("a/b/c/package-info.cpd", pinfo);

    for (int index = 0; index < 32; ++index) {
      final ByteArrayInputStream pdata =
        new ByteArrayInputStream(data.toString().getBytes(UTF_8));
      bdef0.add("a/b/c/file" + index + ".txt", pdata);
    }

    final File b0_file = new File(this.folder.getRoot(), "b0.jar");
    Files.copy(bdef0.build(), b0_file.toPath());

    final Bundle b0 =
      this.context.installBundle(b0_file.toURI().toString());

    b0.start();

    final StringBuilder reqs = new StringBuilder(128);
    reqs.append(NAMESPACE);
    reqs.append(";");
    reqs.append("filter=(&(name=a.b.c)(version>=1)(version<2))");

    final TinyBundle bdef1 =
      TinyBundles
        .bundle()
        .set("Bundle-SymbolicName", "b1")
        .set("Bundle-Version", "1.0.0")
        .set("Require-Capability", reqs.toString());

    final File b1_file = new File(this.folder.getRoot(), "b1.jar");
    Files.copy(bdef1.build(), b1_file.toPath());

    final Bundle b1 =
      this.context.installBundle(b1_file.toURI().toString());

    b1.start();

    for (int index = 0; index < 32; ++index) {
      final CoResource r =
        this.resolver.resolve(
          b1, CoResourceID.of("a.b.c", "hello" + index));
      Assert.assertEquals("a.b.c", r.id().packageName());
      Assert.assertEquals("hello" + index, r.id().name());
      Assert.assertEquals("a.b.c.hello" + index, r.id().qualifiedName());
    }

    b0.stop();

    this.expected.expect(CoResourceExceptionNonexistent.class);
    this.resolver.resolve(b1, CoResourceID.of("a.b.c", "hello0"));
  }

  @Test
  public void testNonexistentPackage()
    throws Exception
  {
    final TinyBundle bdef0 =
      TinyBundles
        .bundle()
        .set("Bundle-SymbolicName", "b0")
        .set("Bundle-Version", "1.0.0");

    final File b0_file = new File(this.folder.getRoot(), "b0.jar");
    Files.copy(bdef0.build(), b0_file.toPath());

    final Bundle b0 =
      this.context.installBundle(b0_file.toURI().toString());

    b0.start();

    this.expected.expect(CoResourceExceptionNonexistent.class);
    this.resolver.resolve(
      b0, CoResourceID.of("a.b.c", "z"));
  }
}
