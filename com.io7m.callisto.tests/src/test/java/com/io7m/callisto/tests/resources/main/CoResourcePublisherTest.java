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

import com.io7m.callisto.resources.api.CoResourceCatalogParserProviderType;
import com.io7m.callisto.resources.api.CoResourceType;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import static com.io7m.callisto.resources.api.CoResourcePublicationType.TOPIC_ERRORS;
import static com.io7m.callisto.resources.api.CoResourceType.RESOURCES_MANIFEST_HEADER;
import static org.osgi.service.event.EventConstants.EVENT_TOPIC;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public final class CoResourcePublisherTest
{
  @Configuration
  public Option[] config()
  {
    return CoreOptions.options(
      CoreOptions.url("link:classpath:com.io7m.jnull.core.link"),
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

  @Inject
  public CoResourceCatalogParserProviderType parsers;

  @Inject
  public BundleContext context;

  @Inject
  public EventAdmin events;

  @Test
  public void testMissingResourceFile()
    throws Exception
  {
    final Hashtable<String, Object> props = new Hashtable<>();
    props.put(EVENT_TOPIC, TOPIC_ERRORS);

    final ArrayList<Event> saved_events = new ArrayList<>();
    final EventHandler handler = saved_events::add;
    this.context.registerService(EventHandler.class, handler, props);

    final TinyBundle bb =
      TinyBundles
        .bundle()
        .set(RESOURCES_MANIFEST_HEADER, "/catalog.crc")
        .set("Bundle-SymbolicName", "com.io7m.callisto.tests.bundle0");

    final File file = new File(this.folder.getRoot(), "empty.jar");
    Files.copy(bb.build(), file.toPath());

    final Collection<ServiceReference<CoResourceType>> refs0 =
      this.context.getServiceReferences(CoResourceType.class, null);

    Assert.assertEquals(0L, (long) refs0.size());

    this.context.installBundle(file.toURI().toString());

    final Collection<ServiceReference<CoResourceType>> refs1 =
      this.context.getServiceReferences(CoResourceType.class, null);

    Assert.assertEquals(0L, (long) refs1.size());
    Assert.assertEquals(1L, (long) saved_events.size());
    Assert.assertThat(
      (String) saved_events.get(0).getProperty("message"),
      StringContains.containsString("File not found"));
  }

  @Test
  public void testBadResourceFile()
    throws Exception
  {
    final Hashtable<String, Object> props = new Hashtable<>();
    props.put(EVENT_TOPIC, TOPIC_ERRORS);

    final ArrayList<Event> saved_events = new ArrayList<>();
    final EventHandler handler = saved_events::add;
    this.context.registerService(EventHandler.class, handler, props);

    final TinyBundle bb =
      TinyBundles
        .bundle()
        .set(RESOURCES_MANIFEST_HEADER, "/catalog.crc")
        .set("Bundle-SymbolicName", "com.io7m.callisto.tests.bundle0")
        .add(
          "catalog.crc",
          CoResourcePublisherTest.class.getResource("bad-catalog.txt"));

    final File file = new File(this.folder.getRoot(), "empty.jar");
    Files.copy(bb.build(), file.toPath());

    final Collection<ServiceReference<CoResourceType>> refs0 =
      this.context.getServiceReferences(CoResourceType.class, null);

    Assert.assertEquals(0L, (long) refs0.size());

    this.context.installBundle(file.toURI().toString());

    final Collection<ServiceReference<CoResourceType>> refs1 =
      this.context.getServiceReferences(CoResourceType.class, null);

    Assert.assertEquals(0L, (long) refs1.size());
    Assert.assertEquals(1L, (long) saved_events.size());
    Assert.assertThat(
      (String) saved_events.get(0).getProperty("message"),
      StringContains.containsString("Incorrect R type syntax"));
  }

  @Test
  public void testGoodResourceFile()
    throws Exception
  {
    final Hashtable<String, Object> props = new Hashtable<>();
    props.put(EVENT_TOPIC, TOPIC_ERRORS);

    final ArrayList<Event> saved_events = new ArrayList<>();
    final EventHandler handler = saved_events::add;
    this.context.registerService(EventHandler.class, handler, props);

    final TinyBundle bb =
      TinyBundles
        .bundle()
        .set(RESOURCES_MANIFEST_HEADER, "/catalog.crc")
        .set("Bundle-SymbolicName", "com.io7m.callisto.tests.bundle0")
        .add(
          "catalog.crc",
          CoResourcePublisherTest.class.getResource("good-catalog.txt"));

    final File file = new File(this.folder.getRoot(), "empty.jar");
    Files.copy(bb.build(), file.toPath());

    final Collection<ServiceReference<CoResourceType>> refs0 =
      this.context.getServiceReferences(CoResourceType.class, null);

    Assert.assertEquals(0L, (long) refs0.size());

    this.context.installBundle(file.toURI().toString());

    final Collection<ServiceReference<CoResourceType>> refs1 =
      this.context.getServiceReferences(CoResourceType.class, null);

    Assert.assertEquals(1L, (long) refs1.size());
    Assert.assertEquals(0L, (long) saved_events.size());
  }
}
