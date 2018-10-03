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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.KeyValueMap;
import org.apache.sling.feature.builder.BuilderUtil.ArtifactMerge;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class BuilderUtilTest {

    private List<Map.Entry<Integer, Artifact>> getBundles(final Bundles f) {
        final List<Map.Entry<Integer, Artifact>> result = new ArrayList<>();
        for(final Map.Entry<Integer, List<Artifact>> entry : f.getBundlesByStartOrder().entrySet()) {
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

    private int assertContains(final List<Map.Entry<Integer, Artifact>> bundles,
            final int level, final ArtifactId id) {
        for(int i=0; i < bundles.size(); i++) {
            Map.Entry<Integer, Artifact> entry = bundles.get(i);
            if ( entry.getKey().intValue() == level
                 && entry.getValue().getId().equals(id) ) {
                return i;
            }
        }
        fail(id.toMvnId());
        return -1;
    }

    @Test public void testMergeBundlesWithAlgHighest() {
        final Bundles target = new Bundles();

        target.add(createBundle("g/a/1.0", 1));
        target.add(createBundle("g/b/2.0", 2));
        target.add(createBundle("g/c/2.5", 3));

        final Bundles source = new Bundles();
        source.add(createBundle("g/a/1.1", 1));
        source.add(createBundle("g/b/1.9", 2));
        source.add(createBundle("g/c/2.5", 3));

        final Feature orgFeat = new Feature(new ArtifactId("gid", "aid", "123", null, null));

        BuilderUtil.mergeBundles(target, source, orgFeat, ArtifactMerge.HIGHEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(3, result.size());
        int i = assertContains(result, 1, ArtifactId.parse("g/a/1.1"));
        Map.Entry<Integer, Artifact> a1 = result.get(i);
        assertEquals("gid:aid:123", a1.getValue().getMetadata().get("org-feature"));

        int i2 = assertContains(result, 2, ArtifactId.parse("g/b/2.0"));
        Map.Entry<Integer, Artifact> a2 = result.get(i2);
        assertNull(a2.getValue().getMetadata().get("org-feature"));
        assertContains(result, 3, ArtifactId.parse("g/c/2.5"));
    }

    @Test public void testMergeBundlesWithAlgLatest() {
        final Bundles target = new Bundles();

        target.add(createBundle("g/a/1.0", 1));
        target.add(createBundle("g/b/2.0", 2));
        target.add(createBundle("g/c/2.5", 3));

        final Bundles source = new Bundles();
        source.add(createBundle("g/a/1.1", 1));
        source.add(createBundle("g/b/1.9", 2));
        source.add(createBundle("g/c/2.5", 3));

        final Feature orgFeat = new Feature(new ArtifactId("gid", "aid", "123", null, null));

        BuilderUtil.mergeBundles(target, source, orgFeat, ArtifactMerge.LATEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(3, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.1"));
        assertContains(result, 2, ArtifactId.parse("g/b/1.9"));
        assertContains(result, 3, ArtifactId.parse("g/c/2.5"));
    }

    @Test public void testMergeBundlesDifferentStartlevel() {
        final Bundles target = new Bundles();

        target.add(createBundle("g/a/1.0", 1));

        final Bundles source = new Bundles();
        source.add(createBundle("g/a/1.1", 2));

        final Feature orgFeat = new Feature(new ArtifactId("gid", "aid", "123", null, null));
        BuilderUtil.mergeBundles(target, source, orgFeat, ArtifactMerge.LATEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(1, result.size());
        assertContains(result, 2, ArtifactId.parse("g/a/1.1"));
    }

    @Test public void testMergeBundles() {
        final Bundles target = new Bundles();

        target.add(createBundle("g/a/1.0", 1));
        target.add(createBundle("g/b/2.0", 2));
        target.add(createBundle("g/c/2.5", 3));

        final Bundles source = new Bundles();
        source.add(createBundle("g/d/1.1", 1));
        source.add(createBundle("g/e/1.9", 2));
        source.add(createBundle("g/f/2.5", 3));

        final Feature orgFeat = new Feature(new ArtifactId("gid", "aid", "123", null, null));
        BuilderUtil.mergeBundles(target, source, orgFeat, ArtifactMerge.LATEST);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(6, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.0"));
        assertContains(result, 2, ArtifactId.parse("g/b/2.0"));
        assertContains(result, 3, ArtifactId.parse("g/c/2.5"));
        assertContains(result, 1, ArtifactId.parse("g/d/1.1"));
        assertContains(result, 2, ArtifactId.parse("g/e/1.9"));
        assertContains(result, 3, ArtifactId.parse("g/f/2.5"));
    }

    @Test public void testMultipleTimes() {
        final Bundles target = new Bundles();
        target.add(createBundle("g/a/1.0", 1));

        final Bundles source = new Bundles();
        source.add(createBundle("g/b/1.0", 1));

        final Feature orgFeat = new Feature(new ArtifactId("gid", "aid", "123", "c1", "slingfeature"));
        BuilderUtil.mergeBundles(target, source, orgFeat, ArtifactMerge.LATEST);

        final Bundles target2 = new Bundles();
        final Feature orgFeat2 = new Feature(new ArtifactId("g", "a", "1", null, null));
        BuilderUtil.mergeBundles(target2, target, orgFeat2, ArtifactMerge.LATEST);

        List<Entry<Integer, Artifact>> result = getBundles(target2);
        assertEquals(2, result.size());
        int i1 = assertContains(result, 1, ArtifactId.parse("g/a/1.0"));
        assertEquals("g:a:1", result.get(i1).getValue().getMetadata().get("org-feature"));
        int i2 = assertContains(result, 1, ArtifactId.parse("g/b/1.0"));
        assertEquals("gid:aid:slingfeature:c1:123", result.get(i2).getValue().getMetadata().get("org-feature"));
    }

    @Test public void testMergeExtensions() {
        Extension target = new Extension(ExtensionType.JSON, "target", true);

        target.setJSON("[\"target1\", \"target2\"]");

        Extension source = new Extension(ExtensionType.JSON, "source", true);

        source.setJSON("[\"source1\",\"source2\"]");

        BuilderUtil.mergeExtensions(target, source, ArtifactMerge.HIGHEST);

        assertEquals(target.getJSON(), "[\"target1\",\"target2\",\"source1\",\"source2\"]");

    }

    @Test public void testMergeVariables() {
        KeyValueMap target = new KeyValueMap();
        target.put("x", "327");

        KeyValueMap source = new KeyValueMap();
        source.put("a", "b");

        BuilderUtil.mergeVariables(target, source, null);
        assertEquals(1, source.size());
        assertEquals("b", source.get("a"));

        assertEquals(2, target.size());
        assertEquals("b", target.get("a"));
        assertEquals("327", target.get("x"));
    }

    @SafeVarargs
    public static Artifact createBundle(final String id, final int startOrder, Map.Entry<String, String> ... metadata) {
        final Artifact a = new Artifact(ArtifactId.parse(id));
        a.getMetadata().put(Artifact.KEY_START_ORDER, String.valueOf(startOrder));

        for (Map.Entry<String, String> md : metadata) {
            a.getMetadata().put(md.getKey(), md.getValue());
        }

        return a;
    }
}
