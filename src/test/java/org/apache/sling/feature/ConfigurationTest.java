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
package org.apache.sling.feature;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConfigurationTest {

    @Test
    public void testNullArgument() {
        try {
            new Configuration(null);
            fail();
        } catch (final IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testFactoryPid() {
        final Configuration fc = new Configuration("org.apache.sling.factory~script");
        assertTrue(fc.isFactoryConfiguration());
        assertEquals("org.apache.sling.factory", fc.getFactoryPid());
        assertEquals("script", fc.getName());
        assertEquals("org.apache.sling.factory~script", fc.getPid());
    }

    @Test
    public void testPid() {
        final Configuration c = new Configuration("org.apache.sling.script");
        assertFalse(c.isFactoryConfiguration());
        assertNull(c.getFactoryPid());
        assertNull(c.getName());
        assertEquals("org.apache.sling.script", c.getPid());
    }

    @Test
    public void testStaticFactoryPidMethods() {
        final String factoryPid = "org.apache.sling.factory~script";
        final String pid = "org.apache.sling.script";

        assertTrue(Configuration.isFactoryConfiguration(factoryPid));
        assertEquals("org.apache.sling.factory", Configuration.getFactoryPid(factoryPid));
        assertEquals("script", Configuration.getName(factoryPid));

        assertFalse(Configuration.isFactoryConfiguration(pid));
        assertNull(Configuration.getFactoryPid(pid));
        assertNull(Configuration.getName(pid));
    }

    @Test
    public void testFeatureOrigins() {
        final ArtifactId self = ArtifactId.parse("self:self:1");

        final Configuration cfg = new Configuration("foo");
        assertTrue(cfg.getFeatureOrigins().isEmpty());
        assertNull(cfg.getConfigurationProperties().get(Configuration.PROP_FEATURE_ORIGINS));
        assertNull(cfg.getProperties().get(Configuration.PROP_FEATURE_ORIGINS));
        assertEquals(1, cfg.getFeatureOrigins(self).size());
        assertEquals(self, cfg.getFeatureOrigins(self).get(0));

        // single id
        final ArtifactId id = ArtifactId.parse("g:a:1");
        cfg.setFeatureOrigins(Collections.singletonList(id));
        assertEquals(1, cfg.getFeatureOrigins().size());
        assertEquals(id, cfg.getFeatureOrigins().get(0));
        assertEquals(1, cfg.getFeatureOrigins(self).size());
        assertEquals(id, cfg.getFeatureOrigins(self).get(0));

        assertNull(cfg.getConfigurationProperties().get(Configuration.PROP_FEATURE_ORIGINS));
        assertNotNull(cfg.getProperties().get(Configuration.PROP_FEATURE_ORIGINS));
        final String[] array = (String[]) cfg.getProperties().get(Configuration.PROP_FEATURE_ORIGINS);
        assertArrayEquals(new String[] {id.toMvnId()}, array);

        // add another id
        final ArtifactId id2 = ArtifactId.parse("g:b:2");
        cfg.setFeatureOrigins(Arrays.asList(id, id2));
        assertEquals(2, cfg.getFeatureOrigins().size());
        assertEquals(id, cfg.getFeatureOrigins().get(0));
        assertEquals(id2, cfg.getFeatureOrigins().get(1));
        assertEquals(2, cfg.getFeatureOrigins(self).size());
        assertEquals(id, cfg.getFeatureOrigins(self).get(0));
        assertEquals(id2, cfg.getFeatureOrigins(self).get(1));

        assertNull(cfg.getConfigurationProperties().get(Configuration.PROP_FEATURE_ORIGINS));
        assertNotNull(cfg.getProperties().get(Configuration.PROP_FEATURE_ORIGINS));
        final String[] array2 = (String[]) cfg.getProperties().get(Configuration.PROP_FEATURE_ORIGINS);
        assertArrayEquals(new String[] {id.toMvnId(), id2.toMvnId()}, array2);
    }

    @Test
    public void testDuplicateConfigKeys() throws Exception {
        Configuration c1 = new Configuration("a.b.c");
        c1.getProperties().put("aaa", "123");
        c1.getProperties().put("AaA", "456");

        assertEquals(
                "As keys are case insensitive, there should just be 1 key",
                1,
                c1.getProperties().size());
    }

    @Test
    public void testPropertyFeatureOrigins() {
        final ArtifactId self = ArtifactId.parse("self:self:1");

        final Configuration cfg = new Configuration("foo");
        cfg.getProperties().put("a", true);
        assertTrue(cfg.getFeatureOrigins("a").isEmpty());
        assertNull(cfg.getConfigurationProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a")));
        assertNull(cfg.getProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a")));
        assertEquals(1, cfg.getFeatureOrigins("a", self).size());
        assertEquals(self, cfg.getFeatureOrigins("a", self).get(0));

        // single id
        final ArtifactId id = ArtifactId.parse("g:a:1");
        cfg.setFeatureOrigins("a", Collections.singletonList(id));
        assertEquals(1, cfg.getFeatureOrigins("a").size());
        assertEquals(id, cfg.getFeatureOrigins("a").get(0));
        assertEquals(1, cfg.getFeatureOrigins("a", self).size());
        assertEquals(id, cfg.getFeatureOrigins("a", self).get(0));

        assertNull(cfg.getConfigurationProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a")));
        assertNotNull(cfg.getProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a")));
        final String[] array = (String[]) cfg.getProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a"));
        assertArrayEquals(new String[] {id.toMvnId()}, array);

        // add another id
        final ArtifactId id2 = ArtifactId.parse("g:b:2");
        cfg.setFeatureOrigins("a", Arrays.asList(id, id2));
        assertEquals(2, cfg.getFeatureOrigins("a").size());
        assertEquals(id, cfg.getFeatureOrigins("a").get(0));
        assertEquals(id2, cfg.getFeatureOrigins("a").get(1));
        assertEquals(2, cfg.getFeatureOrigins("a", self).size());
        assertEquals(id, cfg.getFeatureOrigins("a", self).get(0));
        assertEquals(id2, cfg.getFeatureOrigins("a", self).get(1));

        assertNull(cfg.getConfigurationProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a")));
        assertNotNull(cfg.getProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a")));
        final String[] array2 = (String[]) cfg.getProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a"));
        assertArrayEquals(new String[] {id.toMvnId(), id2.toMvnId()}, array2);

        // remove
        cfg.getProperties().remove("a");
        cfg.setFeatureOrigins("a", null);
        assertTrue(cfg.getFeatureOrigins("a").isEmpty());
        assertNull(cfg.getConfigurationProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a")));
        assertNull(cfg.getProperties()
                .get(Configuration.PROP_FEATURE_ORIGINS.concat("-").concat("a")));
    }
}
