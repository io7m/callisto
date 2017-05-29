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

package com.io7m.callisto.tests.resources.api;

import com.io7m.callisto.resources.api.CoResourceExceptionBundleDuplicate;
import com.io7m.callisto.resources.api.CoResourceExceptionBundleMalformed;
import com.io7m.callisto.resources.api.CoResourceExceptionPackageError;
import com.io7m.callisto.resources.api.CoResourceModelType;
import mockit.Expectations;
import mockit.Mocked;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.io7m.callisto.resources.api.CoResourcePackageNamespace.NAMESPACE;
import static com.io7m.callisto.resources.api.CoResourcePackageNamespace.NAME_ATTRIBUTE_NAME;
import static com.io7m.callisto.resources.api.CoResourcePackageNamespace.VERSION_ATTRIBUTE_NAME;

public abstract class CoResourceModelContract
{
  protected abstract CoResourceModelType createEmptyModel();

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Test
  public final void testPackageExportNameMissing(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
  {
    new Expectations()
    {{
      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = wiring;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    this.expected.expect(CoResourceExceptionBundleMalformed.class);
    this.expected.expectMessage(StringContains.containsString(
      "Required attribute is missing"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testPackageExportVersionMissing(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
  {
    new Expectations()
    {{
      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = wiring;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    this.expected.expect(CoResourceExceptionBundleMalformed.class);
    this.expected.expectMessage(StringContains.containsString(
      "Required attribute is missing"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testPackageExportVersionWrongType(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
  {
    new Expectations()
    {{
      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, Integer.valueOf(23));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = wiring;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    this.expected.expect(CoResourceExceptionBundleMalformed.class);
    this.expected.expectMessage(StringContains.containsString(
      "Declared attribute is of the wrong type"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testBundleRegisterNoWiring(
    final @Mocked Bundle bundle)
  {
    new Expectations()
    {{
      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = null;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    e.bundleRegister(bundle);
    Assert.assertFalse(e.bundleIsRegistered(bundle));
  }

  @Test
  public final void testBundleRegisterNoWires(
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
  {
    new Expectations()
    {{
      final ArrayList<BundleCapability> capabilities = new ArrayList<>();

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = wiring;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    e.bundleRegister(bundle);
    Assert.assertFalse(e.bundleIsRegistered(bundle));
  }

  @Test
  public final void testBundleRegisterMissingPackageFile(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource(this.anyString);
      this.result = null;

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = wiring;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    this.expected.expect(CoResourceExceptionBundleMalformed.class);
    this.expected.expectMessage(StringContains.containsString(
      "Package declaration file is missing"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testBundleRegisterMissingFile(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/package-info.cpd");
      this.result = CoResourceModelContract.class.getResource("good0.cpd");

      bundle.getResource("/a/b/c/z.txt");
      this.result = null;

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = wiring;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    this.expected.expect(CoResourceExceptionPackageError.class);
    this.expected.expectMessage(StringContains.containsString(
      "Errors were encountered during parsing"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testBundleRegisterDuplicateFile(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/package-info.cpd");
      this.result = CoResourceModelContract.class.getResource("duplicate0.cpd");

      bundle.getResource("/a/b/c/z.txt");
      this.result = CoResourceModelContract.class.getResource("z.txt");

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = wiring;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    this.expected.expect(CoResourceExceptionPackageError.class);
    this.expected.expectMessage(StringContains.containsString(
      "Errors were encountered during parsing"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testBundleRegisterOK(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/package-info.cpd");
      this.result = CoResourceModelContract.class.getResource("good0.cpd");

      bundle.getResource("/a/b/c/z.txt");
      this.result = CoResourceModelContract.class.getResource("z.txt");

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = wiring;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    final Optional<CoResourceModelType.BundleRegisteredType> r =
      e.bundleRegister(bundle);
    Assert.assertTrue(r.isPresent());
    Assert.assertTrue(e.bundleIsRegistered(bundle));

    final CoResourceModelType.BundleRegisteredType rr = r.get();
    Assert.assertTrue(rr.exportedPackages().containsKey("a.b.c"));
  }

  @Test
  public final void testBundleRegisterTwice(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/package-info.cpd");
      this.result = CoResourceModelContract.class.getResource("good0.cpd");

      bundle.getResource("/a/b/c/z.txt");
      this.result = CoResourceModelContract.class.getResource("z.txt");

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = wiring;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    final Optional<CoResourceModelType.BundleRegisteredType> r =
      e.bundleRegister(bundle);
    Assert.assertTrue(r.isPresent());
    Assert.assertTrue(e.bundleIsRegistered(bundle));

    final CoResourceModelType.BundleRegisteredType rr = r.get();
    Assert.assertTrue(rr.exportedPackages().containsKey("a.b.c"));

    this.expected.expect(CoResourceExceptionBundleDuplicate.class);
    this.expected.expectMessage(StringContains.containsString(
      "The given bundle has already been registered"));
    e.bundleRegister(bundle);
  }
}
