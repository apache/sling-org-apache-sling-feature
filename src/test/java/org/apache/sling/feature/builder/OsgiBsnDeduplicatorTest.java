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
package org.apache.sling.feature.builder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class OsgiBsnDeduplicatorTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** Same BSN+version, different Maven coords — typical cpconverter output. */
    @Test
    public void wildcardLatestKeepsTheBundleFromTheLastFeature() throws Exception {
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId platform = ArtifactId.parse("org.ow2.asm:asm-tree:9.9.1");
        final ArtifactId extension = ArtifactId.parse("org.objectweb.asm:tree:9.9.1");
        urls.put(platform, jarWithManifest("org.objectweb.asm.tree", "9.9.1"));
        urls.put(extension, jarWithManifest("org.objectweb.asm.tree", "9.9.1"));

        final Feature merged = assembleTwoFeatures(platform, extension, urls, ArtifactId.parse("*:*:LATEST"));

        assertEquals(
                "extension feature is last in the arglist, so it wins",
                Collections.singletonList(extension),
                idsOf(merged));
    }

    @Test
    public void wildcardFirstKeepsTheBundleFromTheFirstFeature() throws Exception {
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId platform = ArtifactId.parse("org.ow2.asm:asm-tree:9.9.1");
        final ArtifactId extension = ArtifactId.parse("org.objectweb.asm:tree:9.9.1");
        urls.put(platform, jarWithManifest("org.objectweb.asm.tree", "9.9.1"));
        urls.put(extension, jarWithManifest("org.objectweb.asm.tree", "9.9.1"));

        final Feature merged = assembleTwoFeatures(platform, extension, urls, ArtifactId.parse("*:*:FIRST"));

        assertEquals(Collections.singletonList(platform), idsOf(merged));
    }

    @Test
    public void wildcardHighestPicksTheHigherBundleVersion() throws Exception {
        // HIGHEST consults Bundle-Version, not arglist order.
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId v1 = ArtifactId.parse("g.one:a:1.0.0");
        final ArtifactId v2 = ArtifactId.parse("g.two:b:2.0.0");
        urls.put(v1, jarWithManifest("same.bsn", "1.0.0"));
        urls.put(v2, jarWithManifest("same.bsn", "2.0.0"));

        final Feature merged = assembleTwoFeatures(v1, v2, urls, ArtifactId.parse("*:*:HIGHEST"));

        assertEquals(Collections.singletonList(v2), idsOf(merged));
    }

    @Test
    public void wildcardAllKeepsBothBundles() throws Exception {
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId v1 = ArtifactId.parse("g.one:a:1.0.0");
        final ArtifactId v2 = ArtifactId.parse("g.two:b:2.0.0");
        urls.put(v1, jarWithManifest("same.bsn", "1.0.0"));
        urls.put(v2, jarWithManifest("same.bsn", "2.0.0"));

        final Feature merged = assembleTwoFeatures(v1, v2, urls, ArtifactId.parse("*:*:ALL"));

        assertEquals(Arrays.asList(v1, v2), idsOf(merged));
    }

    /** Symmetric with the existing GAV-clash "Artifact override rule required" failure. */
    @Test
    public void noOverrideThrowsOnCollision() throws Exception {
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId platform = ArtifactId.parse("org.ow2.asm:asm-tree:9.9.1");
        final ArtifactId extension = ArtifactId.parse("org.objectweb.asm:tree:9.9.1");
        urls.put(platform, jarWithManifest("org.objectweb.asm.tree", "9.9.1"));
        urls.put(extension, jarWithManifest("org.objectweb.asm.tree", "9.9.1"));

        try {
            assembleTwoFeatures(platform, extension, urls, null);
            fail("Expected IllegalStateException");
        } catch (final IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("org.objectweb.asm.tree"));
        }
    }

    /** Default off — pre-2.1.0 behaviour preserved. */
    @Test
    public void detectionDisabledKeepsBothBundles() throws Exception {
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId platform = ArtifactId.parse("org.ow2.asm:asm-tree:9.9.1");
        final ArtifactId extension = ArtifactId.parse("org.objectweb.asm:tree:9.9.1");
        urls.put(platform, jarWithManifest("org.objectweb.asm.tree", "9.9.1"));
        urls.put(extension, jarWithManifest("org.objectweb.asm.tree", "9.9.1"));

        final Feature platformFeature = new Feature(ArtifactId.parse("g.one:platform:1"));
        platformFeature.getBundles().add(new Artifact(platform));
        final Feature extensionFeature = new Feature(ArtifactId.parse("g.two:extension:1"));
        extensionFeature.getBundles().add(new Artifact(extension));

        final BuilderContext ctx = new BuilderContext(id -> null).setArtifactProvider(urls::get);
        // Note: setOsgiBsnCollisionDetection NOT called → default is disabled
        final Feature merged =
                FeatureBuilder.assemble(ArtifactId.parse("g.merged:merged:1"), ctx, platformFeature, extensionFeature);

        assertEquals(Arrays.asList(platform, extension), idsOf(merged));
    }

    /** BSN-only collision (different Bundle-Versions); */
    @Test
    public void differentBundleVersionsCollideToo() throws Exception {
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId v1 = ArtifactId.parse("g.one:a:1.0.0");
        final ArtifactId v2 = ArtifactId.parse("g.two:b:2.0.0");
        urls.put(v1, jarWithManifest("same.bsn", "1.0.0"));
        urls.put(v2, jarWithManifest("same.bsn", "2.0.0"));

        final Feature merged = assembleTwoFeatures(v1, v2, urls, ArtifactId.parse("*:*:LATEST"));

        assertEquals(Collections.singletonList(v2), idsOf(merged));
    }

    @Test
    public void singletonDirectiveOnBsnDoesNotSplitGroups() throws Exception {
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId platform = ArtifactId.parse("g.one:a:1.0.0");
        final ArtifactId extension = ArtifactId.parse("g.two:b:1.0.0");
        urls.put(platform, jarWithManifest("same.bsn;singleton:=true", "1.0.0"));
        urls.put(extension, jarWithManifest("same.bsn", "1.0.0"));

        final Feature merged = assembleTwoFeatures(platform, extension, urls, ArtifactId.parse("*:*:LATEST"));

        assertEquals(Collections.singletonList(extension), idsOf(merged));
    }

    /** Non-OSGi jars are not grouped, so detection is a no-op for them. */
    @Test
    public void plainJarsWithoutBsnArePassedThrough() throws Exception {
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId a = ArtifactId.parse("g.one:a:1");
        final ArtifactId b = ArtifactId.parse("g.two:b:1");
        urls.put(a, jarWithManifest(null, null));
        urls.put(b, jarWithManifest(null, null));

        final Feature merged = assembleTwoFeatures(a, b, urls, null);

        assertEquals(Arrays.asList(a, b), idsOf(merged));
    }

    @Test
    public void noArtifactProviderTreatsDetectionAsNoop() {
        final Feature platform = new Feature(ArtifactId.parse("g.one:platform:1"));
        platform.getBundles().add(new Artifact(ArtifactId.parse("org.ow2.asm:asm-tree:9.9.1")));
        final Feature extension = new Feature(ArtifactId.parse("g.two:extension:1"));
        extension.getBundles().add(new Artifact(ArtifactId.parse("org.objectweb.asm:tree:9.9.1")));

        // No setArtifactProvider → cannot read manifests → detection gracefully no-ops.
        final BuilderContext ctx = new BuilderContext(id -> null).setOsgiBsnCollisionDetection(true);
        final Feature merged = FeatureBuilder.assemble(ArtifactId.parse("g.merged:merged:1"), ctx, platform, extension);

        assertEquals(2, merged.getBundles().size());
    }

    @Test
    public void specificVersionOverrideSelectsByBundleVersion() throws Exception {
        final Map<ArtifactId, URL> urls = new HashMap<>();
        final ArtifactId v1 = ArtifactId.parse("g.one:a:1.0.0");
        final ArtifactId v2 = ArtifactId.parse("g.two:b:2.0.0");
        urls.put(v1, jarWithManifest("same.bsn", "1.0.0"));
        urls.put(v2, jarWithManifest("same.bsn", "2.0.0"));

        final Feature merged = assembleTwoFeatures(v1, v2, urls, ArtifactId.parse("*:*:2.0.0"));

        assertEquals(Collections.singletonList(v2), idsOf(merged));
    }

    // --- helpers ---

    private Feature assembleTwoFeatures(
            final ArtifactId platformBundle,
            final ArtifactId extensionBundle,
            final Map<ArtifactId, URL> urls,
            final ArtifactId wildcardOverride) {
        final Feature platform = new Feature(ArtifactId.parse("g.one:platform:1"));
        platform.getBundles().add(new Artifact(platformBundle));
        final Feature extension = new Feature(ArtifactId.parse("g.two:extension:1"));
        extension.getBundles().add(new Artifact(extensionBundle));

        final BuilderContext ctx =
                new BuilderContext(id -> null).setArtifactProvider(urls::get).setOsgiBsnCollisionDetection(true);
        if (wildcardOverride != null) {
            ctx.addArtifactsOverride(wildcardOverride);
        }
        return FeatureBuilder.assemble(ArtifactId.parse("g.merged:merged:1"), ctx, platform, extension);
    }

    private static List<ArtifactId> idsOf(final Feature f) {
        return f.getBundles().stream().map(Artifact::getId).collect(Collectors.toList());
    }

    /**
     * Build a tiny jar with the given OSGi headers and return its file:// URL.
     * Pass null bsn/version to produce a jar with a manifest but no OSGi headers.
     */
    private URL jarWithManifest(final String bsn, final String version) throws Exception {
        final Manifest manifest = new Manifest();
        final Attributes main = manifest.getMainAttributes();
        main.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (bsn != null) {
            main.putValue("Bundle-SymbolicName", bsn);
        }
        if (version != null) {
            main.putValue("Bundle-Version", version);
        }
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (final JarOutputStream jar = new JarOutputStream(buf, manifest)) {
            // empty body — manifest is what we care about
        }
        final File f = tmp.newFile();
        try (final FileOutputStream out = new FileOutputStream(f)) {
            out.write(buf.toByteArray());
        }
        return f.toURI().toURL();
    }
}
