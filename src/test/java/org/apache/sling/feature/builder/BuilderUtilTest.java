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
import org.junit.Test;
import org.mockito.Mockito;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;

import static org.junit.Assert.assertEquals;
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

        List<String> overrides = Arrays.asList("g:a:HIGHEST", "g:b:HIGHEST");
        BuilderUtil.mergeBundles(target, source, orgFeat, overrides, null);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(3, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.1"));
        assertContains(result, 2, ArtifactId.parse("g/b/2.0"));
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

        List<String> overrides = Arrays.asList("g:a:LATEST", "g:b:LATEST");
        BuilderUtil.mergeBundles(target, source, orgFeat, overrides, null);

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
        List<String> overrides = Arrays.asList("g:a:LATEST", "g:b:LATEST");
        BuilderUtil.mergeBundles(target, source, orgFeat, overrides, null);

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
        BuilderUtil.mergeBundles(target, source, orgFeat, new ArrayList<>(), null);

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
        BuilderUtil.mergeBundles(target, source, orgFeat, new ArrayList<>(), null);

        final Bundles target2 = new Bundles();
        final Feature orgFeat2 = new Feature(new ArtifactId("g", "a", "1", null, null));
        BuilderUtil.mergeBundles(target2, target, orgFeat2, new ArrayList<>(), null);

        List<Entry<Integer, Artifact>> result = getBundles(target2);
        assertEquals(2, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.0"));
        assertContains(result, 1, ArtifactId.parse("g/b/1.0"));
    }

    @Test public void testMergeExtensions() {
        Extension target = new Extension(ExtensionType.JSON, "target", true);

        target.setJSON("[\"target1\", \"target2\"]");

        Extension source = new Extension(ExtensionType.JSON, "source", true);

        source.setJSON("[\"source1\",\"source2\"]");

        BuilderUtil.mergeExtensions(target, source, null, new ArrayList<>(), null);

        assertEquals(target.getJSON(), "[\"target1\",\"target2\",\"source1\",\"source2\"]");

    }

    @Test public void testMergeVariables() {
        Map<String,String> target = new HashMap<>();
        target.put("x", "327");

        Map<String,String> source = new HashMap<>();
        source.put("a", "b");

        BuilderUtil.mergeVariables(target, source, null);
        assertEquals(1, source.size());
        assertEquals("b", source.get("a"));

        assertEquals(2, target.size());
        assertEquals("b", target.get("a"));
        assertEquals("327", target.get("x"));
    }

    static class TestMergeHandler implements MergeHandler {
        @Override
        public boolean canMerge(Extension extension) {
            return "foo".equals(extension.getName());
        }

        @Override
        public void merge(HandlerContext context, Feature target, Feature source, Extension targetEx, Extension sourceEx) {
            JsonObject tobj = null;
            if (targetEx != null) {
                tobj = Json.createReader(new StringReader(targetEx.getJSON())).readObject();
            }
            JsonObject sobj = Json.createReader(new StringReader(sourceEx.getJSON())).readObject();

            JsonArrayBuilder ja = Json.createArrayBuilder();
            if (tobj != null && tobj.containsKey("org")) {
                JsonArray a = tobj.getJsonArray("org");
                for (JsonValue o : a) {
                    if (o instanceof JsonString) {
                        ja.add(((JsonString) o).getString());
                    }
                }
            }
            ja.add(source.getId().toMvnId());

            StringWriter sw = new StringWriter();
            JsonGenerator gen = Json.createGenerator(sw);
            gen.writeStartObject();
            copyJsonObject(sobj, gen);
            gen.write("org", ja.build());

            JsonObjectBuilder jo = Json.createObjectBuilder();
            boolean hasCfg = false;
            for (Map.Entry<String, String> entry : context.getConfiguration().entrySet()) {
                jo.add(entry.getKey(), entry.getValue());
                hasCfg = true;
            }
            if (hasCfg)
                gen.write("cfg", jo.build());

            gen.writeEnd();
            gen.close();

            Extension tex = new Extension(ExtensionType.JSON, "foo", false);
            tex.setJSON(sw.toString());
            target.getExtensions().remove(targetEx);
            target.getExtensions().add(tex);
        }

    }

    static void copyJsonObject(JsonObject obj, JsonGenerator gen, String ... exclusions) {
        for (Map.Entry<String, JsonValue> entry : obj.entrySet()) {
            if (Arrays.asList(exclusions).contains(entry.getKey()))
                continue;

            gen.write(entry.getKey(), entry.getValue());
        }
    }

    @Test public void testMergeDefaultExtensionsFirst() {
        FeatureProvider fp = Mockito.mock(FeatureProvider.class);
        BuilderContext ctx = new BuilderContext(fp);
        Feature fs = new Feature(ArtifactId.fromMvnId("g:s:1"));
        Extension e = new Extension(ExtensionType.JSON, "foo", false);
        e.setJSON("{\"a\": 123}");
        fs.getExtensions().add(e);
        Feature ft = new Feature(ArtifactId.fromMvnId("g:t:1"));

        assertEquals("Precondition", 0, ft.getExtensions().size());
        BuilderUtil.mergeExtensions(ft, fs, ctx, new ArrayList<>(), null);
        assertEquals(1, ft.getExtensions().size());

        Extension actual = ft.getExtensions().get(0);
        String expected = "{\"a\": 123}";

        JsonReader ar = Json.createReader(new StringReader(actual.getJSON()));
        JsonReader er = Json.createReader(new StringReader(expected));
        assertEquals(er.readObject(), ar.readObject());
    }

    @Test public void testMergeDefaultExtensionsSecond() {
        FeatureProvider fp = Mockito.mock(FeatureProvider.class);
        BuilderContext ctx = new BuilderContext(fp);
        Feature fs = new Feature(ArtifactId.fromMvnId("g:s:1"));
        Extension e = new Extension(ExtensionType.JSON, "foo", false);
        e.setJSON("[{\"a\": 123}]");
        fs.getExtensions().add(e);
        Feature ft = new Feature(ArtifactId.fromMvnId("g:t:1"));
        Extension et = new Extension(ExtensionType.JSON, "foo", false);
        et.setJSON("[{\"a\": 456}]");
        ft.getExtensions().add(et);

        assertEquals("Precondition", 1, ft.getExtensions().size());
        BuilderUtil.mergeExtensions(ft, fs, ctx, new ArrayList<>(), null);
        assertEquals(1, ft.getExtensions().size());

        Extension actual = ft.getExtensions().get(0);
        String expected = "[{\"a\": 456}, {\"a\": 123}]";

        JsonReader ar = Json.createReader(new StringReader(actual.getJSON()));
        JsonReader er = Json.createReader(new StringReader(expected));
        assertEquals(er.readArray(), ar.readArray());
    }

    @Test public void testMergeCustomExtensionsFirst() {
        Map<String,String> m = new HashMap<>();
        m.put("abc", "def");
        m.put("hij", "klm");

        FeatureProvider fp = Mockito.mock(FeatureProvider.class);
        BuilderContext ctx = new BuilderContext(fp);
        ctx.setHandlerConfiguration("TestMergeHandler", m);
        ctx.addMergeExtensions(new TestMergeHandler());
        Feature fs = new Feature(ArtifactId.fromMvnId("g:s:1"));
        Extension e = new Extension(ExtensionType.JSON, "foo", false);
        e.setJSON("{\"a\": 123}");
        fs.getExtensions().add(e);
        Feature ft = new Feature(ArtifactId.fromMvnId("g:t:1"));

        assertEquals("Precondition", 0, ft.getExtensions().size());
        BuilderUtil.mergeExtensions(ft, fs, ctx, new ArrayList<>(), null);
        assertEquals(1, ft.getExtensions().size());

        Extension actual = ft.getExtensions().get(0);
        String expected = "{\"a\": 123, \"org\": [\"g:s:1\"], \"cfg\":{\"abc\":\"def\",\"hij\":\"klm\"}}";

        JsonReader ar = Json.createReader(new StringReader(actual.getJSON()));
        JsonReader er = Json.createReader(new StringReader(expected));
        assertEquals(er.readObject(), ar.readObject());
    }

    @Test public void testMergeCustomExtensionsSecond() {
        FeatureProvider fp = Mockito.mock(FeatureProvider.class);
        BuilderContext ctx = new BuilderContext(fp);
        ctx.addMergeExtensions(new TestMergeHandler());
        Feature fs = new Feature(ArtifactId.fromMvnId("g:s:1"));
        Extension e = new Extension(ExtensionType.JSON, "foo", false);
        e.setJSON("{\"a\": 123}");
        fs.getExtensions().add(e);
        Feature ft = new Feature(ArtifactId.fromMvnId("g:t:1"));
        Extension et = new Extension(ExtensionType.JSON, "foo", false);
        et.setJSON("{\"a\": 123, \"org\": [\"g:s2:2\"]}");
        ft.getExtensions().add(et);

        assertEquals("Precondition", 1, ft.getExtensions().size());
        BuilderUtil.mergeExtensions(ft, fs, ctx, new ArrayList<>(), null);
        assertEquals(1, ft.getExtensions().size());

        Extension actual = ft.getExtensions().get(0);
        String expected = "{\"a\": 123, \"org\": [\"g:s2:2\", \"g:s:1\"]}";

        JsonReader ar = Json.createReader(new StringReader(actual.getJSON()));
        JsonReader er = Json.createReader(new StringReader(expected));
        assertEquals(er.readObject(), ar.readObject());
    }

    @Test public void testSelectArtifactOverrideAll() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<String> overrides = Arrays.asList("gid:aid2:1", "gid:aid:ALL ");
        assertEquals(Arrays.asList(a1, a2), BuilderUtil.selectArtifactOverride(a1, a2, overrides));
    }

    @Test public void testSelectArtifactOverrideIdenticalNeedsNoRule() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        assertEquals(Collections.singletonList(a1), BuilderUtil.selectArtifactOverride(a1, a2, Collections.emptyList()));
    }

    @Test public void testSelectArtifactOverride1() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<String> overrides = Collections.singletonList("gid:aid:1");
        assertEquals(Collections.singletonList(a1), BuilderUtil.selectArtifactOverride(a1, a2, overrides));
    }

    @Test public void testSelectArtifactOverride2() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<String> overrides = Collections.singletonList("gid:aid:2");
        assertEquals(Collections.singletonList(a2), BuilderUtil.selectArtifactOverride(a1, a2, overrides));
    }

    @Test(expected=IllegalStateException.class)
    public void testSelectArtifactOverride3() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<String> overrides = Collections.singletonList("gid:aid:3");
        BuilderUtil.selectArtifactOverride(a1, a2, overrides);
    }

    @Test(expected=IllegalStateException.class)
    public void testSelectArtifactOverrideDifferentGroupID() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("aid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<String> overrides = Collections.singletonList("gid:aid:2");
        BuilderUtil.selectArtifactOverride(a1, a2, overrides);
    }

    @Test(expected=IllegalStateException.class)
    public void testSelectArtifactOverrideDifferentArtifactID() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:gid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<String> overrides = Collections.singletonList("gid:aid:2");
        BuilderUtil.selectArtifactOverride(a1, a2, overrides);
    }

    @Test(expected=IllegalStateException.class)
    public void testSelectArtifactOverrideDifferentNoRule() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        BuilderUtil.selectArtifactOverride(a1, a2, Collections.emptyList());
    }

    @Test public void testSelectArtifactOverrideHigest() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1.1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2.0.1"));
        List<String> overrides = Collections.singletonList("gid:aid:HIGHEST");
        assertEquals(Collections.singletonList(a2), BuilderUtil.selectArtifactOverride(a1, a2, overrides));
        assertEquals(Collections.singletonList(a2), BuilderUtil.selectArtifactOverride(a2, a1, overrides));
    }

    @Test public void testSelectArtifactOverrideLatest() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1.1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2.0.1"));
        List<String> overrides = Collections.singletonList("gid:aid:LATEST");
        assertEquals(Collections.singletonList(a2), BuilderUtil.selectArtifactOverride(a1, a2, overrides));
        assertEquals(Collections.singletonList(a1), BuilderUtil.selectArtifactOverride(a2, a1, overrides));
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
