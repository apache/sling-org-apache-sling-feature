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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class ArtifactTest {

    @Test
    public void testFeatureOrigins() {
        final ArtifactId self = ArtifactId.parse("self:self:1");

        final Artifact art = new Artifact(ArtifactId.parse("art:art:1"));
        assertEquals(0, art.getFeatureOrigins().length);
        assertNull(art.getMetadata().get(Artifact.KEY_FEATURE_ORIGINS));
        assertEquals(1, art.getFeatureOrigins(self).length);
        assertEquals(self, art.getFeatureOrigins(self)[0]);

        // single id
        final ArtifactId id = ArtifactId.parse("g:a:1");
        art.setFeatureOrigins(id);
        assertEquals(1, art.getFeatureOrigins().length);
        assertEquals(id, art.getFeatureOrigins()[0]);
        assertEquals(1, art.getFeatureOrigins(self).length);
        assertEquals(id, art.getFeatureOrigins(self)[0]);

        assertNotNull(art.getMetadata().get(Artifact.KEY_FEATURE_ORIGINS));
        assertEquals(id.toMvnId(), art.getMetadata().get(Artifact.KEY_FEATURE_ORIGINS));

        // add another id
        final ArtifactId id2 = ArtifactId.parse("g:b:2");
        art.setFeatureOrigins(id, id2);
        assertEquals(2, art.getFeatureOrigins().length);
        assertEquals(id, art.getFeatureOrigins()[0]);
        assertEquals(id2, art.getFeatureOrigins()[1]);
        assertEquals(2, art.getFeatureOrigins(self).length);
        assertEquals(id, art.getFeatureOrigins(self)[0]);
        assertEquals(id2, art.getFeatureOrigins(self)[1]);

        assertNotNull(art.getMetadata().get(Artifact.KEY_FEATURE_ORIGINS));
        assertEquals(
                id.toMvnId().concat(",").concat(id2.toMvnId()),
                art.getMetadata().get(Artifact.KEY_FEATURE_ORIGINS));
    }
}
