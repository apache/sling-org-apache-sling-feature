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
package org.apache.sling.feature.osgi;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Hashtable;

import org.apache.felix.cm.json.io.Configurations;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConvertersTest {

    @Test
    public void testEmptyFeatureConversion() throws Exception {
        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));

        // convert to OSGi feature
        final org.osgi.service.feature.Feature osgiFeature = Converters.convert(feature);
        assertEquals("g:a:1", osgiFeature.getID().toString());
        assertFalse(osgiFeature.getDescription().isPresent());
        assertFalse(osgiFeature.getDocURL().isPresent());
        assertFalse(osgiFeature.getLicense().isPresent());
        assertFalse(osgiFeature.getSCM().isPresent());
        assertFalse(osgiFeature.getName().isPresent());
        assertFalse(osgiFeature.getVendor().isPresent());
        assertFalse(osgiFeature.isComplete());
        assertTrue(osgiFeature.getCategories().isEmpty());
        assertTrue(osgiFeature.getBundles().isEmpty());
        assertTrue(osgiFeature.getConfigurations().isEmpty());
        assertTrue(osgiFeature.getExtensions().isEmpty());
        assertTrue(osgiFeature.getVariables().isEmpty());

        // and back to Sling Feature
        final Feature slingFeature = Converters.convert(osgiFeature);
        assertEquals("g:a:1", slingFeature.getId().toMvnId());
        assertNull(slingFeature.getDescription());
        assertNull(slingFeature.getDocURL());
        assertNull(slingFeature.getLicense());
        assertNull(slingFeature.getSCMInfo());
        assertNull(slingFeature.getTitle());
        assertNull(slingFeature.getVendor());
        assertFalse(slingFeature.isComplete());
        assertTrue(slingFeature.getCategories().isEmpty());
        assertTrue(slingFeature.getBundles().isEmpty());
        assertTrue(slingFeature.getConfigurations().isEmpty());
        assertTrue(slingFeature.getExtensions().isEmpty());
        assertTrue(slingFeature.getVariables().isEmpty());
    }

    @Test
    public void testMetadataConversion() throws Exception {
        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        feature.setComplete(true);
        feature.setDescription("description");
        feature.setDocURL("doc-url");
        feature.setLicense("license");
        feature.setSCMInfo("info");
        feature.setTitle("title");
        feature.setVendor("vendor");
        feature.getCategories().add("c1");
        feature.getCategories().add("c2");

        // convert to OSGi feature
        final org.osgi.service.feature.Feature osgiFeature = Converters.convert(feature);
        assertEquals("g:a:1", osgiFeature.getID().toString());
        assertEquals("description", osgiFeature.getDescription().get());
        assertEquals("doc-url", osgiFeature.getDocURL().get());
        assertEquals("license", osgiFeature.getLicense().get());
        assertEquals("info", osgiFeature.getSCM().get());
        assertEquals("title", osgiFeature.getName().get());
        assertEquals("vendor", osgiFeature.getVendor().get());
        assertTrue(osgiFeature.isComplete());
        assertEquals(Arrays.asList("c1", "c2"), osgiFeature.getCategories());

        // and back to Sling Feature
        final Feature slingFeature = Converters.convert(osgiFeature);
        assertEquals("g:a:1", slingFeature.getId().toMvnId());
        assertEquals("description", slingFeature.getDescription());
        assertEquals("doc-url", slingFeature.getDocURL());
        assertEquals("license", slingFeature.getLicense());
        assertEquals("info", slingFeature.getSCMInfo());
        assertEquals("title", slingFeature.getTitle());
        assertEquals("vendor", slingFeature.getVendor());
        assertTrue(slingFeature.isComplete());
        assertEquals(Arrays.asList("c1", "c2"), slingFeature.getCategories());
    }

    @Test
    public void testBundleConversion() throws Exception {
        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        final Artifact bundle = new Artifact(ArtifactId.parse("g:b:2"));
        bundle.getMetadata().put("key", "foo");
        feature.getBundles().add(bundle);

        // convert to OSGi feature
        final org.osgi.service.feature.Feature osgiFeature = Converters.convert(feature);
        assertEquals(1, osgiFeature.getBundles().size());
        final org.osgi.service.feature.FeatureBundle ob =
                osgiFeature.getBundles().get(0);
        assertEquals("g:b:2", ob.getID().toString());
        assertEquals(1, ob.getMetadata().size());
        assertEquals("foo", ob.getMetadata().get("key"));

        // and back to Sling Feature
        final Feature slingFeature = Converters.convert(osgiFeature);
        assertEquals(1, slingFeature.getBundles().size());
        final Artifact sb = slingFeature.getBundles().get(0);
        assertEquals("g:b:2", sb.getId().toMvnId());
        assertEquals(1, sb.getMetadata().size());
        assertEquals("foo", sb.getMetadata().get("key"));
    }

    @Test
    public void testConfigurationConversion() throws Exception {
        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        final Configuration c1 = new Configuration("org.sling.config");
        c1.getProperties().put("key", "foo");
        feature.getConfigurations().add(c1);
        final Configuration c2 = new Configuration("org.sling.factory~name");
        c2.getProperties().put("value", 5);
        feature.getConfigurations().add(c2);

        // convert to OSGi feature
        final org.osgi.service.feature.Feature osgiFeature = Converters.convert(feature);
        assertEquals(2, osgiFeature.getConfigurations().size());
        final org.osgi.service.feature.FeatureConfiguration oc1 =
                osgiFeature.getConfigurations().get("org.sling.config");
        assertEquals("org.sling.config", oc1.getPid());
        assertFalse(oc1.getFactoryPid().isPresent());
        assertEquals(1, oc1.getValues().size());
        assertEquals("foo", oc1.getValues().get("key"));

        final org.osgi.service.feature.FeatureConfiguration oc2 =
                osgiFeature.getConfigurations().get("org.sling.factory~name");
        assertEquals("org.sling.factory~name", oc2.getPid());
        assertEquals("org.sling.factory", oc2.getFactoryPid().get());
        assertEquals(1, oc2.getValues().size());
        assertEquals(5, oc2.getValues().get("value"));

        // and back to Sling Feature
        final Feature slingFeature = Converters.convert(osgiFeature);
        assertEquals(2, slingFeature.getConfigurations().size());
        final Configuration sc1 = slingFeature.getConfigurations().get(0);
        assertEquals("org.sling.config", sc1.getPid());
        assertNull(sc1.getFactoryPid());
        assertNull(sc1.getName());
        assertEquals(1, sc1.getProperties().size());
        assertEquals("foo", sc1.getProperties().get("key"));

        final Configuration sc2 = slingFeature.getConfigurations().get(1);
        assertEquals("org.sling.factory~name", sc2.getPid());
        assertEquals("org.sling.factory", sc2.getFactoryPid());
        assertEquals("name", sc2.getName());
        assertEquals(1, sc2.getProperties().size());
        assertEquals(5, sc2.getProperties().get("value"));
    }

    @Test
    public void testVariablesConversion() throws Exception {
        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        feature.getVariables().put("v1", "a");
        feature.getVariableMetadata("v1").put("x", "y");

        // convert to OSGi feature
        final org.osgi.service.feature.Feature osgiFeature = Converters.convert(feature);
        assertEquals(1, osgiFeature.getVariables().size());
        assertEquals("a", osgiFeature.getVariables().get("v1"));
        assertEquals(1, osgiFeature.getExtensions().size());
        assertNotNull(osgiFeature.getExtensions().get(Extension.EXTENSION_NAME_INTERNAL_DATA));

        // and back to Sling Feature
        final Feature slingFeature = Converters.convert(osgiFeature);
        assertEquals(1, slingFeature.getVariables().size());
        assertEquals("a", slingFeature.getVariables().get("v1"));
        assertEquals("y", slingFeature.getVariableMetadata("v1").get("x"));
        assertTrue(slingFeature.getExtensions().isEmpty());
    }

    @Test
    public void testFrameworkPropertiesConversion() throws Exception {
        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        feature.getFrameworkProperties().put("v1", "a");
        feature.getFrameworkPropertyMetadata("v1").put("x", "y");

        // convert to OSGi feature
        final org.osgi.service.feature.Feature osgiFeature = Converters.convert(feature);
        assertEquals(2, osgiFeature.getExtensions().size());
        assertNotNull(osgiFeature.getExtensions().get(Extension.EXTENSION_NAME_INTERNAL_DATA));
        final org.osgi.service.feature.FeatureExtension e =
                osgiFeature.getExtensions().get("framework-launching-properties");
        assertNotNull(e);
        assertEquals(org.osgi.service.feature.FeatureExtension.Type.JSON, e.getType());
        try (final StringReader r = new StringReader(e.getJSON())) {
            final Hashtable<String, Object> p = Configurations.buildReader()
                    .verifyAsBundleResource(true)
                    .build(r)
                    .readConfiguration();
            assertEquals(1, p.size());
            assertEquals("a", p.get("v1"));
        }

        // and back to Sling Feature
        final Feature slingFeature = Converters.convert(osgiFeature);
        assertEquals(1, slingFeature.getFrameworkProperties().size());
        assertEquals("a", slingFeature.getFrameworkProperties().get("v1"));
        assertEquals("y", slingFeature.getFrameworkPropertyMetadata("v1").get("x"));
        assertTrue(slingFeature.getExtensions().isEmpty());
    }

    @Test
    public void testJSONExtensionConversion() throws Exception {
        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        final Extension e = new Extension(ExtensionType.JSON, "ext", ExtensionState.OPTIONAL);
        e.setJSON("{\"a\":true}");
        feature.getExtensions().add(e);

        // convert to OSGi feature
        final org.osgi.service.feature.Feature osgiFeature = Converters.convert(feature);
        assertEquals(1, osgiFeature.getExtensions().size());
        final org.osgi.service.feature.FeatureExtension oe =
                osgiFeature.getExtensions().get("ext");
        assertNotNull(oe);
        assertEquals(org.osgi.service.feature.FeatureExtension.Type.JSON, oe.getType());
        assertEquals("{\"a\":true}", oe.getJSON());

        // and back to Sling Feature
        final Feature slingFeature = Converters.convert(osgiFeature);
        assertEquals(1, slingFeature.getExtensions().size());
        final Extension se = slingFeature.getExtensions().getByName("ext");
        assertNotNull(se);
        assertEquals(ExtensionType.JSON, se.getType());
        assertEquals("{\"a\":true}", se.getJSON());
    }

    @Test
    public void testTextExtensionConversion() throws Exception {
        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        final Extension e = new Extension(ExtensionType.TEXT, "ext", ExtensionState.OPTIONAL);
        e.setText("Hello World");
        feature.getExtensions().add(e);

        // convert to OSGi feature
        final org.osgi.service.feature.Feature osgiFeature = Converters.convert(feature);
        assertEquals(1, osgiFeature.getExtensions().size());
        final org.osgi.service.feature.FeatureExtension oe =
                osgiFeature.getExtensions().get("ext");
        assertNotNull(oe);
        assertEquals(org.osgi.service.feature.FeatureExtension.Type.TEXT, oe.getType());
        assertEquals("Hello World", String.join("\n", oe.getText()));

        // and back to Sling Feature
        final Feature slingFeature = Converters.convert(osgiFeature);
        assertEquals(1, slingFeature.getExtensions().size());
        final Extension se = slingFeature.getExtensions().getByName("ext");
        assertNotNull(se);
        assertEquals(ExtensionType.TEXT, se.getType());
        assertEquals("Hello World", se.getText());
    }

    @Test
    public void testArtifactExtensionConversion() throws Exception {
        final Feature feature = new Feature(ArtifactId.parse("g:a:1"));
        final Extension e = new Extension(ExtensionType.ARTIFACTS, "ext", ExtensionState.OPTIONAL);
        final Artifact artifact = new Artifact(ArtifactId.parse("g:b:2"));
        artifact.getMetadata().put("key", "foo");
        e.getArtifacts().add(artifact);
        feature.getExtensions().add(e);

        // convert to OSGi feature
        final org.osgi.service.feature.Feature osgiFeature = Converters.convert(feature);
        assertEquals(1, osgiFeature.getExtensions().size());
        final org.osgi.service.feature.FeatureExtension oe =
                osgiFeature.getExtensions().get("ext");
        assertNotNull(oe);
        assertEquals(org.osgi.service.feature.FeatureExtension.Type.ARTIFACTS, oe.getType());
        assertEquals(1, oe.getArtifacts().size());
        final org.osgi.service.feature.FeatureArtifact oa = oe.getArtifacts().get(0);
        assertEquals("g:b:2", oa.getID().toString());
        assertEquals(1, oa.getMetadata().size());
        assertEquals("foo", oa.getMetadata().get("key"));

        // and back to Sling Feature
        final Feature slingFeature = Converters.convert(osgiFeature);
        assertEquals(1, slingFeature.getExtensions().size());
        final Extension se = slingFeature.getExtensions().getByName("ext");
        assertNotNull(se);
        assertEquals(ExtensionType.ARTIFACTS, se.getType());
        assertEquals(1, se.getArtifacts().size());
        final Artifact sa = se.getArtifacts().get(0);
        assertEquals("g:b:2", sa.getId().toMvnId());
        assertEquals(1, sa.getMetadata().size());
        assertEquals("foo", sa.getMetadata().get("key"));
    }
}
