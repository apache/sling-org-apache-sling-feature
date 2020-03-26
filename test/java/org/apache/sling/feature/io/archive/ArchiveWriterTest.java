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
package org.apache.sling.feature.io.archive;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.junit.Test;

public class ArchiveWriterTest {

    public static final String ARTIFACT = "/features/final.json";

    @Test
    public void testArchiveWrite() throws IOException {
        final Feature f = new Feature(ArtifactId.parse("g:f:1"));
        f.getBundles().add(new Artifact(ArtifactId.parse("g:a:2")));

        byte[] archive = null;
        final Set<ArtifactId> ids = new HashSet<>();
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ArchiveWriter.write(out, null, id -> {
                ids.add(id);
                return ArchiveWriterTest.class.getResource(ARTIFACT);
            }, f).finish();
            out.flush();
            archive = out.toByteArray();
        }
        assertEquals(1, ids.size());
        assertTrue(ids.contains(ArtifactId.parse("g:a:2")));

        // read "artifact"
        final byte[] artifactBytes;
        try (final InputStream is = ArchiveWriterTest.class.getResourceAsStream(ARTIFACT)) {
            artifactBytes = readFromStream(is);
        }

        final Set<ArtifactId> readIds = new HashSet<>();
        final Set<ArtifactId> readFeatureIds = new HashSet<>();

        final List<Feature> features = new ArrayList<>();
        try (final InputStream in = new ByteArrayInputStream(archive)) {
            features.addAll(ArchiveReader.read(in, (id, is) -> {

                // read contents
                byte[] read = readFromStream(is);

                // is feature?
                if ( id.equals(f.getId()) ) {
                    try ( final Reader reader = new StringReader(new String(read, StandardCharsets.UTF_8))) {
                        final Feature readFeature = FeatureJSONReader.read(reader, id.toString());
                        assertEquals(f.getId(), readFeature.getId());
                        readFeatureIds.add(f.getId());
                    }
                } else {
                    readIds.add(id);
                    assertEquals(artifactBytes.length, read.length);
                    assertArrayEquals(artifactBytes, read);
                }
            }));
        }

        assertEquals(1, readFeatureIds.size());
        assertTrue(readFeatureIds.contains(f.getId()));

        assertEquals(1, readIds.size());
        assertTrue(readIds.contains(ArtifactId.parse("g:a:2")));

        assertEquals(1, features.size());
        final Feature g = features.get(0);
        assertEquals(f.getId(), g.getId());
    }

    private byte[] readFromStream(final InputStream is) throws IOException {
        byte[] read;
        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];
            int l;
            while ((l = is.read(buf)) > 0) {
                baos.write(buf, 0, l);
            }
            baos.flush();
            read = baos.toByteArray();
        }
        return read;
    }
}
