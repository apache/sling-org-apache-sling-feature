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
package org.apache.sling.feature.io.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;
import org.junit.Test;
import org.osgi.resource.Capability;

public class FeatureJSONReaderTest {

    @Test public void testRead() throws Exception {
        final Feature feature = U.readFeature("test");
        assertNotNull(feature);
        assertNotNull(feature.getId());
        assertEquals("org.apache.sling", feature.getId().getGroupId());
        assertEquals("test-feature", feature.getId().getArtifactId());
        assertEquals("1.1", feature.getId().getVersion());
        assertEquals("jar", feature.getId().getType());
        assertNull(feature.getId().getClassifier());

        assertEquals(2, feature.getConfigurations().size());
        final Configuration cfg1 = U.findConfiguration(feature.getConfigurations(), "my.pid");
        assertEquals(7, cfg1.getProperties().get("number"));
        final Configuration cfg2 = U.findConfiguration(feature.getConfigurations(), "my.factory.pid~name");
        assertEquals("yeah", cfg2.getProperties().get("a.value"));

        assertEquals(3, feature.getCapabilities().size());
        Capability capability = U.findCapability(feature.getCapabilities(),"osgi.service");
        assertNotNull(capability.getAttributes().get("objectClass"));

        assertEquals(Arrays.asList("org.osgi.service.http.runtime.HttpServiceRuntime"), capability.getAttributes().get("objectClass"));

    }

    @Test public void testReadRepoInitExtension() throws Exception {
        Feature feature = U.readFeature("repoinit");
        Extensions extensions = feature.getExtensions();
        assertEquals(1, extensions.size());
        Extension ext = extensions.iterator().next();
        assertEquals("some repo init\ntext", ext.getText());
    }

    @Test public void testReadRepoInitExtensionArray() throws Exception {
        Feature feature = U.readFeature("repoinit2");
        Extensions extensions = feature.getExtensions();
        assertEquals(1, extensions.size());
        Extension ext = extensions.iterator().next();
        assertEquals("some repo init\ntext\n", ext.getText());
    }

    @Test public void testReadArtifactsExtensions() throws Exception {
        final Feature feature = U.readFeature("artifacts-extension");
        ArtifactsExtensions.testReadArtifactsExtensions(feature);
    }

    @Test
    public void testFinalFlag() throws Exception {
        final Feature featureA = U.readFeature("test");
        assertFalse(featureA.isFinal());

        final Feature featureB = U.readFeature("final");
        assertTrue(featureB.isFinal());
    }

    @Test
    public void testCompleteFlag() throws Exception {
        final Feature featureA = U.readFeature("test");
        assertFalse(featureA.isComplete());

        final Feature featureB = U.readFeature("complete");
        assertTrue(featureB.isComplete());
    }

    @Test
    public void testReadMultiBSNVer() throws Exception {
        final Feature f = U.readFeature("test3");
        Bundles fb = f.getBundles();
        assertEquals(2, fb.size());
        assertTrue(fb.containsExact(ArtifactId.fromMvnId("org.apache.sling:foo:1.2.3")));
        assertTrue(fb.containsExact(ArtifactId.fromMvnId("org.apache.sling:foo:4.5.6")));
        assertFalse(fb.containsExact(ArtifactId.fromMvnId("org.apache.sling:foo:7.8.9")));
    }

    @Test
    public void readComments() throws Exception {
        // we only test whether the feature can be read without problems
        U.readFeature("feature-model");
    }
}
