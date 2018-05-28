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

import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.KeyValueMap;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApplicationBuilderTest {

    private static final Map<String, Feature> FEATURES = new HashMap<>();

    static {
        // Feature-0
        final Feature f0 = new Feature(ArtifactId.parse("g/a/1"));
        f0.getFrameworkProperties().put("foo", new String[]{"2", "3"});
        f0.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-bar/4.5.6", 3));
        f0.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_low/2", 5));

        // Feature-1
        final Feature f1 = new Feature(ArtifactId.parse("g/a/2"));
        f1.getFrameworkProperties().put("foo", new String[]{"4", "3"});
        f1.getFrameworkProperties().put("bar", "Y");

        final Configuration c11 = new Configuration("org.apache.sling.foo");
        c11.getProperties().put("prop-11", "value-11");
        c11.getProperties().put("prop-12", "value-12");
        final Configuration c12 = new Configuration("org.apache.sling.bar");
        c12.getProperties().put("prop-21", "value-21");

        f1.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-bar/4.5.7", 3));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/test_version/2", 5));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/foo/1", 5));

        f1.getConfigurations().add(c11);
        f1.getConfigurations().add(c12);

        // Feature-2
        final Feature f2 = new Feature(ArtifactId.parse("g/b/2"));
        f2.getFrameworkProperties().put("foo", new String[]{"3", "4"});
        f2.getFrameworkProperties().put("bar", "X");

        final Configuration c21 = new Configuration("org.apache.sling.foo");
        c21.getProperties().put("prop-11", "value-21");
        final Configuration c22 = new Configuration("org.apache.sling.test");
        c22.getProperties().put("prop-21", "value-31");

        f2.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-bar/4.5.7", 4));
        f2.getBundles().add(BuilderUtilTest.createBundle("group/test_version/3", 2));
        f2.getBundles().add(BuilderUtilTest.createBundle("group/bar/1", 2));

        f2.getConfigurations().add(c21);
        f2.getConfigurations().add(c22);


        FEATURES.put(f0.getId().toMvnId(), f0);
        FEATURES.put(f1.getId().toMvnId(), f1);
        FEATURES.put(f2.getId().toMvnId(), f2);
    }

    private final FeatureProvider provider = new FeatureProvider() {

        @Override
        public Feature provide(final ArtifactId id) {
            return FEATURES.get(id.getGroupId() + ":" + id.getArtifactId() + ":" + id.getVersion());
        }
    };

    @Test
    public void testFeatureNotFound() {
        boolean success = true;
        try {
            ApplicationBuilder.assemble(null, new BuilderContext(provider), "g/b/1", "g/a/1");
        } catch (IllegalStateException exp) {
            assertEquals("Unable to find included feature g/b/1", exp.getMessage());
            success = false;
        }
        assertFalse("ApplicationBuilder built application with non-existing feature", success);
    }

    @Test
    public void testDuplicateFeatures() {
        Application application = ApplicationBuilder.assemble(null, new BuilderContext(provider), "g/a/1", "g/a/2");
        List<ArtifactId> featuresInApp = application.getFeatureIds();
        // verify only one of the features in present
        assertEquals(1, featuresInApp.size());
        // verify feature with higher version is present
        assertEquals("mvn:g/a/2", featuresInApp.get(0).toMvnUrl());
        // verify bundles from feature-2 are present
        application.getBundles().containsExact(ArtifactId.parse("org.apache.sling/foo-bar/4.5.7"));

        //verify order of features doesn't change behaviour
        application = ApplicationBuilder.assemble(null, new BuilderContext(provider), "g/a/2", "g/a/1");
        featuresInApp = application.getFeatureIds();
        assertEquals(1, featuresInApp.size());
        assertEquals("mvn:g/a/2", featuresInApp.get(0).toMvnUrl());
        application.getBundles().containsExact(ArtifactId.parse("org.apache.sling/foo-bar/4.5.7"));
    }

    @Test
    public void testApplicationMerge() {
        Application application = ApplicationBuilder.assemble(null, new BuilderContext(provider), "g/a/1", "g/a/2", "g/b/2");
        Bundles bundles = application.getBundles();

        // verify bundle merge
        assertTrue(bundles.containsSame(ArtifactId.parse("org.apache.sling/foo-bar/4.5.7")));
        assertTrue(bundles.containsSame(ArtifactId.parse("group/foo/1")));
        assertTrue(bundles.containsSame(ArtifactId.parse("group/bar/1")));
        assertTrue(bundles.containsExact(ArtifactId.parse("group/test_version/3")));
        assertFalse(bundles.containsExact(ArtifactId.parse("group/test_version/2")));

        // verify bundle start order
        // verify latest artifacts wins on clash
        assertEquals(15, bundles.getSame(ArtifactId.parse("org.apache.sling/foo-bar/4.5.7")).getStartOrder());

        assertEquals(10, bundles.getSame(ArtifactId.parse("group/foo/1")).getStartOrder());
        assertEquals(13, bundles.getSame(ArtifactId.parse("group/bar/1")).getStartOrder());
        assertEquals(13, bundles.getSame(ArtifactId.parse("group/test_version/3")).getStartOrder());

        // verify framework properties
        KeyValueMap frameworkProps = application.getFrameworkProperties();
        // verify later framework properties overrides on clash
        assertEquals(2, frameworkProps.size());
        String[] prop1 = (String[]) frameworkProps.getObject("foo");
        assertEquals(2, prop1.length);
        assertEquals("3", prop1[0]);
        assertEquals("4", prop1[1]);
        String prop2 = frameworkProps.get("bar");
        assertEquals("X", prop2);

        // verify configurations
        Configurations configs = application.getConfigurations();
        assertEquals(3, configs.size());
        // verify configurations with different pid, doesn't merge properties
        assertEquals("value-21", configs.getConfiguration("org.apache.sling.bar").getProperties().get("prop-21"));
        assertEquals("value-31", configs.getConfiguration("org.apache.sling.test").getProperties().get("prop-21"));

        // verify configurations with same pid, merge properties where later overrides on clash
        assertEquals("value-21", configs.getConfiguration("org.apache.sling.foo").getProperties().get("prop-11"));
        assertEquals("value-12", configs.getConfiguration("org.apache.sling.foo").getProperties().get("prop-12"));
    }

}
