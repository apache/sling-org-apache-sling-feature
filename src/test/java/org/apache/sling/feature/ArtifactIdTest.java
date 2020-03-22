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
package org.apache.sling.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.osgi.framework.Version;

public class ArtifactIdTest {

    private static final String G = "g";
    private static final String A = "a";

    @Test
    public void testSameVersion() {
        final String v1 = "1";
        final String v10 = "1.0";
        final String v100 = "1.0.0";

        final Version ve1 = new ArtifactId(G, A, v1, null, null).getOSGiVersion();
        final Version ve10 = new ArtifactId(G, A, v10, null, null).getOSGiVersion();
        final Version ve100 = new ArtifactId(G, A, v100, null, null).getOSGiVersion();

        assertEquals(0, ve1.compareTo(ve10));
        assertEquals(0, ve10.compareTo(ve100));
        assertEquals(0, ve1.compareTo(ve100));
        assertEquals(0, ve10.compareTo(ve1));
        assertEquals(0, ve100.compareTo(ve10));
        assertEquals(0, ve100.compareTo(ve1));
    }

    @Test
    public void testVersions() {
        final String v1 = "1";
        final String v20 = "2.0";
        final String v150 = "1.5.0";

        final Version ve1 = new ArtifactId(G, A, v1, null, null).getOSGiVersion();
        final Version ve20 = new ArtifactId(G, A, v20, null, null).getOSGiVersion();
        final Version ve150 = new ArtifactId(G, A, v150, null, null).getOSGiVersion();

        assertTrue(ve1.compareTo(ve20) < 0);
        assertTrue(ve20.compareTo(ve150) > 0);
        assertTrue(ve1.compareTo(ve150) < 0);
        assertTrue(ve20.compareTo(ve1) > 0);
        assertTrue(ve150.compareTo(ve20) < 0);
        assertTrue(ve150.compareTo(ve1) > 0);
    }

    @Test
    public void testSnapshotQualifier() {
        final Version v1 = new ArtifactId(G, A, "1", null, null).getOSGiVersion();
        final Version v1snapshot = new ArtifactId(G, A, "1-SNAPSHOT", null, null).getOSGiVersion();
        final Version v1a = new ArtifactId(G, A, "1-A", null, null).getOSGiVersion();

        // snapshot in OSGi is higher than the corresponding version
        assertTrue(v1.compareTo(v1snapshot) < 0);
        assertTrue(v1snapshot.compareTo(v1) > 0);

        // qualifier is higher than the version
        assertTrue(v1a.compareTo(v1) > 0);
        assertTrue(v1.compareTo(v1a) < 0);

        // qualifier in OSGi is lower than snapshot (A is lower than SNAPSHOT)
        assertTrue(v1a.compareTo(v1snapshot) < 0);
        assertTrue(v1snapshot.compareTo(v1a) > 0);
    }

    @Test
    public void testQualifiers() {
        final Version va = new ArtifactId(G, A, "1-A", null, null).getOSGiVersion();
        final Version vb = new ArtifactId(G, A, "1-B", null, null).getOSGiVersion();
        assertTrue(va.compareTo(vb) < 0);
        assertTrue(vb.compareTo(va) > 0);

        final Version vc = new ArtifactId(G, A, "0.11.14.1.0010", null, null).getOSGiVersion();
        assertEquals(0, vc.getMajor());
        assertEquals(11, vc.getMinor());
        assertEquals(14, vc.getMicro());
        assertEquals("1_0010", vc.getQualifier());
    }

    @Test
    public void testOSGiVersion() {
        final Version v = new ArtifactId(G, A, "1.5.2.SNAPSHOT", null, null).getOSGiVersion();
        assertEquals(1, v.getMajor());
        assertEquals(5, v.getMinor());
        assertEquals(2, v.getMicro());
        assertEquals("SNAPSHOT", v.getQualifier());
    }

    @Test
    public void testStrangeVersions() {
        final Version v = new ArtifactId(G, A, "3.0.3-20170712.062549-4", null, null).getOSGiVersion();
        assertEquals(3, v.getMajor());
        assertEquals(0, v.getMinor());
        assertEquals(3, v.getMicro());
        assertEquals("20170712_062549-4", v.getQualifier());
    }

    @Test public void testCoordinatesGAV() {
        final ArtifactId id = ArtifactId.fromMvnId("group.a:artifact.b:1.0");
        assertEquals("group.a", id.getGroupId());
        assertEquals("artifact.b", id.getArtifactId());
        assertEquals("1.0", id.getVersion());
        assertEquals("jar", id.getType());
        assertNull(id.getClassifier());
    }

    @Test public void testCoordinatesGAVP() {
        final ArtifactId id = ArtifactId.fromMvnId("group.a:artifact.b:zip:1.0");
        assertEquals("group.a", id.getGroupId());
        assertEquals("artifact.b", id.getArtifactId());
        assertEquals("1.0", id.getVersion());
        assertEquals("zip", id.getType());
        assertNull(id.getClassifier());
    }

    @Test public void testCoordinatesGAVPC() {
        final ArtifactId id = ArtifactId.fromMvnId("group.a:artifact.b:zip:foo:1.0");
        assertEquals("group.a", id.getGroupId());
        assertEquals("artifact.b", id.getArtifactId());
        assertEquals("1.0", id.getVersion());
        assertEquals("zip", id.getType());
        assertEquals("foo", id.getClassifier());
    }

    @Test public void testClassifierAndTypeToMvnId() {
        final ArtifactId id = new ArtifactId("group.a", "artifact.b", "1.0", "foo", "zip");
        assertEquals("group.a:artifact.b:zip:foo:1.0", id.toMvnId());
    }

    // --

    @Test public void testCoordinatesGAVfromUrl() {
        final ArtifactId id = ArtifactId.fromMvnUrl("mvn:group.a/artifact.b/1.0");
        assertEquals("group.a", id.getGroupId());
        assertEquals("artifact.b", id.getArtifactId());
        assertEquals("1.0", id.getVersion());
        assertEquals("jar", id.getType());
        assertNull(id.getClassifier());
    }

    @Test public void testCoordinatesGAVPfromUrl() {
        final ArtifactId id = ArtifactId.fromMvnUrl("mvn:group.a/artifact.b/1.0/zip");
        assertEquals("group.a", id.getGroupId());
        assertEquals("artifact.b", id.getArtifactId());
        assertEquals("1.0", id.getVersion());
        assertEquals("zip", id.getType());
        assertNull(id.getClassifier());
    }

    @Test public void testCoordinatesGAVPCfromUrl() {
        final ArtifactId id = ArtifactId.fromMvnUrl("mvn:group.a/artifact.b/1.0/zip/foo");
        assertEquals("group.a", id.getGroupId());
        assertEquals("artifact.b", id.getArtifactId());
        assertEquals("1.0", id.getVersion());
        assertEquals("zip", id.getType());
        assertEquals("foo", id.getClassifier());
    }

    @Test public void testClassifierAndTypeToMvnUlr() {
        final ArtifactId id = new ArtifactId("group.a", "artifact.b", "1.0", "foo", "zip");
        assertEquals("mvn:group.a/artifact.b/1.0/zip/foo", id.toMvnUrl());
    }

    @Test
    public void testToMvnPath() {
        final ArtifactId a1 = new ArtifactId("group.a", "artifact.b", "1.0", "foo", "zip");
        assertEquals("group/a/artifact.b/1.0/artifact.b-1.0-foo.zip", a1.toMvnPath());

        final ArtifactId a2 = new ArtifactId("group.a", "artifact.b", "1.0", null, "zip");
        assertEquals("group/a/artifact.b/1.0/artifact.b-1.0.zip", a2.toMvnPath());
    }

    @Test
    public void testToMvnName() {
        final ArtifactId a1 = new ArtifactId("group.a", "artifact.b", "1.0", "foo", "zip");
        assertEquals("artifact.b-1.0-foo.zip", a1.toMvnName());

        final ArtifactId a2 = new ArtifactId("group.a", "artifact.b", "1.0", null, "zip");
        assertEquals("artifact.b-1.0.zip", a2.toMvnName());
    }

    @Test
    public void testChangeVersion() {
        final ArtifactId a1 = new ArtifactId("group.a", "artifact.b", "1.0", "foo", "zip");
        final ArtifactId a2 = a1.changeVersion("3.0");
        assertTrue(a1.isSame(a2));
        assertEquals("3.0", a2.getVersion());

        try {
            a1.changeVersion(null);
            fail();
        } catch (IllegalArgumentException ignore) {
            // expected
        }
    }

    @Test
    public void testChangeClassifier() {
        final ArtifactId a1 = new ArtifactId("group.a", "artifact", "1.0", "foo", "zip");
        final ArtifactId a2 = a1.changeClassifier("bar");
        final ArtifactId a3 = a1.changeClassifier(null);

        // we use mvn path to compare all parts together in a single check
        assertEquals("group/a/artifact/1.0/artifact-1.0-bar.zip", a2.toMvnPath());
        assertEquals("group/a/artifact/1.0/artifact-1.0.zip", a3.toMvnPath());
    }

    @Test
    public void testChangeType() {
        final ArtifactId a1 = new ArtifactId("group.a", "artifact", "1.0", "foo", "zip");
        final ArtifactId a2 = a1.changeType("json");
        final ArtifactId a3 = a1.changeType(null);

        // we use mvn path to compare all parts together in a single check
        assertEquals("group/a/artifact/1.0/artifact-1.0-foo.json", a2.toMvnPath());
        assertEquals("group/a/artifact/1.0/artifact-1.0-foo.jar", a3.toMvnPath());
    }

    @Test
    public void testFromMvnPath() {
        final List<ArtifactId> ids = new ArrayList<>();
        ids.add(new ArtifactId("group", "artifact", "1.0", null, "zip"));
        ids.add(new ArtifactId("group", "artifact", "1.0", "foo", "zip"));
        ids.add(new ArtifactId("group", "artifact", "1.0", "a-classifier", "zip"));
        ids.add(new ArtifactId("group", "artifact", "1.alhpa", null, "zip"));
        ids.add(new ArtifactId("group", "my-artifact", "1.0", null, "zip"));
        ids.add(new ArtifactId("group", "my-artifact", "1.0", "a-classifier", "zip"));
        ids.add(new ArtifactId("group", "my-artifact", "1.alhpa", null, "zip"));

        ids.add(new ArtifactId("c.a.group", "artifact", "1.0", null, "zip"));
        ids.add(new ArtifactId("c.a.group", "artifact", "1.0", "foo", "zip"));
        ids.add(new ArtifactId("c.a.group", "artifact", "1.0", "a-classifier", "zip"));
        ids.add(new ArtifactId("c.a.group", "artifact", "1.alhpa", null, "zip"));
        ids.add(new ArtifactId("c.a.group", "my-artifact", "1.0", null, "zip"));
        ids.add(new ArtifactId("c.a.group", "my-artifact", "1.0", "a-classifier", "zip"));
        ids.add(new ArtifactId("c.a.group", "my-artifact", "1.alhpa", null, "zip"));

        for (final ArtifactId id : ids) {
            final String path = id.toMvnPath();
            final ArtifactId newId = ArtifactId.fromMvnPath(path);

            assertEquals(id, newId);
        }
    }

    @Test
    public void testParse() {
        final ArtifactId a1 = new ArtifactId("group.a", "artifact", "1.0", "foo", "zip");
        assertEquals(a1, ArtifactId.parse(a1.toMvnId()));
        assertEquals(a1, ArtifactId.parse(a1.toMvnUrl()));
    }
}
