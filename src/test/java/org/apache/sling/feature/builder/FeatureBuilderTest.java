/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.builder;

import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.FeatureConstants;
import org.apache.sling.feature.Include;
import org.junit.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FeatureBuilderTest {

    private static final Map<String, Feature> FEATURES = new HashMap<>();

    static {
        final Feature f1 = new Feature(ArtifactId.parse("g/a/1"));
        f1.getVariables().put("varx", "myvalx");

        f1.getFrameworkProperties().put("foo", "2");
        f1.getFrameworkProperties().put("bar", "X");

        f1.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-bar/4.5.6", 3));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_low/2", 5));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_high/2", 5));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevel/1", 5));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevelandversion/1", 5));

        final Configuration c1 = new Configuration("org.apache.sling.foo");
        c1.getProperties().put("prop", "value");
        f1.getConfigurations().add(c1);

        FEATURES.put(f1.getId().toMvnId(), f1);
    }

    private final FeatureProvider provider = new FeatureProvider() {

        @Override
        public Feature provide(final ArtifactId id) {
            return FEATURES.get(id.getGroupId() + ":" + id.getArtifactId() + ":" + id.getVersion());
        }
    };

    private List<Map.Entry<Integer, Artifact>> getBundles(final Feature f) {
        final List<Map.Entry<Integer, Artifact>> result = new ArrayList<>();
        for(final Map.Entry<Integer, List<Artifact>> entry : f.getBundles().getBundlesByStartOrder().entrySet()) {
            for(final Artifact artifact : entry.getValue()) {
                result.add(new Map.Entry<Integer, Artifact>() {

                    @Override
                    public Integer getKey() {
                        return entry.getKey();
                    }

                    @Override
                    public Artifact getValue() {
                        return artifact;
                    }

                    @Override
                    public Artifact setValue(Artifact value) {
                        return null;
                    }
                });
            }
        }

        return result;
    }

    private void equals(final Feature expected, final Feature actuals) {
        assertFalse(expected.isAssembled());
        assertTrue(actuals.isAssembled());

        assertEquals(expected.getId(), actuals.getId());
        assertEquals(expected.getTitle(), actuals.getTitle());
        assertEquals(expected.getDescription(), actuals.getDescription());
        assertEquals(expected.getVendor(), actuals.getVendor());
        assertEquals(expected.getLicense(), actuals.getLicense());

        // variables
        assertEquals(expected.getVariables(), actuals.getVariables());

        // bundles
        final List<Map.Entry<Integer, Artifact>> expectedBundles = getBundles(expected);
        final List<Map.Entry<Integer, Artifact>> actualsBundles = getBundles(actuals);
        assertEquals(expectedBundles.size(), actualsBundles.size());
        for(final Map.Entry<Integer, Artifact> entry : expectedBundles) {
            boolean found = false;
            for(final Map.Entry<Integer, Artifact> inner : actualsBundles) {
                if ( inner.getValue().getId().equals(entry.getValue().getId()) ) {
                    found = true;
                    assertEquals("Startlevel of bundle " + entry.getValue(), entry.getKey(), inner.getKey());
                    assertEquals("Metadata of bundle " + entry.getValue(), entry.getValue().getMetadata(), inner.getValue().getMetadata());
                    break;
                }
            }
            assertTrue("Bundle " + entry.getValue() + " in level " + entry.getKey(), found);
        }

        // configurations
        assertEquals(expected.getConfigurations().size(), actuals.getConfigurations().size());
        for(final Configuration cfg : expected.getConfigurations()) {
            final Configuration found = (cfg.isFactoryConfiguration() ? actuals.getConfigurations().getFactoryConfiguration(cfg.getFactoryPid(), cfg.getName())
                                                                      : actuals.getConfigurations().getConfiguration(cfg.getPid()));
            assertNotNull("Configuration " + cfg, found);
            assertEquals("Configuration " + cfg, cfg.getProperties(), found.getProperties());
        }

        // frameworkProperties
        assertEquals(expected.getFrameworkProperties(), actuals.getFrameworkProperties());

        // requirements
        assertEquals(expected.getRequirements().size(), actuals.getRequirements().size());
        for(final Requirement r : expected.getRequirements()) {
            boolean found = false;
            for(final Requirement i : actuals.getRequirements()) {
                if ( r.equals(i) ) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

        // capabilities
        assertEquals(expected.getCapabilities().size(), actuals.getCapabilities().size());
        for(final Capability r : expected.getCapabilities()) {
            boolean found = false;
            for(final Capability i : actuals.getCapabilities()) {
                if ( r.equals(i) ) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

        // extensions
        assertEquals(expected.getExtensions().size(), actuals.getExtensions().size());
        for(final Extension ext : expected.getExtensions()) {
            final Extension inner = actuals.getExtensions().getByName(ext.getName());
            assertNotNull(inner);
            assertEquals(ext.getType(), inner.getType());
            switch ( ext.getType()) {
                case JSON : assertEquals(ext.getJSON(), inner.getJSON());
                            break;
                case TEXT : assertEquals(ext.getText(), inner.getText());
                            break;
                case ARTIFACTS : assertEquals(ext.getArtifacts().size(), inner.getArtifacts().size());
                                 for(final Artifact art : ext.getArtifacts()) {
                                     boolean found = false;
                                     for(final Artifact i : inner.getArtifacts()) {
                                         if ( art.getId().equals(i.getId()) ) {
                                             found = true;
                                             assertEquals(art.getMetadata(), i.getMetadata());
                                             break;
                                         }
                                     }
                                     assertTrue(found);
                                 }
            }
        }

        // includes should always be empty
        assertNull(actuals.getInclude());
    }

    @Test public void testNoIncludesNoUpgrade() throws Exception {
        final Feature base = new Feature(ArtifactId.parse("org.apache.sling/test-feature/1.1"));

        final Requirement r1 = new RequirementImpl(null, "osgi.contract",
                Collections.singletonMap("filter", "(&(osgi.contract=JavaServlet)(version=3.1))"), null);
        base.getRequirements().add(r1);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("osgi.implementation", "osgi.http");
        attrs.put("version:Version", "1.1");
        final Capability c1 = new CapabilityImpl(null, "osgi.implementation",
                Collections.singletonMap("uses", "javax.servlet,javax.servlet.http,org.osgi.service.http.context,org.osgi.service.http.whiteboard"),
                attrs);
        base.getCapabilities().add(c1);
        final Capability c2 = new CapabilityImpl(null, "osgi.service",
                Collections.singletonMap("uses", "org.osgi.service.http.runtime,org.osgi.service.http.runtime.dto"),
                Collections.singletonMap("objectClass:List<String>", "org.osgi.service.http.runtime.HttpServiceRuntime"));
        base.getCapabilities().add(c2);

        base.getFrameworkProperties().put("foo", "1");
        base.getFrameworkProperties().put("brave", "something");
        base.getFrameworkProperties().put("org.apache.felix.scr.directory", "launchpad/scr");

        final Artifact a1 = new Artifact(ArtifactId.parse("org.apache.sling/oak-server/1.0.0"));
        a1.getMetadata().put(Artifact.KEY_START_ORDER, "1");
        a1.getMetadata().put("hash", "4632463464363646436");
        base.getBundles().add(a1);
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/application-bundle/2.0.0", 1));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/another-bundle/2.1.0", 1));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-xyz/1.2.3", 2));

        final Configuration co1 = new Configuration("my.pid");
        co1.getProperties().put("foo", 5L);
        co1.getProperties().put("bar", "test");
        co1.getProperties().put("number", 7);
        base.getConfigurations().add(co1);

        final Configuration co2 = new Configuration("my.factory.pid", "name");
        co2.getProperties().put("a.value", "yeah");
        base.getConfigurations().add(co2);

        assertFalse(base.isAssembled());

        final Feature assembled = FeatureBuilder.assemble(base, new BuilderContext(provider));

        equals(base, assembled);
    }

    @Test public void testSingleInclude() throws Exception {
        final Feature base = new Feature(ArtifactId.parse("org.apache.sling/test-feature/1.1"));
        final Include i1 = new Include(ArtifactId.parse("g/a/1"));
        base.setInclude(i1);

        final Requirement r1 = new RequirementImpl(null, "osgi.contract",
                Collections.singletonMap("filter", "(&(osgi.contract=JavaServlet)(version=3.1))"), null);
        base.getRequirements().add(r1);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("osgi.implementation", "osgi.http");
        attrs.put("version:Version", "1.1");
        final Capability c1 = new CapabilityImpl(null, "osgi.implementation",
                Collections.singletonMap("uses", "javax.servlet,javax.servlet.http,org.osgi.service.http.context,org.osgi.service.http.whiteboard"),
                attrs);
        base.getCapabilities().add(c1);

        i1.getFrameworkPropertiesRemovals().add("foo");
        base.getFrameworkProperties().put("foo", "1");
        base.getFrameworkProperties().put("brave", "something");
        base.getFrameworkProperties().put("org.apache.felix.scr.directory", "launchpad/scr");

        final Artifact a1 = new Artifact(ArtifactId.parse("org.apache.sling/oak-server/1.0.0"));
        a1.getMetadata().put(Artifact.KEY_START_ORDER, "1");
        a1.getMetadata().put("hash", "4632463464363646436");
        a1.getMetadata().put("org-feature", "org.apache.sling:test-feature:1.1");
        base.getBundles().add(a1);
        Map.Entry<String, String> md = new AbstractMap.SimpleEntry<String, String>("org-feature", "org.apache.sling:test-feature:1.1");
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/application-bundle/2.0.0", 1, md));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/another-bundle/2.1.0", 1, md));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-xyz/1.2.3", 2, md));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_low/1", 5, md));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_high/5", 5, md));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevel/1", 10, md));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevelandversion/2", 10, md));

        final Configuration co1 = new Configuration("my.pid");
        co1.getProperties().put("foo", 5L);
        co1.getProperties().put("bar", "test");
        co1.getProperties().put("number", 7);
        base.getConfigurations().add(co1);

        final Configuration co2 = new Configuration("my.factory.pid", "name");
        co2.getProperties().put("a.value", "yeah");
        base.getConfigurations().add(co2);

        assertFalse(base.isAssembled());

        // create the expected result
        final Feature result = base.copy();
        result.getVariables().put("varx", "myvalx");
        result.setInclude(null);
        result.getFrameworkProperties().put("bar", "X");
        Map.Entry<String, String> md2 = new AbstractMap.SimpleEntry<String, String>("org-feature", "g:a:1");
        result.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-bar/4.5.6", 3, md2));
        final Configuration co3 = new Configuration("org.apache.sling.foo");
        co3.getProperties().put("prop", "value");
        result.getConfigurations().add(co3);

        BuilderContext builderContext = new BuilderContext(provider);
        builderContext.addArtifactsOverrides(Arrays.asList(
                "group:testnewversion_low:LATEST",
                "group:testnewversion_high:LATEST",
                "group:testnewstartlevelandversion:LATEST"));

        // assemble
        final Feature assembled = FeatureBuilder.assemble(base, builderContext);

        // and test
        equals(result, assembled);
    }

    @Test public void testDeduplicationInclude() throws Exception {
        final ArtifactId idA = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId idB = ArtifactId.fromMvnId("g:b:1.0.0");

        final Feature a = new Feature(idA);
        final Feature b = new Feature(idB);
        // feature b includes feature a
        final Include inc = new Include(idA);
        b.setInclude(inc);

        // assemble application, it should only contain feature b as a is included by b
        Feature[] features = FeatureBuilder.deduplicate(new BuilderContext(new FeatureProvider() {
            @Override
            public Feature provide(ArtifactId id) {
                return null;
            }
        }), a, b);
        assertEquals(1, features.length);
        assertEquals(idB, features[0].getId());
    }

    @Test public void testDeduplicationVersion() throws Exception {
        final ArtifactId idA = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId idB = ArtifactId.fromMvnId("g:a:1.1.0");

        final Feature a = new Feature(idA);
        final Feature b = new Feature(idB);

        // assemble application, it should only contain feature b as a is included by b
        Feature[] features = FeatureBuilder.deduplicate(new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(ArtifactId id) {
                return null;
            }
        }), a, b);
        assertEquals(1, features.length);
        assertEquals(idB, features[0].getId());
    }

    @Test public void testBundleRemoveWithExactVersion() throws Exception {
        final ArtifactId bundleA1 = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId bundleA2 = ArtifactId.fromMvnId("g:a:1.1.0");
        final ArtifactId bundleB = ArtifactId.fromMvnId("g:b:1.1.0");

        final Feature a = new Feature(ArtifactId.fromMvnId("g:a-base:1"));
        a.getBundles().add(new Artifact(bundleA1));
        a.getBundles().add(new Artifact(bundleA2));
        a.getBundles().add(new Artifact(bundleB));
        final Feature b = new Feature(ArtifactId.fromMvnId("g:a-include:1"));
        final Include inc = new Include(a.getId());
        inc.getBundleRemovals().add(bundleA2);
        b.setInclude(inc);

        // assemble feature include
        Feature feature = FeatureBuilder.assemble(b, new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(ArtifactId id) {
                if ( id.equals(a.getId()) ) {
                    return a;
                }
                return null;
            }
        }));
        final Set<ArtifactId> set = new HashSet<>();
        for(final Artifact c : feature.getBundles()) {
            set.add(c.getId());
        }
        assertEquals(2, set.size());
        assertTrue(set.contains(bundleA1));
        assertTrue(set.contains(bundleB));
    }

    @Test public void testBundleRemoveWithAnyVersion() throws Exception {
        final ArtifactId bundleA1 = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId bundleA2 = ArtifactId.fromMvnId("g:a:1.1.0");
        final ArtifactId bundleB = ArtifactId.fromMvnId("g:b:1.1.0");

        final Feature a = new Feature(ArtifactId.fromMvnId("g:a-base:1"));
        a.getBundles().add(new Artifact(bundleA1));
        a.getBundles().add(new Artifact(bundleA2));
        a.getBundles().add(new Artifact(bundleB));
        final Feature b = new Feature(ArtifactId.fromMvnId("g:a-include:1"));
        final Include inc = new Include(a.getId());
        inc.getBundleRemovals().add(ArtifactId.fromMvnId("g:a:0.0.0"));
        b.setInclude(inc);

        // assemble feature include
        Feature feature = FeatureBuilder.assemble(b, new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(ArtifactId id) {
                if ( id.equals(a.getId()) ) {
                    return a;
                }
                return null;
            }
        }));
        final Set<ArtifactId> set = new HashSet<>();
        for(final Artifact c : feature.getBundles()) {
            set.add(c.getId());
        }
        assertEquals(1, set.size());
        assertTrue(set.contains(bundleB));
    }

    @Test public void testBundleRemoveNoMatch() throws Exception {
        final ArtifactId bundleA1 = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId bundleA2 = ArtifactId.fromMvnId("g:a:1.1.0");
        final ArtifactId bundleB = ArtifactId.fromMvnId("g:b:1.1.0");

        final Feature a = new Feature(ArtifactId.fromMvnId("g:a-base:1"));
        a.getBundles().add(new Artifact(bundleA1));
        a.getBundles().add(new Artifact(bundleB));
        final Feature b = new Feature(ArtifactId.fromMvnId("g:a-include:1"));
        final Include inc = new Include(a.getId());
        inc.getBundleRemovals().add(bundleA2);
        b.setInclude(inc);

        try {
            FeatureBuilder.assemble(b, new BuilderContext(new FeatureProvider() {

                @Override
                public Feature provide(ArtifactId id) {
                    if ( id.equals(a.getId()) ) {
                        return a;
                    }
                    return null;
                }
            }));
            fail();
        } catch ( final IllegalStateException ise) {
            // expected
        }
    }

    @Test public void testExtensionArtifactRemoveWithExactVersion() throws Exception {
        final ArtifactId bundleA1 = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId bundleA2 = ArtifactId.fromMvnId("g:a:1.1.0");
        final ArtifactId bundleB = ArtifactId.fromMvnId("g:b:1.1.0");

        final Feature a = new Feature(ArtifactId.fromMvnId("g:a-base:1"));
        final Extension e = new Extension(ExtensionType.ARTIFACTS, "foo", false);
        e.getArtifacts().add(new Artifact(bundleA1));
        e.getArtifacts().add(new Artifact(bundleA2));
        e.getArtifacts().add(new Artifact(bundleB));
        a.getExtensions().add(e);
        final Feature b = new Feature(ArtifactId.fromMvnId("g:a-include:1"));
        final Include inc = new Include(a.getId());
        inc.getArtifactExtensionRemovals().put("foo", Arrays.asList(bundleA2));
        b.setInclude(inc);

        // assemble feature include
        Feature feature = FeatureBuilder.assemble(b, new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(ArtifactId id) {
                if ( id.equals(a.getId()) ) {
                    return a;
                }
                return null;
            }
        }));
        final Set<ArtifactId> set = new HashSet<>();
        for(final Artifact c : feature.getExtensions().getByName("foo").getArtifacts()) {
            set.add(c.getId());
        }
        assertEquals(2, set.size());
        assertTrue(set.contains(bundleA1));
        assertTrue(set.contains(bundleB));
    }

    @Test public void testExtensionArtifactRemoveWithAnyVersion() throws Exception {
        final ArtifactId bundleA1 = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId bundleA2 = ArtifactId.fromMvnId("g:a:1.1.0");
        final ArtifactId bundleB = ArtifactId.fromMvnId("g:b:1.1.0");

        final Feature a = new Feature(ArtifactId.fromMvnId("g:a-base:1"));
        final Extension e = new Extension(ExtensionType.ARTIFACTS, "foo", false);
        e.getArtifacts().add(new Artifact(bundleA1));
        e.getArtifacts().add(new Artifact(bundleA2));
        e.getArtifacts().add(new Artifact(bundleB));
        a.getExtensions().add(e);
        final Feature b = new Feature(ArtifactId.fromMvnId("g:a-include:1"));
        final Include inc = new Include(a.getId());
        inc.getArtifactExtensionRemovals().put("foo", Arrays.asList(ArtifactId.fromMvnId("g:a:0.0.0")));
        b.setInclude(inc);

        // assemble feature include
        Feature feature = FeatureBuilder.assemble(b, new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(ArtifactId id) {
                if ( id.equals(a.getId()) ) {
                    return a;
                }
                return null;
            }
        }));
        final Set<ArtifactId> set = new HashSet<>();
        for(final Artifact c : feature.getExtensions().getByName("foo").getArtifacts()) {
            set.add(c.getId());
        }
        assertEquals(1, set.size());
        assertTrue(set.contains(bundleB));
    }

    @Test public void testExtensionArtifactRemoveNoMatch() throws Exception {
        final ArtifactId bundleA1 = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId bundleA2 = ArtifactId.fromMvnId("g:a:1.1.0");
        final ArtifactId bundleB = ArtifactId.fromMvnId("g:b:1.1.0");

        final Feature a = new Feature(ArtifactId.fromMvnId("g:a-base:1"));
        final Extension e = new Extension(ExtensionType.ARTIFACTS, "foo", false);
        e.getArtifacts().add(new Artifact(bundleA1));
        e.getArtifacts().add(new Artifact(bundleB));
        a.getExtensions().add(e);
        final Feature b = new Feature(ArtifactId.fromMvnId("g:a-include:1"));
        final Include inc = new Include(a.getId());
        inc.getArtifactExtensionRemovals().put("foo", Arrays.asList(bundleA2));
        b.setInclude(inc);

        try {
            FeatureBuilder.assemble(b, new BuilderContext(new FeatureProvider() {

                @Override
                public Feature provide(ArtifactId id) {
                    if ( id.equals(a.getId()) ) {
                        return a;
                    }
                    return null;
                }
            }));
            fail();
        } catch ( final IllegalStateException ise) {
            // expected
        }
    }

    @Test public void testIncludedFeatureProvided() throws Exception {
        final ArtifactId idA = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId idB = ArtifactId.fromMvnId("g:b:1.0.0");

        final Feature a = new Feature(idA);
        final Feature b = new Feature(idB);
        // feature b includes feature a
        final Include inc = new Include(idA);
        b.setInclude(inc);

        // assemble feature, it should only contain feature b as a is included by b
        final Feature target = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:F:1.0.0"), new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(ArtifactId id) {
                return null;
            }
                }), a, b);
        final Extension list = target.getExtensions().getByName(FeatureConstants.EXTENSION_NAME_ASSEMBLED_FEATURES);
        assertNotNull(list);
        assertEquals(1, list.getArtifacts().size());
        assertEquals(idB, list.getArtifacts().get(0).getId());
    }

    @Test public void testHandleVars() throws Exception {
        ArtifactId aid = new ArtifactId("gid", "aid", "1.2.3", null, null);
        Feature feature = new Feature(aid);
        Map<String,String> kvMap = feature.getVariables();
        kvMap.put("var1", "bar");
        kvMap.put("varvariable", "${myvar}");
        kvMap.put("var.2", "2");


        assertEquals("foobarfoo", FeatureBuilder.replaceVariables("foo${var1}foo", null, feature));
        assertEquals("barbarbar", FeatureBuilder.replaceVariables("${var1}${var1}${var1}", null, feature));
        assertEquals("${}test${myvar}2", FeatureBuilder.replaceVariables("${}test${varvariable}${var.2}", null, feature ));
        assertEquals("${undefined}",FeatureBuilder.replaceVariables("${undefined}", null, feature));
    }

    @Test public void testHandleVarsWithConflict() throws Exception {
        ArtifactId aid = new ArtifactId("gid", "aid", "1.2.3", null, null);
        ArtifactId bid = new ArtifactId("gid", "bid", "2.0.0", null, null);

        Feature aFeature = new Feature(aid);
        Feature bFeature = new Feature(bid);

        Map<String,String> kvMapA = aFeature.getVariables();
        kvMapA.put("var1", "val1");
        kvMapA.put("var2", "val2");

        Map<String,String> kvMapB = bFeature.getVariables();
        kvMapB.put("var1", "val1");
        kvMapB.put("var2", "val2");
        kvMapB.put("var3", "val3");

        Map<String,String> override = new HashMap<>();
        override.put("var3", "valo");
        override.put("val4", "notused");

        Feature cFeature = FeatureBuilder.assemble(new ArtifactId("gid", "cid", "3.0.0", null, null), new BuilderContext(new FeatureProvider()
        {
            @Override
            public Feature provide(ArtifactId id)
            {
                return null;
            }
                }).addVariablesOverwrites(override), aFeature, bFeature);

        Map<String,String> vars = new HashMap<>();
        vars.putAll(kvMapA);
        vars.putAll(kvMapB);

        assertFalse(cFeature.getVariables().equals(vars));
        vars.put("var3", "valo");

        assertTrue(cFeature.getVariables().equals(vars));

        kvMapB.put("var2", "valm");

        try {
            FeatureBuilder.assemble(new ArtifactId("gid", "cid", "3.0.0", null, null), new BuilderContext(new FeatureProvider()
            {
                @Override
                public Feature provide(ArtifactId id)
                {
                    return null;
                }
                    }).addVariablesOverwrites(override), aFeature, bFeature);
            fail("Excepted merge exception");
        } catch (IllegalStateException expected) {}

        override.put("var2", "valo");

        cFeature = FeatureBuilder.assemble(new ArtifactId("gid", "cid", "3.0.0", null, null), new BuilderContext(new FeatureProvider()
        {
            @Override
            public Feature provide(ArtifactId id)
            {
                return null;
            }
                }).addVariablesOverwrites(override), aFeature, bFeature);

        vars = new HashMap<>();
        vars.putAll(kvMapA);
        vars.putAll(kvMapB);
        vars.put("var2", "valo");
        vars.put("var3", "valo");

        assertTrue(cFeature.getVariables().equals(vars));

        override.put("var2", null);

        cFeature = FeatureBuilder.assemble(new ArtifactId("gid", "cid", "3.0.0", null, null), new BuilderContext(new FeatureProvider()
        {
            @Override
            public Feature provide(ArtifactId id)
            {
                return null;
            }
                }).addVariablesOverwrites(override), aFeature, bFeature);

        vars.put("var2", null);
        assertTrue(cFeature.getVariables().equals(vars));

    }

    @Test
    public void testFinalFlag() throws Exception {
        // feature inclusion without final flag is already tested by other tests
        final ArtifactId idA = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId idB = ArtifactId.fromMvnId("g:b:1.0.0");

        final Feature a = new Feature(idA);
        a.setFinal(true);
        final Feature b = new Feature(idB);
        // feature b includes feature a
        final Include inc = new Include(idA);
        b.setInclude(inc);

        // assemble feature, this should throw an exception
        try {
            FeatureBuilder.assemble(b, new BuilderContext(new FeatureProvider() {

                @Override
                public Feature provide(ArtifactId id) {
                    if (id.equals(idA)) {
                        return a;
                    }
                    return null;
                }
            }));
            fail();
        } catch (final IllegalStateException ise) {
            assertTrue(ise.getMessage().contains(" final "));
        }
    }
}
