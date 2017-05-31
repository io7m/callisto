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

import com.io7m.callisto.resources.api.CoResourceBundleIdentifier;
import com.io7m.callisto.resources.api.CoResourceExceptionBundleDuplicate;
import com.io7m.callisto.resources.api.CoResourceExceptionBundleMalformed;
import com.io7m.callisto.resources.api.CoResourceExceptionBundleParsingError;
import com.io7m.callisto.resources.api.CoResourceExceptionNonexistent;
import com.io7m.callisto.resources.api.CoResourceID;
import com.io7m.callisto.resources.api.CoResourceLookupResult;
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
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
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
  public final void testPackageExportNonexistent(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource("simple.cpd");

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.d");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

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
      "The bundle's Provide-Capability header exports a nonexistent package"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testPackageExportNameMissing(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource("simple.cpd");

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

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
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource("empty.cpd");

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

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
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource("simple.cpd");

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, Integer.valueOf(23));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

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
  public final void testBundleRegisterDeclarationFileEmpty(
    final @Mocked Bundle bundle)
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource("empty.cpd");

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = null;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    final Optional<CoResourceModelType.BundleRegisteredType> b_opt =
      e.bundleRegister(bundle);
    Assert.assertTrue(b_opt.isPresent());
    final CoResourceModelType.BundleRegisteredType b = b_opt.get();
    Assert.assertEquals(
      CoResourceBundleIdentifier.of("a.b.c", new Version(1, 0, 0)),
      b.id());
    Assert.assertTrue(b.packagesExported().isEmpty());
    Assert.assertTrue(b.packagesPrivate().isEmpty());
    Assert.assertTrue(e.bundleIsRegistered(bundle));

    e.bundleUnregister(b);
    Assert.assertFalse(e.bundleIsRegistered(bundle));
  }

  @Test
  public final void testBundleRegisterDeclarationFileMissing(
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = null;

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = null;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    this.expected.expect(CoResourceExceptionBundleMalformed.class);
    this.expected.expectMessage(StringContains.containsString(
      "Resource bundle declaration file is missing"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testBundleRegisterDeclarationFileMissingResource(
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c.txt");
      this.result = null;

      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource(
        "missing-resource.cpd");

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = null;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    this.expected.expect(CoResourceExceptionBundleParsingError.class);
    this.expected.expectMessage(StringContains.containsString(
      "java.io.FileNotFoundException"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testBundleRegisterDeclarationFileDuplicateResource(
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c.txt");
      this.result = CoResourceModelContract.class.getResource("z.txt");

      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource(
        "duplicate-resource.cpd");

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = null;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    this.expected.expect(CoResourceExceptionBundleParsingError.class);
    this.expected.expectMessage(StringContains.containsString(
      "A resource is already exported"));
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
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource("simple.cpd");

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

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
    Assert.assertTrue(rr.packagesExported().containsKey("a.b.c"));

    e.bundleUnregister(rr);
    Assert.assertFalse(e.bundleIsRegistered(bundle));
  }

  @Test
  public final void testBundleRegisterNotRegisterable(
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      this.result = headers;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = null;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    Assert.assertFalse(e.bundleIsRegisterable(bundle));

    final Optional<CoResourceModelType.BundleRegisteredType> r =
      e.bundleRegister(bundle);
    Assert.assertFalse(r.isPresent());
    Assert.assertFalse(e.bundleIsRegistered(bundle));
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
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource("simple.cpd");

      final Map<String, Object> cap_attributes = new HashMap<>();
      cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
      cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

      capability.getAttributes();
      this.result = cap_attributes;

      final ArrayList<BundleCapability> capabilities = new ArrayList<>();
      capabilities.add(capability);

      wiring.getCapabilities(NAMESPACE);
      this.result = capabilities;

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

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
    Assert.assertTrue(rr.packagesExported().containsKey("a.b.c"));

    this.expected.expect(CoResourceExceptionBundleDuplicate.class);
    this.expected.expectMessage(StringContains.containsString(
      "The given bundle has already been registered"));
    e.bundleRegister(bundle);
  }

  @Test
  public final void testBundleResourceLookupSelfOK(
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource(
        "simple-with-resources.cpd");

      bundle.getResource("/a/b/c/d.txt");
      this.result = CoResourceModelContract.class.getResource("z.txt");

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

      bundle.getSymbolicName();
      this.result = "a.b.c";
      bundle.getVersion();
      this.result = new Version(1, 0, 0);
      bundle.adapt(BundleWiring.class);
      this.result = null;
    }};

    final CoResourceModelType e = this.createEmptyModel();
    final Optional<CoResourceModelType.BundleRegisteredType> r =
      e.bundleRegister(bundle);
    Assert.assertTrue(r.isPresent());
    Assert.assertTrue(e.bundleIsRegistered(bundle));

    final CoResourceModelType.BundleRegisteredType rr = r.get();
    Assert.assertTrue(rr.packagesPrivate().containsKey("a.b.c"));
    Assert.assertTrue(rr.packagesExported().isEmpty());

    final CoResourceID res_id =
      CoResourceID.of("a.b.c", "x");
    final CoResourceLookupResult result =
      e.bundleResourceLookup(bundle, res_id);

    Assert.assertSame(bundle, result.owner());
    Assert.assertEquals(
      CoResourceID.of("a.b.c", "x"),
      result.resource().id());
  }

  @Test
  public final void testBundleResourceLookupSelfNoPackage(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource(
        "simple.cpd");

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

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
    Assert.assertTrue(rr.packagesExported().containsKey("a.b.c"));
    Assert.assertTrue(rr.packagesPrivate().isEmpty());

    final CoResourceID res_id =
      CoResourceID.of("a.b.d", "x");

    this.expected.expect(CoResourceExceptionNonexistent.class);
    e.bundleResourceLookup(bundle, res_id);
  }

  @Test
  public final void testBundleResourceLookupSelfNoFile(
    final @Mocked BundleCapability capability,
    final @Mocked BundleWiring wiring,
    final @Mocked Bundle bundle)
    throws Exception
  {
    new Expectations()
    {{
      bundle.getResource("/a/b/c/bundle.cpd");
      this.result = CoResourceModelContract.class.getResource(
        "simple.cpd");

      bundle.getHeaders();
      final Hashtable<String, String> headers = new Hashtable<>();
      headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
      this.result = headers;

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
    Assert.assertTrue(rr.packagesExported().containsKey("a.b.c"));
    Assert.assertTrue(rr.packagesPrivate().isEmpty());

    final CoResourceID res_id =
      CoResourceID.of("a.b.c", "x");

    this.expected.expect(CoResourceExceptionNonexistent.class);
    e.bundleResourceLookup(bundle, res_id);
  }

  @Test
  public final void testBundleResourceLookupExportedOK(
    final @Mocked BundleRevision revision_0,
    final @Mocked BundleCapability capability_0,
    final @Mocked BundleWiring wiring_0,
    final @Mocked Bundle bundle_0,
    final @Mocked BundleCapability capability_1,
    final @Mocked BundleWire requirement_1,
    final @Mocked BundleWiring wiring_1,
    final @Mocked Bundle bundle_1)
    throws Exception
  {
    new Expectations()
    {{
      {
        final Map<String, Object> cap_attributes = new HashMap<>();
        cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
        cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

        capability_0.getAttributes();
        this.result = cap_attributes;

        final ArrayList<BundleCapability> capabilities = new ArrayList<>();
        capabilities.add(capability_0);

        wiring_0.getCapabilities(NAMESPACE);
        this.result = capabilities;

        bundle_0.getResource("/a/b/c/bundle.cpd");
        this.result = CoResourceModelContract.class.getResource(
          "simple-with-resources.cpd");

        bundle_0.getResource("/a/b/c/d.txt");
        this.result = CoResourceModelContract.class.getResource("z.txt");

        bundle_0.getHeaders();
        final Hashtable<String, String> headers = new Hashtable<>();
        headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
        this.result = headers;

        bundle_0.getSymbolicName();
        this.result = "a.b.c";
        bundle_0.getVersion();
        this.result = new Version(1, 0, 0);
        bundle_0.adapt(BundleWiring.class);
        this.result = wiring_0;
      }

      {
        revision_0.getBundle();
        this.result = bundle_0;

        capability_1.getRevision();
        this.result = revision_0;

        final Map<String, Object> cap_attributes = new HashMap<>();
        cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
        cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

        capability_1.getAttributes();
        this.result = cap_attributes;

        requirement_1.getCapability();
        this.result = capability_1;

        final ArrayList<BundleWire> requirements = new ArrayList<>();
        requirements.add(requirement_1);

        wiring_1.getRequiredWires(NAMESPACE);
        this.result = requirements;

        bundle_1.getHeaders();
        final Hashtable<String, String> headers = new Hashtable<>();
        this.result = headers;

        bundle_1.getSymbolicName();
        this.result = "a.b.d";
        bundle_1.getVersion();
        this.result = new Version(1, 0, 0);
        bundle_1.adapt(BundleWiring.class);
        this.result = wiring_1;
      }
    }};

    final CoResourceModelType e = this.createEmptyModel();

    final Optional<CoResourceModelType.BundleRegisteredType> reg_0_opt =
      e.bundleRegister(bundle_0);
    Assert.assertTrue(reg_0_opt.isPresent());
    Assert.assertTrue(e.bundleIsRegistered(bundle_0));

    final CoResourceModelType.BundleRegisteredType reg_0 = reg_0_opt.get();
    Assert.assertTrue(reg_0.packagesExported().containsKey("a.b.c"));
    Assert.assertTrue(reg_0.packagesPrivate().isEmpty());

    final Optional<CoResourceModelType.BundleRegisteredType> reg_1_opt =
      e.bundleRegister(bundle_1);
    Assert.assertTrue(reg_1_opt.isPresent());
    Assert.assertTrue(e.bundleIsRegistered(bundle_0));

    final CoResourceModelType.BundleRegisteredType reg_1 = reg_1_opt.get();
    Assert.assertTrue(reg_1.packagesExported().isEmpty());
    Assert.assertTrue(reg_1.packagesPrivate().isEmpty());

    final CoResourceID res_id =
      CoResourceID.of("a.b.c", "x");
    final CoResourceLookupResult res_result =
      e.bundleResourceLookup(bundle_1, res_id);

    Assert.assertEquals(res_id, res_result.resource().id());
    Assert.assertSame(bundle_0, res_result.owner());
  }

  @Test
  public final void testBundleResourceLookupExportedMissingFile(
    final @Mocked BundleRevision revision_0,
    final @Mocked BundleCapability capability_0,
    final @Mocked BundleWiring wiring_0,
    final @Mocked Bundle bundle_0,
    final @Mocked BundleCapability capability_1,
    final @Mocked BundleWire requirement_1,
    final @Mocked BundleWiring wiring_1,
    final @Mocked Bundle bundle_1)
    throws Exception
  {
    new Expectations()
    {{
      {
        final Map<String, Object> cap_attributes = new HashMap<>();
        cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
        cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

        capability_0.getAttributes();
        this.result = cap_attributes;

        final ArrayList<BundleCapability> capabilities = new ArrayList<>();
        capabilities.add(capability_0);

        wiring_0.getCapabilities(NAMESPACE);
        this.result = capabilities;

        bundle_0.getResource("/a/b/c/bundle.cpd");
        this.result = CoResourceModelContract.class.getResource(
          "simple-with-resources.cpd");

        bundle_0.getResource("/a/b/c/d.txt");
        this.result = CoResourceModelContract.class.getResource("z.txt");

        bundle_0.getHeaders();
        final Hashtable<String, String> headers = new Hashtable<>();
        headers.put("Callisto-Resource-Bundle", "/a/b/c/bundle.cpd");
        this.result = headers;

        bundle_0.getSymbolicName();
        this.result = "a.b.c";
        bundle_0.getVersion();
        this.result = new Version(1, 0, 0);
        bundle_0.adapt(BundleWiring.class);
        this.result = wiring_0;
      }

      {
        revision_0.getBundle();
        this.result = bundle_0;

        capability_1.getRevision();
        this.result = revision_0;

        final Map<String, Object> cap_attributes = new HashMap<>();
        cap_attributes.put(NAME_ATTRIBUTE_NAME, "a.b.c");
        cap_attributes.put(VERSION_ATTRIBUTE_NAME, new Version(1, 0, 0));

        capability_1.getAttributes();
        this.result = cap_attributes;

        requirement_1.getCapability();
        this.result = capability_1;

        final ArrayList<BundleWire> requirements = new ArrayList<>();
        requirements.add(requirement_1);

        wiring_1.getRequiredWires(NAMESPACE);
        this.result = requirements;

        bundle_1.getHeaders();
        final Hashtable<String, String> headers = new Hashtable<>();
        this.result = headers;

        bundle_1.getSymbolicName();
        this.result = "a.b.d";
        bundle_1.getVersion();
        this.result = new Version(1, 0, 0);
        bundle_1.adapt(BundleWiring.class);
        this.result = wiring_1;
      }
    }};

    final CoResourceModelType e = this.createEmptyModel();

    final Optional<CoResourceModelType.BundleRegisteredType> reg_0_opt =
      e.bundleRegister(bundle_0);
    Assert.assertTrue(reg_0_opt.isPresent());
    Assert.assertTrue(e.bundleIsRegistered(bundle_0));

    final CoResourceModelType.BundleRegisteredType reg_0 = reg_0_opt.get();
    Assert.assertTrue(reg_0.packagesExported().containsKey("a.b.c"));
    Assert.assertTrue(reg_0.packagesPrivate().isEmpty());

    final Optional<CoResourceModelType.BundleRegisteredType> reg_1_opt =
      e.bundleRegister(bundle_1);
    Assert.assertTrue(reg_1_opt.isPresent());
    Assert.assertTrue(e.bundleIsRegistered(bundle_0));

    final CoResourceModelType.BundleRegisteredType reg_1 = reg_1_opt.get();
    Assert.assertTrue(reg_1.packagesExported().isEmpty());
    Assert.assertTrue(reg_1.packagesPrivate().isEmpty());

    final CoResourceID res_id =
      CoResourceID.of("a.b.c", "a");

    this.expected.expect(CoResourceExceptionNonexistent.class);
    this.expected.expectMessage(
      StringContains.containsString(
        "The requested resource is not present in the exporting bundle"));
    e.bundleResourceLookup(bundle_1, res_id);
  }
}
