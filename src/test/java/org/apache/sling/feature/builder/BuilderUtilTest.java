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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.BuilderUtil.HandlerContextImpl;
import org.junit.Test;
import org.mockito.Mockito;

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

        List<ArtifactId> overrides = Arrays.asList(ArtifactId.parse("g:a:HIGHEST"), ArtifactId.parse("g:b:HIGHEST"));
        BuilderUtil.mergeArtifacts(target, source, orgFeat, overrides, null);

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

        List<ArtifactId> overrides = Arrays.asList(ArtifactId.parse("g:a:LATEST"), ArtifactId.parse("g:b:LATEST"));
        BuilderUtil.mergeArtifacts(target, source, orgFeat, overrides, null);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(3, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.1"));
        assertContains(result, 2, ArtifactId.parse("g/b/1.9"));
        assertContains(result, 3, ArtifactId.parse("g/c/2.5"));
    }

    @Test public void testMergeBundlesWithAliasNoRule() {
        final Bundles target = new Bundles();
        Artifact b = createBundle("g/b/2.0", 2);
        b.getMetadata().put("alias", "x:z:1,a:a");
        target.add(b);

        final Bundles source = new Bundles();
        source.add(createBundle("x/z/1.9", 2));

        final Feature orgFeat = new Feature(new ArtifactId("gid", "aid", "123", null, null));
        try {
            BuilderUtil.mergeArtifacts(target, source, orgFeat, Collections.emptyList(), null);
            fail("Expected exception ");
        } catch (Exception e) {
            String msg = e.getMessage();
            assertTrue(msg.contains("override rule required"));
            assertTrue(msg.contains("g:b:"));
            assertTrue(msg.contains("x:z:"));
        }
    }

    @Test public void testMergeBundlesWithAlias() {
        final Bundles target = new Bundles();
        Artifact b = createBundle("g/b/2.0", 2);
        b.getMetadata().put("alias", "x:z:1,a:a");
        target.add(b);

        final Bundles source = new Bundles();
        source.add(createBundle("x/z/1.9", 2));

        final Feature orgFeat = new Feature(new ArtifactId("gid", "aid", "123", null, null));
        List<ArtifactId> overrides = new ArrayList<>();
        overrides.add(ArtifactId.parse("x:z:HIGHEST"));
        BuilderUtil.mergeArtifacts(target, source, orgFeat, overrides, null);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(1, result.size());
        assertContains(result, 2, ArtifactId.parse("g/b/2.0"));
    }

    @Test public void testMergeBundlesDifferentStartlevel() {
        final Bundles target = new Bundles();

        target.add(createBundle("g/a/1.0", 1));

        final Bundles source = new Bundles();
        source.add(createBundle("g/a/1.1", 2));

        final Feature orgFeat = new Feature(new ArtifactId("gid", "aid", "123", null, null));
        List<ArtifactId> overrides = Arrays.asList(ArtifactId.parse("g:a:LATEST"), ArtifactId.parse("g:b:LATEST"));
        BuilderUtil.mergeArtifacts(target, source, orgFeat, overrides, null);

        final List<Map.Entry<Integer, Artifact>> result = getBundles(target);
        assertEquals(1, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.1"));
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
        BuilderUtil.mergeArtifacts(target, source, orgFeat, new ArrayList<>(), null);

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
        BuilderUtil.mergeArtifacts(target, source, orgFeat, new ArrayList<>(), null);

        final Bundles target2 = new Bundles();
        final Feature orgFeat2 = new Feature(new ArtifactId("g", "a", "1", null, null));
        BuilderUtil.mergeArtifacts(target2, target, orgFeat2, new ArrayList<>(), null);

        List<Entry<Integer, Artifact>> result = getBundles(target2);
        assertEquals(2, result.size());
        assertContains(result, 1, ArtifactId.parse("g/a/1.0"));
        assertContains(result, 1, ArtifactId.parse("g/b/1.0"));
    }

    @Test public void testMergeExtensions() {
        Extension target = new Extension(ExtensionType.JSON, "target", ExtensionState.REQUIRED);

        target.setJSON("[\"target1\", \"target2\"]");

        Extension source = new Extension(ExtensionType.JSON, "source", ExtensionState.REQUIRED);

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

            Extension tex = new Extension(ExtensionType.JSON, "foo", ExtensionState.OPTIONAL);
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
        Extension e = new Extension(ExtensionType.JSON, "foo", ExtensionState.OPTIONAL);
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
        Extension e = new Extension(ExtensionType.JSON, "foo", ExtensionState.OPTIONAL);
        e.setJSON("[{\"a\": 123}]");
        fs.getExtensions().add(e);
        Feature ft = new Feature(ArtifactId.fromMvnId("g:t:1"));
        Extension et = new Extension(ExtensionType.JSON, "foo", ExtensionState.OPTIONAL);
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
        Extension e = new Extension(ExtensionType.JSON, "foo", ExtensionState.OPTIONAL);
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
        Extension e = new Extension(ExtensionType.JSON, "foo", ExtensionState.OPTIONAL);
        e.setJSON("{\"a\": 123}");
        fs.getExtensions().add(e);
        Feature ft = new Feature(ArtifactId.fromMvnId("g:t:1"));
        Extension et = new Extension(ExtensionType.JSON, "foo", ExtensionState.OPTIONAL);
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
        List<ArtifactId> overrides = Arrays.asList(ArtifactId.parse("gid:aid2:1"), ArtifactId.parse("gid:aid:ALL"));
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
        List<ArtifactId> overrides = Collections.singletonList(ArtifactId.parse("gid:aid:1"));
        assertEquals(Collections.singletonList(a1), BuilderUtil.selectArtifactOverride(a1, a2, overrides));
    }

    @Test public void testSelectArtifactOverride2() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<ArtifactId> overrides = Collections.singletonList(ArtifactId.parse("gid:aid:2"));
        assertEquals(Collections.singletonList(a2), BuilderUtil.selectArtifactOverride(a1, a2, overrides));
    }

    @Test
    public void testSelectArtifactOverride3() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<ArtifactId> overrides = Collections.singletonList(ArtifactId.parse("gid:aid:3"));
        assertEquals(Collections.singletonList(new Artifact(ArtifactId.fromMvnId("gid:aid:3"))),
                BuilderUtil.selectArtifactOverride(a1, a2, overrides));
    }

    @Test
    public void testSelectArtifactOverrideWithoutClash() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        List<ArtifactId> overrides = Collections.singletonList(ArtifactId.parse("gid:aid:3"));
        assertEquals(Collections.singletonList(new Artifact(ArtifactId.fromMvnId("gid:aid:1"))),
                BuilderUtil.selectArtifactOverride(a1, a2, overrides));
    }

    @Test
    public void testSelectArtifactOverrideMulti() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<ArtifactId> overrides = Arrays.asList(ArtifactId.parse("gid:aid:2"), ArtifactId.parse("gid:aid:3"));
        assertEquals(Arrays.asList(a2),
                BuilderUtil.selectArtifactOverride(a1, a2, overrides));
    }

    @Test(expected=IllegalStateException.class)
    public void testSelectArtifactOverrideDifferentGroupID() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("aid:aid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<ArtifactId> overrides = Collections.singletonList(ArtifactId.parse("gid:aid:2"));
        BuilderUtil.selectArtifactOverride(a1, a2, overrides);
    }

    @Test(expected=IllegalStateException.class)
    public void testSelectArtifactOverrideDifferentArtifactID() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:gid:1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2"));
        List<ArtifactId> overrides = Collections.singletonList(ArtifactId.parse("gid:aid:2"));
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
        List<ArtifactId> overrides = Collections.singletonList(ArtifactId.parse("gid:aid:HIGHEST"));
        assertEquals(Collections.singletonList(a2), BuilderUtil.selectArtifactOverride(a1, a2, overrides));
        assertEquals(Collections.singletonList(a2), BuilderUtil.selectArtifactOverride(a2, a1, overrides));
    }

    @Test public void testSelectArtifactOverrideLatest() {
        Artifact a1 = new Artifact(ArtifactId.fromMvnId("gid:aid:1.1"));
        Artifact a2 = new Artifact(ArtifactId.fromMvnId("gid:aid:2.0.1"));
        List<ArtifactId> overrides = Collections.singletonList(ArtifactId.parse("gid:aid:LATEST"));
        assertEquals(Collections.singletonList(a2), BuilderUtil.selectArtifactOverride(a1, a2, overrides));
        assertEquals(Collections.singletonList(a1), BuilderUtil.selectArtifactOverride(a2, a1, overrides));
    }

    @Test public void testHandlerConfiguration() {
        Map<String, String> cfg1 = Collections.singletonMap("a", "b");
        Map<String, String> cfg2 = Collections.singletonMap("c", "d");
        Map<String, String> allCfg = Collections.singletonMap("f", "g");

        Map<String, Map<String, String>> cm = new HashMap<>();
        cm.put("all", allCfg);
        cm.put("OtherConfig", cfg1);
        cm.put("TestMergeHandler", cfg2);

        BuilderContext bc = Mockito.mock(BuilderContext.class);
        Mockito.when(bc.getHandlerConfigurations()).thenReturn(cm);
        TestMergeHandler mh = new TestMergeHandler();
        HandlerContextImpl hc = new BuilderUtil.HandlerContextImpl(bc, mh);
        Map<String, String> cfg = hc.getConfiguration();
        assertEquals(2, cfg.size());
        assertEquals("d", cfg.get("c"));
        assertEquals("g", cfg.get("f"));
    }

    @Test public void testHandlerConfiguration2() {
        Map<String, String> cfg1 = Collections.singletonMap("a", "b");
        Map<String, String> allCfg = Collections.singletonMap("f", "g");

        Map<String, Map<String, String>> cm = new HashMap<>();
        cm.put("none", allCfg);
        cm.put("OtherConfig", cfg1);

        BuilderContext bc = Mockito.mock(BuilderContext.class);
        Mockito.when(bc.getHandlerConfigurations()).thenReturn(cm);
        PostProcessHandler pph = Mockito.mock(PostProcessHandler.class);
        HandlerContextImpl hc = new BuilderUtil.HandlerContextImpl(bc, pph);
        Map<String, String> cfg = hc.getConfiguration();
        assertEquals(0, cfg.size());
    }

    @Test public void testMergeConfigurations() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration bar = new Configuration("bar");
        bar.getProperties().put("barKey", "valueBAR");
        source.add(bar);
        BuilderUtil.mergeConfigurations(target, source, Collections.emptyMap());
        assertEquals(2, target.size());
        assertEquals(target.getConfiguration("foo").getProperties(), foo.getProperties());
        assertEquals(target.getConfiguration("bar").getProperties(), bar.getProperties());
    }

    @Test public void testMergeConfigurationsCLASH() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        target.add(new Configuration("foo"));
        source.add(new Configuration("foo"));
        try {
            BuilderUtil.mergeConfigurations(target, source, Collections.emptyMap());
            fail();
        } catch (IllegalStateException ex) {

        }
    }

    @Test public void testMergeConfigurationsCLASHPROPERTY() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration foo2 = new Configuration("foo");
        foo2.getProperties().put("barKey", "valueBAR");
        source.add(foo2);
        BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("fo*", BuilderContext.CONFIG_FAIL_ON_PROPERTY_CLASH));

        assertEquals("valueFOO", target.getConfiguration("foo").getProperties().get("fooKey"));
        assertEquals("valueBAR", target.getConfiguration("foo").getProperties().get("barKey"));

        try {
            BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("fo*", BuilderContext.CONFIG_FAIL_ON_PROPERTY_CLASH));
            fail();
        } catch (IllegalStateException ex) {

        }
    }

    @Test public void testMergeConfigurationsUSEFIRST() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration foo2 = new Configuration("foo");
        foo2.getProperties().put("barKey", "valueBAR");
        source.add(foo2);
        BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("fo*", BuilderContext.CONFIG_USE_FIRST));

        assertEquals("valueFOO", target.getConfiguration("foo").getProperties().get("fooKey"));
        assertNull(target.getConfiguration("foo").getProperties().get("barKey"));
    }

    @Test public void testMergeConfigurationsUSELATEST() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration foo2 = new Configuration("foo");
        foo2.getProperties().put("barKey", "valueBAR");
        source.add(foo2);
        BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("fo*", BuilderContext.CONFIG_USE_LATEST));

        assertEquals("valueBAR", target.getConfiguration("foo").getProperties().get("barKey"));
        assertNull(target.getConfiguration("foo").getProperties().get("fooKey"));
    }

    @Test public void testMergeConfigurationsMERGELATEST() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration foo2 = new Configuration("foo");
        foo2.getProperties().put("fooKey", "valueBAR");
        source.add(foo2);
        BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("fo*", BuilderContext.CONFIG_MERGE_LATEST));

        assertEquals("valueBAR", target.getConfiguration("foo").getProperties().get("fooKey"));
    }

    @Test public void testMergeConfigurationsMERGEFIRST() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration foo2 = new Configuration("foo");
        foo2.getProperties().put("fooKey", "valueBAR");
        foo2.getProperties().put("barKey", "valueBAR");
        source.add(foo2);
        BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("fo*", BuilderContext.CONFIG_MERGE_FIRST));

        assertEquals("valueFOO", target.getConfiguration("foo").getProperties().get("fooKey"));
        assertEquals("valueBAR", target.getConfiguration("foo").getProperties().get("barKey"));
    }

    @Test public void testMergeConfigurationsFactory() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo~foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration bar = new Configuration("bar~bar");
        bar.getProperties().put("barKey", "valueBAR");
        source.add(bar);
        BuilderUtil.mergeConfigurations(target, source, Collections.emptyMap());
        assertEquals(2, target.size());
        assertEquals(target.getConfiguration("foo~foo").getProperties(), foo.getProperties());
        assertEquals(target.getConfiguration("bar~bar").getProperties(), bar.getProperties());
    }

    @Test public void testMergeConfigurationsCLASHFactory() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        target.add(new Configuration("foo~foo"));
        source.add(new Configuration("foo~foo"));
        try {
            BuilderUtil.mergeConfigurations(target, source, Collections.emptyMap());
            fail();
        } catch (IllegalStateException ex) {

        }
    }

    @Test public void testMergeConfigurationsCLASHPROPERTYFactory() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo~foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration foo2 = new Configuration("foo~foo");
        foo2.getProperties().put("barKey", "valueBAR");
        source.add(foo2);
        BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("fo*~f*", BuilderContext.CONFIG_FAIL_ON_PROPERTY_CLASH));

        assertEquals("valueFOO", target.getConfiguration("foo~foo").getProperties().get("fooKey"));
        assertEquals("valueBAR", target.getConfiguration("foo~foo").getProperties().get("barKey"));

        try {
            BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("fo*~fo*", BuilderContext.CONFIG_FAIL_ON_PROPERTY_CLASH));
            fail();
        } catch (IllegalStateException ex) {

        }
    }

    @Test public void testMergeConfigurationsUSELATESTFactory() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo~foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration foo2 = new Configuration("foo~foo");
        foo2.getProperties().put("barKey", "valueBAR");
        source.add(foo2);
        BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("fo*~f*", BuilderContext.CONFIG_USE_LATEST));

        assertEquals("valueBAR", target.getConfiguration("foo~foo").getProperties().get("barKey"));
        assertNull(target.getConfiguration("foo~foo").getProperties().get("fooKey"));
    }

    @Test public void testMergeConfigurationsMERGELATESTFactory() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo~foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration foo2 = new Configuration("foo~foo");
        foo2.getProperties().put("fooKey", "valueBAR");
        source.add(foo2);
        BuilderUtil.mergeConfigurations(target, source, Collections.singletonMap("foo~foo", BuilderContext.CONFIG_MERGE_LATEST));

        assertEquals("valueBAR", target.getConfiguration("foo~foo").getProperties().get("fooKey"));
    }

    @Test public void testMergeConfigurationsMixed() {
        Configurations target = new Configurations();
        Configurations source = new Configurations();
        Configuration foo = new Configuration("foo~foo");
        foo.getProperties().put("fooKey", "valueFOO");
        target.add(foo);
        Configuration foo4 = new Configuration("foo~foo");
        foo4.getProperties().put("fooKey", "valueFOO4");
        source.add(foo4);
        Configuration foo2 = new Configuration("foo");
        foo2.getProperties().put("fooKey", "valueBAR");
        source.add(foo2);
        Configuration foo3 = new Configuration("foo");
        foo2.getProperties().put("fooKey", "valueBAR2");
        source.add(foo3);
        Map<String, String> overrides = new HashMap<>();
        overrides.put("foo", BuilderContext.CONFIG_MERGE_LATEST);
        overrides.put("foo~foo", BuilderContext.CONFIG_USE_LATEST);
        BuilderUtil.mergeConfigurations(target, source, overrides);

        assertEquals("valueFOO4", target.getConfiguration("foo~foo").getProperties().get("fooKey"));
        assertEquals("valueBAR2", target.getConfiguration("foo").getProperties().get("fooKey"));
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

    @SafeVarargs
    public static Artifact createBundle(final String id, Map.Entry<String, String> ... metadata) {
        final Artifact a = new Artifact(ArtifactId.parse(id));

        for (Map.Entry<String, String> md : metadata) {
            a.getMetadata().put(md.getKey(), md.getValue());
        }

        return a;
    }
}
