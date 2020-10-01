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

import org.junit.Test;

public class ArtifactResolutionTest {

    @Test public void testValidResolution() {
        final Artifact a = new Artifact(ArtifactId.parse("g:a:1"));

        assertNull(a.getMetadata().get(Artifact.KEY_RESOLUTION));
        assertEquals(ArtifactResolution.MANDATORY, a.getResolution());
        
        a.setResolution(ArtifactResolution.MANDATORY);
        assertEquals(ArtifactResolution.MANDATORY.name(), a.getMetadata().get(Artifact.KEY_RESOLUTION));
        assertEquals(ArtifactResolution.MANDATORY, a.getResolution());

        a.setResolution(ArtifactResolution.OPTIONAL);
        assertEquals(ArtifactResolution.OPTIONAL.name(), a.getMetadata().get(Artifact.KEY_RESOLUTION));
        assertEquals(ArtifactResolution.OPTIONAL, a.getResolution());

        a.setResolution(null);
        assertNull(a.getMetadata().get(Artifact.KEY_RESOLUTION));
        assertEquals(ArtifactResolution.MANDATORY, a.getResolution());
    }

    @Test(expected = IllegalArgumentException.class) 
    public void testInvalidResolution() {
        final Artifact a = new Artifact(ArtifactId.parse("g:a:1"));
        a.getMetadata().put(Artifact.KEY_RESOLUTION, "foo");

        a.getResolution();
    }
}