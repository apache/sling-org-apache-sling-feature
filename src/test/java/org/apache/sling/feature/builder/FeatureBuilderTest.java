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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.MatchingRequirement;
import org.apache.sling.feature.Prototype;
import org.junit.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

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

    static {
        final Feature f2 = new Feature(ArtifactId.parse("g/a/2"));

        f2.getBundles().add(BuilderUtilTest.createBundle("group/testmulti/1", 4));
        f2.getBundles().add(BuilderUtilTest.createBundle("group/testmulti/2", 8));
        f2.getBundles().add(BuilderUtilTest.createBundle("group/someart/1.2.3", 4));

        FEATURES.put(f2.getId().toMvnId(), f2);
    }

    static {
        final Feature f3 = new Feature(ArtifactId.parse("g/a/3"));

        f3.getBundles().add(BuilderUtilTest.createBundle("group/testmulti/2", 8));
        f3.getBundles().add(BuilderUtilTest.createBundle("group/someart/1.2.3", 4));

        FEATURES.put(f3.getId().toMvnId(), f3);
    }

    private final FeatureProvider provider = new FeatureProvider() {

        @Override
        public Feature provide(final ArtifactId id) {
            return FEATURES.get(id.getGroupId() + ":" + id.getArtifactId() + ":" + id.getVersion());
        }
    };

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
        final List<Artifact> expectedBundles = expected.getBundles();
        final List<Artifact> actualsBundles = actuals.getBundles();
        assertEquals(expectedBundles.size(), actualsBundles.size());
        for (int i = 0; i < expectedBundles.size(); i++) {
            final Artifact eb = expectedBundles.get(i);
            final Artifact ab = actualsBundles.get(i);
            assertEquals(eb.getId(), ab.getId());
            assertEquals("Start order of bundle " + eb, eb.getStartOrder(), ab.getStartOrder());
            assertEquals("Metadata of bundle " + eb, eb.getMetadata(), ab.getMetadata());
        }

        // configurations
        assertEquals(expected.getConfigurations().size(), actuals.getConfigurations().size());
        for(final Configuration cfg : expected.getConfigurations()) {
            final Configuration found = actuals.getConfigurations().getConfiguration(cfg.getPid());
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
                if (Objects.equals(r.getResource(), i.getResource())
                        && Objects.equals(r.getNamespace(), i.getNamespace())
                        && Objects.equals(r.getAttributes(), i.getAttributes())
                        && Objects.equals(i.getDirectives(), i.getDirectives())) {
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
                if (Objects.equals(r.getResource(), i.getResource())
                        && Objects.equals(r.getNamespace(), i.getNamespace())
                        && Objects.equals(r.getAttributes(), i.getAttributes())
                        && Objects.equals(i.getDirectives(), i.getDirectives())) {
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
        assertNull(actuals.getPrototype());
    }

    @Test public void testMergeMultipleVersions() {
        Feature a = new Feature(ArtifactId.fromMvnId("g:a:1"));
        Feature b = new Feature(ArtifactId.fromMvnId("g:b:1"));


        a.getBundles().add(BuilderUtilTest.createBundle("o/a/1.0.0", 10));
        a.getBundles().add(BuilderUtilTest.createBundle("o/a/2.0.0", 9));
        a.getBundles().add(BuilderUtilTest.createBundle("o/a/3.0.0", 11));

        b.getBundles().add(BuilderUtilTest.createBundle("o/a/4.0.0", 8));
        b.getBundles().add(BuilderUtilTest.createBundle("o/a/5.0.0", 12));
        b.getBundles().add(BuilderUtilTest.createBundle("o/a/6.0.0", 10));

        Feature ab = new Feature(ArtifactId.fromMvnId("g:ab:1"));
        ab.getBundles().add(BuilderUtilTest.createBundle("o/a/6.0.0", 8, new AbstractMap.SimpleEntry<>(
                Artifact.KEY_FEATURE_ORIGINS, a.getId() + "," + b.getId())));

        Feature assembled = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:ab:1"), new BuilderContext(provider)
                .addArtifactsOverride(ArtifactId.fromMvnId("o:a:HIGHEST")), a, b);
        assembled.getExtensions().clear();

        equals(ab, assembled );

        assembled = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:ab:1"), new BuilderContext(provider)
            .addArtifactsOverride(ArtifactId.fromMvnId("o:a:LATEST")), a, b);
        assembled.getExtensions().clear();

        equals(ab, assembled );

        ab = new Feature(ArtifactId.fromMvnId("g:ab:1"));
        for (Artifact bundle : a.getBundles()) {
            ab.getBundles()
                    .add(BuilderUtilTest.createBundle(bundle.getId().toMvnId(), bundle.getStartOrder(),
                            new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS,
                                    a.getId() + "," + b.getId())));
        }
        for (Artifact bundle : b.getBundles()) {
            ab.getBundles().add(BuilderUtilTest.createBundle(bundle.getId().toMvnId(), bundle.getStartOrder(),
                    new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, b.getId().toMvnId())));
        }

        assembled = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:ab:1"), new BuilderContext(provider)
            .addArtifactsOverride(ArtifactId.fromMvnId("o:a:ALL")), a, b);
        assembled.getExtensions().clear();


        equals(ab, assembled );

        a.getBundles().get(1).setStartOrder(1);

        ab = new Feature(ArtifactId.fromMvnId("g:ab:1"));
        ab.getBundles().add(BuilderUtilTest.createBundle("o/a/6.0.0", 1, new AbstractMap.SimpleEntry<>(
                Artifact.KEY_FEATURE_ORIGINS, a.getId() + "," + b.getId())));
        ab.getBundles().get(0).setStartOrder(1);

        assembled = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:ab:1"), new BuilderContext(provider)
            .addArtifactsOverride(ArtifactId.fromMvnId("o:a:LATEST")), a, b);
        assembled.getExtensions().clear();


        equals(ab, assembled);
    }

    @Test public void testMergeMultipleVersionsNoConflict() {
        Feature a = new Feature(ArtifactId.fromMvnId("g:a:1"));
        Feature b = new Feature(ArtifactId.fromMvnId("g:b:1"));


        a.getBundles().add(BuilderUtilTest.createBundle("o/a/1.0.0", 10));
        a.getBundles().add(BuilderUtilTest.createBundle("o/a/2.0.0", 9));
        a.getBundles().add(BuilderUtilTest.createBundle("o/a/3.0.0", 11));

        b.getBundles().add(BuilderUtilTest.createBundle("o/b/4.0.0", 8));
        b.getBundles().add(BuilderUtilTest.createBundle("o/b/5.0.0", 12));
        b.getBundles().add(BuilderUtilTest.createBundle("o/b/6.0.0", 10));

        Feature ab = new Feature(ArtifactId.fromMvnId("g:ab:1"));
        ab.getBundles().add(BuilderUtilTest.createBundle("o/a/1.0.0", 10,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, a.getId().toMvnId())));
        ab.getBundles().add(BuilderUtilTest.createBundle("o/a/2.0.0", 9,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, a.getId().toMvnId())));
        ab.getBundles().add(BuilderUtilTest.createBundle("o/a/3.0.0", 11,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, a.getId().toMvnId())));

        ab.getBundles().add(BuilderUtilTest.createBundle("o/b/4.0.0", 8,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, b.getId().toMvnId())));
        ab.getBundles().add(BuilderUtilTest.createBundle("o/b/5.0.0", 12,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, b.getId().toMvnId())));
        ab.getBundles().add(BuilderUtilTest.createBundle("o/b/6.0.0", 10,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, b.getId().toMvnId())));


        Feature assembled = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:ab:1"), new BuilderContext(provider), a, b);
        assembled.getExtensions().clear();

        equals(ab, assembled);

        assembled = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:ab:2"), new BuilderContext(provider), assembled);
        assembled.getExtensions().clear();

        Feature ab2 = new Feature(ArtifactId.fromMvnId("g:ab:2"));
        ab2.getBundles().add(BuilderUtilTest.createBundle("o/a/1.0.0", 10,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, a.getId().toMvnId())));
        ab2.getBundles().add(BuilderUtilTest.createBundle("o/a/2.0.0", 9,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, a.getId().toMvnId())));
        ab2.getBundles().add(BuilderUtilTest.createBundle("o/a/3.0.0", 11,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, a.getId().toMvnId())));

        ab2.getBundles().add(BuilderUtilTest.createBundle("o/b/4.0.0", 8,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, b.getId().toMvnId())));
        ab2.getBundles().add(BuilderUtilTest.createBundle("o/b/5.0.0", 12,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, b.getId().toMvnId())));
        ab2.getBundles().add(BuilderUtilTest.createBundle("o/b/6.0.0", 10,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, b.getId().toMvnId())));

        equals(ab2, assembled);
    }

    @Test public void testDistinctOrigions() {
        Artifact a = BuilderUtilTest.createBundle("a/b/1.0.0", 12, new AbstractMap.SimpleEntry<>(
                Artifact.KEY_FEATURE_ORIGINS, "b/b/1.0.0,b/b/1.0.0,,b/b/2.0.0"));
        Artifact b = BuilderUtilTest.createBundle("b/b/2.0.0", 12,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, "b/b/1.0.0,b/b/2.0.0"));
        assertArrayEquals(a.getFeatureOrigins(), b.getFeatureOrigins());

        a.setFeatureOrigins(new ArtifactId[]{ArtifactId.parse("b/b/1.0.0"), null, ArtifactId.parse("b/b/1.0.0"), ArtifactId.parse("b/b/2.0.0")});

        assertArrayEquals(a.getFeatureOrigins(), b.getFeatureOrigins());
    }


    @Test public void testNoIncludesNoUpgrade() throws Exception {
        final Feature base = new Feature(ArtifactId.parse("org.apache.sling/test-feature/1.1"));

        final MatchingRequirement r1 = new MatchingRequirementImpl(null, "osgi.contract",
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

        final Configuration co2 = new Configuration("my.factory.pid~name");
        co2.getProperties().put("a.value", "yeah");
        base.getConfigurations().add(co2);

        assertFalse(base.isAssembled());

        final Feature assembled = FeatureBuilder.assemble(base, new BuilderContext(provider));

        equals(base, assembled);
    }

    @Test public void testSingleInclude() throws Exception {
        final Feature base = new Feature(ArtifactId.parse("org.apache.sling/test-feature/1.1"));
        final Prototype i1 = new Prototype(ArtifactId.parse("g/a/1"));
        base.setPrototype(i1);

        final MatchingRequirement r1 = new MatchingRequirementImpl(null, "osgi.contract",
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
        base.getBundles().add(a1);
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/application-bundle/2.0.0", 1));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/another-bundle/2.1.0", 1));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-xyz/1.2.3", 2));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_low/1", 5));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_high/5", 5));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevel/1", 10));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevelandversion/2", 10));

        final Configuration co1 = new Configuration("my.pid");
        co1.getProperties().put("foo", 5L);
        co1.getProperties().put("bar", "test");
        co1.getProperties().put("number", 7);
        base.getConfigurations().add(co1);

        final Configuration co2 = new Configuration("my.factory.pid~name");
        co2.getProperties().put("a.value", "yeah");
        base.getConfigurations().add(co2);

        assertFalse(base.isAssembled());

        // create the expected result
        final Feature result = base.copy();
        result.setPrototype(null);
        result.getBundles().clear();

        result.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-bar/4.5.6", 3,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        result.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_low/2", 5,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        result.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_low/1", 5,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        result.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_high/2", 5,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        result.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_high/5", 5,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        result.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevel/1", 5,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        result.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevelandversion/1", 5,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        result.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevelandversion/2", 10,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        Artifact copy = a1.copy(a1.getId());
        copy.getMetadata().put(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId());
        result.getBundles().add(copy);
        result.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/application-bundle/2.0.0", 1,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        result.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/another-bundle/2.1.0", 1,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        result.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-xyz/1.2.3", 2,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));

        result.getVariables().put("varx", "myvalx");
        result.getFrameworkProperties().put("bar", "X");
        final Configuration co3 = new Configuration("org.apache.sling.foo");
        co3.getProperties().put("prop", "value");
        result.getConfigurations().add(co3);

        // assemble
        final Feature assembled = FeatureBuilder.assemble(base, new BuilderContext(provider));

        // and test
        equals(result, assembled);
    }

    @Test public void testSingleIncludeMultiVersion() {
        Feature base = new Feature(ArtifactId.fromMvnId("g:tgtart:1"));
        Prototype i1 = new Prototype(ArtifactId.fromMvnId("g:a:3"));
        base.setPrototype(i1);
        base.getBundles().add(new Artifact(ArtifactId.fromMvnId("g:myart:1")));
        base.getBundles().add(new Artifact(ArtifactId.fromMvnId("group:testmulti:1")));
        base.getBundles().add(new Artifact(ArtifactId.fromMvnId("group:testmulti:3")));

        BuilderContext builderContext = new BuilderContext(provider);
        Feature assembled = FeatureBuilder.assemble(base, builderContext);

        Feature result = new Feature(ArtifactId.parse("g:tgtart:1"));

        result.getBundles().add(BuilderUtilTest.createBundle("group/testmulti/2", 8,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));
        Artifact b1 = BuilderUtilTest.createBundle("group:testmulti:1",
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b1);


        Artifact b2 = BuilderUtilTest.createBundle("group:testmulti:3",
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b2);

        Artifact b3 = BuilderUtilTest.createBundle("group:someart:1.2.3",
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        b3.setStartOrder(4);
        result.getBundles().add(b3);
        Artifact b0 = BuilderUtilTest.createBundle("g:myart:1",
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b0);


        equals(result, assembled);

        Feature addOn = new Feature(ArtifactId.fromMvnId("g:addon:1"));
        addOn.getBundles().add(BuilderUtilTest.createBundle("group:someart:1.2.3"));
        assembled = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:tgtart:2"), builderContext, assembled, addOn);
        assembled.getExtensions().clear();

        result = result.copy(ArtifactId.fromMvnId("g:tgtart:2"));
        int idx = result.getBundles().indexOf(BuilderUtilTest.createBundle("group:someart:1.2.3"));
        result.getBundles().remove(idx);
        result.getBundles().add(idx,
                BuilderUtilTest.createBundle("group:someart:1.2.3", 4, new AbstractMap.SimpleEntry<>(
                        Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId() + "," + addOn.getId())));

        equals(result, assembled);
    }

    @Test public void testSingleIncludeMultiVersion2() {
        Feature base = new Feature(ArtifactId.fromMvnId("g:tgtart:1"));
        Prototype i1 = new Prototype(ArtifactId.fromMvnId("g:a:2"));
        base.setPrototype(i1);
        base.getBundles().add(new Artifact(ArtifactId.fromMvnId("g:myart:1")));

        BuilderContext builderContext = new BuilderContext(provider);
        Feature assembled = FeatureBuilder.assemble(base, builderContext);

        Feature result = new Feature(ArtifactId.parse("g:tgtart:1"));
        Artifact b1 = BuilderUtilTest.createBundle("group:testmulti:1", 4,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b1);
        Artifact b2 = BuilderUtilTest.createBundle("group:testmulti:2", 8,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b2);
        Artifact b3 = BuilderUtilTest.createBundle("group:someart:1.2.3", 4,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b3);
        Artifact b0 = BuilderUtilTest.createBundle("g:myart:1",
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b0);

        equals(result, assembled);
    }

    @Test public void testSingleIncludeMultiVersion3() {
        Feature base = new Feature(ArtifactId.fromMvnId("g:tgtart:1"));
        Prototype i1 = new Prototype(ArtifactId.fromMvnId("g:a:2"));
        base.setPrototype(i1);
        base.getBundles().add(new Artifact(ArtifactId.fromMvnId("g:myart:1")));
        base.getBundles().add(new Artifact(ArtifactId.fromMvnId("group:testmulti:1")));

        BuilderContext builderContext = new BuilderContext(provider);
        Feature assembled = FeatureBuilder.assemble(base, builderContext);

        Feature result = new Feature(ArtifactId.parse("g:tgtart:1"));

        result.getBundles().add(BuilderUtilTest.createBundle("group/testmulti/2", 8,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));

        Artifact b1 = BuilderUtilTest.createBundle("group:testmulti:1", 4,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b1);
        Artifact b3 = BuilderUtilTest.createBundle("group:someart:1.2.3", 4,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b3);
        Artifact b0 = BuilderUtilTest.createBundle("g:myart:1",
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b0);

        equals(result, assembled);
    }

    @Test public void testSingleIncludeMultiVersion4() {
        Feature base = new Feature(ArtifactId.fromMvnId("g:tgtart:1"));
        Prototype i1 = new Prototype(ArtifactId.fromMvnId("g:a:2"));
        base.setPrototype(i1);
        base.getBundles().add(new Artifact(ArtifactId.fromMvnId("g:myart:1")));
        base.getBundles().add(new Artifact(ArtifactId.fromMvnId("group:testmulti:1")));
        base.getBundles().add(new Artifact(ArtifactId.fromMvnId("group:testmulti:3")));

        BuilderContext builderContext = new BuilderContext(provider);
        Feature assembled = FeatureBuilder.assemble(base, builderContext);

        Feature result = new Feature(ArtifactId.parse("g:tgtart:1"));

        result.getBundles().add(BuilderUtilTest.createBundle("group/testmulti/2", 8,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId())));

        Artifact b1 = BuilderUtilTest.createBundle("group:testmulti:1", 4,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b1);

        Artifact b2 = BuilderUtilTest.createBundle("group:testmulti:3",
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b2);

        Artifact b3 = BuilderUtilTest.createBundle("group:someart:1.2.3", 4,
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b3);
        Artifact b0 = BuilderUtilTest.createBundle("g:myart:1",
                new AbstractMap.SimpleEntry<>(Artifact.KEY_FEATURE_ORIGINS, base.getId().toMvnId()));
        result.getBundles().add(b0);

        equals(result, assembled);
    }

    @Test public void testDeduplicationInclude() throws Exception {
        final ArtifactId idA = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId idB = ArtifactId.fromMvnId("g:b:1.0.0");

        final Feature a = new Feature(idA);
        final Feature b = new Feature(idB);
        // feature b includes feature a
        final Prototype inc = new Prototype(idA);
        b.setPrototype(inc);

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

    @Test public void testMergeIncludeDedup() throws Exception {
        final ArtifactId idA = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId idB = ArtifactId.fromMvnId("g:b:1.0.0");

        final Feature a = new Feature(idA);
        ArtifactId b1ID = ArtifactId.fromMvnId("g:bundle1:1.2.3");
        ArtifactId b2ID = ArtifactId.fromMvnId("g:bundle2:4.5.6");
        Artifact b1 = new Artifact(b1ID);
        Artifact b2 = new Artifact(b2ID);
        a.getBundles().add(b1);
        a.getBundles().add(b2);

        final Feature b = new Feature(idB);
        // feature b includes feature a and removes a bundle
        final Prototype inc = new Prototype(idA);
        b.setPrototype(inc);
        inc.getBundleRemovals().add(b1ID);

        // Merge all features together
        ArtifactId c = ArtifactId.fromMvnId("g:c:1.0.0");
        Feature fc = FeatureBuilder.assemble(c, new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(ArtifactId id) {
                return null;
            }
        }), a, b);

        // Test that the feature that acted as a prototype is not included in the merge.
        assertEquals(1, fc.getBundles().size());
        assertEquals(b2, fc.getBundles().iterator().next());
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
        final Prototype inc = new Prototype(a.getId());
        inc.getBundleRemovals().add(bundleA2);
        b.setPrototype(inc);

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
        final Prototype inc = new Prototype(a.getId());
        inc.getBundleRemovals().add(ArtifactId.fromMvnId("g:a:0.0.0"));
        b.setPrototype(inc);

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
        final Prototype inc = new Prototype(a.getId());
        inc.getBundleRemovals().add(bundleA2);
        b.setPrototype(inc);

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
        final Extension e = new Extension(ExtensionType.ARTIFACTS, "foo", ExtensionState.OPTIONAL);
        e.getArtifacts().add(new Artifact(bundleA1));
        e.getArtifacts().add(new Artifact(bundleA2));
        e.getArtifacts().add(new Artifact(bundleB));
        a.getExtensions().add(e);
        final Feature b = new Feature(ArtifactId.fromMvnId("g:a-include:1"));
        final Prototype inc = new Prototype(a.getId());
        inc.getArtifactExtensionRemovals().put("foo", Arrays.asList(bundleA2));
        b.setPrototype(inc);

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
        final Extension e = new Extension(ExtensionType.ARTIFACTS, "foo", ExtensionState.OPTIONAL);
        e.getArtifacts().add(new Artifact(bundleA1));
        e.getArtifacts().add(new Artifact(bundleA2));
        e.getArtifacts().add(new Artifact(bundleB));
        a.getExtensions().add(e);
        final Feature b = new Feature(ArtifactId.fromMvnId("g:a-include:1"));
        final Prototype inc = new Prototype(a.getId());
        inc.getArtifactExtensionRemovals().put("foo", Arrays.asList(ArtifactId.fromMvnId("g:a:0.0.0")));
        b.setPrototype(inc);

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
        final Extension e = new Extension(ExtensionType.ARTIFACTS, "foo", ExtensionState.OPTIONAL);
        e.getArtifacts().add(new Artifact(bundleA1));
        e.getArtifacts().add(new Artifact(bundleB));
        a.getExtensions().add(e);
        final Feature b = new Feature(ArtifactId.fromMvnId("g:a-include:1"));
        final Prototype inc = new Prototype(a.getId());
        inc.getArtifactExtensionRemovals().put("foo", Arrays.asList(bundleA2));
        b.setPrototype(inc);

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
        final Prototype inc = new Prototype(idA);
        b.setPrototype(inc);

        // assemble feature, it should only contain feature b as a is included by b
        final Feature target = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:F:1.0.0"), new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(ArtifactId id) {
                return null;
            }
                }), a, b);
        final Extension list = target.getExtensions().getByName(Extension.EXTENSION_NAME_ASSEMBLED_FEATURES);
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
        kvMap.put("var-3", "3");


        assertEquals("foobarfoo", FeatureBuilder.replaceVariables("foo${var1}foo", null, feature));
        assertEquals("barbarbar", FeatureBuilder.replaceVariables("${var1}${var1}${var1}", null, feature));
        assertEquals("${}test${myvar}2", FeatureBuilder.replaceVariables("${}test${varvariable}${var.2}", null, feature ));
        assertEquals("${undefined}",FeatureBuilder.replaceVariables("${undefined}", null, feature));
        assertEquals("var-3",FeatureBuilder.replaceVariables("var-${var-3}", null, feature));
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
                }).addVariablesOverrides(override), aFeature, bFeature);

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
                    }).addVariablesOverrides(override), aFeature, bFeature);
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
                }).addVariablesOverrides(override), aFeature, bFeature);

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
                }).addVariablesOverrides(override), aFeature, bFeature);

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
        final Prototype inc = new Prototype(idA);
        b.setPrototype(inc);

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

    @Test
    public void testConfigurationMerge() {
        Feature a = new Feature(ArtifactId.fromMvnId("g:a:1.0.0"));
        a.getConfigurations().add(new Configuration("foo"));
        Feature b = new Feature(ArtifactId.fromMvnId("g:b:1.0.0"));
        b.getConfigurations().add(new Configuration("bar"));

        Feature c = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:c:1.0.0"), new BuilderContext(provider), a, b);
        c.getExtensions().clear();

        Feature test = new Feature(ArtifactId.fromMvnId("g:c:1.0.0"));
        test.getConfigurations().add(new Configuration("foo"));
        test.getConfigurations().add(new Configuration("bar"));

        assertEquals(test, c);

        b.getConfigurations().add(new Configuration("foo"));

        try
        {
            FeatureBuilder.assemble(ArtifactId.fromMvnId("g:c:1.0.0"), new BuilderContext(provider), a, b);
            fail();
        } catch (IllegalStateException ex) {

        }

        b.getConfigurations().get(0).getProperties().put("foo", "bar");

        c = FeatureBuilder.assemble(ArtifactId.fromMvnId("g:c:1.0.0"), new BuilderContext(provider).addConfigsOverrides(Collections.singletonMap("*", BuilderContext.CONFIG_USE_LATEST)), a, b);

        test.getConfigurations().add(b.getConfigurations().get(0));

        c.getExtensions().clear();

        assertEquals(test, c);
    }

    /**
     * Merge three features. First feature has no extension, second and third have.
     * Make sure that the extension of the second feature is not modified.
     * Then use the aggregated result and merge another feature into it and
     * merge sure thet the extension of the first aggregation is not modified
     * (see SLING-9260)
     */
    @Test public void testCopyOfExtensionWhenMerging() {
        final Feature f1 = new Feature(ArtifactId.parse("g/a/1"));

        final Feature f2 = new Feature(ArtifactId.parse("g/b/1"));
        Extension e2 = new Extension(ExtensionType.TEXT, Extension.EXTENSION_NAME_REPOINIT, ExtensionState.REQUIRED);
        e2.setText("line2");
        f2.getExtensions().add(e2);

        final Feature f3 = new Feature(ArtifactId.parse("g/c/1"));
        Extension e3 = new Extension(ExtensionType.TEXT, Extension.EXTENSION_NAME_REPOINIT, ExtensionState.REQUIRED);
        e3.setText("line3");
        f3.getExtensions().add(e3);

        final BuilderContext bc = new BuilderContext(provider);
        final Feature f = FeatureBuilder.assemble(ArtifactId.parse("f/f/1"), bc, f1, f2, f3);
        assertEquals("line2\nline3", f.getExtensions().getByName(Extension.EXTENSION_NAME_REPOINIT).getText());

        // e2 is not modified
        assertEquals("line2", e2.getText());

        final Feature f4 = new Feature(ArtifactId.parse("g/c/1"));
        Extension e4 = new Extension(ExtensionType.TEXT, Extension.EXTENSION_NAME_REPOINIT, ExtensionState.REQUIRED);
        e4.setText("line4");
        f4.getExtensions().add(e4);

        final Feature ff = FeatureBuilder.assemble(ArtifactId.parse("f/g/1"), bc, f, f4);
        assertEquals("line2\nline3\nline4", ff.getExtensions().getByName(Extension.EXTENSION_NAME_REPOINIT).getText());
        assertEquals("line2\nline3", f.getExtensions().getByName(Extension.EXTENSION_NAME_REPOINIT).getText());
    }

    @Test public void testReplaceVarInArray() {
        final Feature f = new Feature(ArtifactId.parse("g/a/1"));
        f.getVariables().put("key", "hello");
        final Configuration c = new Configuration("pid");
        c.getProperties().put("prop", new String[] {"${key}", "world"});
        f.getConfigurations().add(c);

        FeatureBuilder.resolveVariables(f, null);

        String[] result = (String[]) c.getProperties().get("prop");
        assertEquals(2, result.length);
        assertEquals("hello", result[0]);
        assertEquals("world", result[1]);
    }

    private static class MatchingRequirementImpl extends RequirementImpl implements MatchingRequirement {

        public MatchingRequirementImpl(Resource res, String ns, Map<String, String> dirs, Map<String, Object> attrs) {
            super(res, ns, dirs, attrs);
        }
    }
}
