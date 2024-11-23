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
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class FeatureTest {

    @Test
    public void testVariableMetadata() {
        final ArtifactId self = ArtifactId.parse("self:self:1");
        final Feature f = new Feature(self);

        f.getVariables().put("a", "foo");
        final Map<String, Object> metadata = f.getVariableMetadata("a");
        assertNotNull(metadata);

        assertNull(f.getVariableMetadata("b"));
        f.getVariables().put("b", "bar");
        assertNotNull(f.getVariableMetadata("b"));

        metadata.put("hello", "world");

        assertEquals(1, f.getVariableMetadata("a").size());

        f.getVariables().remove("b");
        assertNull(f.getVariableMetadata("b"));
    }

    @Test
    public void testFrameworkPropertiesMetadata() {
        final ArtifactId self = ArtifactId.parse("self:self:1");
        final Feature f = new Feature(self);

        f.getFrameworkProperties().put("a", "foo");
        final Map<String, Object> metadata = f.getFrameworkPropertyMetadata("a");
        assertNotNull(metadata);

        assertNull(f.getFrameworkPropertyMetadata("b"));
        f.getFrameworkProperties().put("b", "bar");
        assertNotNull(f.getFrameworkPropertyMetadata("b"));

        metadata.put("hello", "world");

        assertEquals(1, f.getFrameworkPropertyMetadata("a").size());

        f.getFrameworkProperties().remove("b");
        assertNull(f.getFrameworkPropertyMetadata("b"));
    }

    @Test
    public void testVariableOrigins() {
        final ArtifactId self = ArtifactId.parse("self:self:1");
        final Feature f = new Feature(self);

        f.getVariables().put("a", "foo");
        final Map<String, Object> metadata = f.getVariableMetadata("a");

        assertNull(metadata.get(Artifact.KEY_FEATURE_ORIGINS));
        assertEquals(1, f.getFeatureOrigins(metadata).size());
        assertEquals(self, f.getFeatureOrigins(metadata).get(0));

        // single id
        final ArtifactId id = ArtifactId.parse("g:a:1");
        f.setFeatureOrigins(metadata, Collections.singletonList(id));
        assertEquals(1, f.getFeatureOrigins(metadata).size());
        assertEquals(id, f.getFeatureOrigins(metadata).get(0));

        assertNotNull(metadata.get(Artifact.KEY_FEATURE_ORIGINS));
        final String[] array = (String[]) metadata.get(Artifact.KEY_FEATURE_ORIGINS);
        assertArrayEquals(new String[] {id.toMvnId()}, array);

        // add another id
        final ArtifactId id2 = ArtifactId.parse("g:b:2");
        f.setFeatureOrigins(metadata, Arrays.asList(id, id2));
        assertEquals(2, f.getFeatureOrigins(metadata).size());
        assertEquals(id, f.getFeatureOrigins(metadata).get(0));
        assertEquals(id2, f.getFeatureOrigins(metadata).get(1));

        assertNotNull(metadata.get(Artifact.KEY_FEATURE_ORIGINS));
        final String[] array2 = (String[]) metadata.get(Artifact.KEY_FEATURE_ORIGINS);
        assertArrayEquals(new String[] {id.toMvnId(), id2.toMvnId()}, array2);

        assertSame(metadata, f.getVariableMetadata("a"));
    }

    @Test
    public void testFrameworkPropertiesOrigins() {
        final ArtifactId self = ArtifactId.parse("self:self:1");
        final Feature f = new Feature(self);

        f.getFrameworkProperties().put("a", "foo");
        final Map<String, Object> metadata = f.getFrameworkPropertyMetadata("a");

        assertNull(metadata.get(Artifact.KEY_FEATURE_ORIGINS));
        assertEquals(1, f.getFeatureOrigins(metadata).size());
        assertEquals(self, f.getFeatureOrigins(metadata).get(0));

        // single id
        final ArtifactId id = ArtifactId.parse("g:a:1");
        f.setFeatureOrigins(metadata, Collections.singletonList(id));
        assertEquals(1, f.getFeatureOrigins(metadata).size());
        assertEquals(id, f.getFeatureOrigins(metadata).get(0));

        assertNotNull(metadata.get(Artifact.KEY_FEATURE_ORIGINS));
        final String[] array = (String[]) metadata.get(Artifact.KEY_FEATURE_ORIGINS);
        assertArrayEquals(new String[] {id.toMvnId()}, array);

        // add another id
        final ArtifactId id2 = ArtifactId.parse("g:b:2");
        f.setFeatureOrigins(metadata, Arrays.asList(id, id2));
        assertEquals(2, f.getFeatureOrigins(metadata).size());
        assertEquals(id, f.getFeatureOrigins(metadata).get(0));
        assertEquals(id2, f.getFeatureOrigins(metadata).get(1));

        assertNotNull(metadata.get(Artifact.KEY_FEATURE_ORIGINS));
        final String[] array2 = (String[]) metadata.get(Artifact.KEY_FEATURE_ORIGINS);
        assertArrayEquals(new String[] {id.toMvnId(), id2.toMvnId()}, array2);

        assertSame(metadata, f.getFrameworkPropertyMetadata("a"));
    }
}
