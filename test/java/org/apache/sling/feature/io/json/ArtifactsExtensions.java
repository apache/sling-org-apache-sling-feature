package org.apache.sling.feature.io.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;

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
public class ArtifactsExtensions {

    public static void testReadArtifactsExtensions(Feature feature) {
        Extensions extensions = feature.getExtensions();

        assertEquals(2, extensions.size());

        Extension extension1 = extensions.getByName("my-extension1");
        assertNotNull(extension1);
        assertEquals(extension1.getName(), "my-extension1");
        assertEquals(extension1.getState(), ExtensionState.OPTIONAL);
        assertEquals(1, extension1.getArtifacts().size());

        ArtifactId artifactId1 = extension1.getArtifacts().get(0).getId();
        assertEquals(artifactId1.getGroupId(), "org.apache.sling");
        assertEquals(artifactId1.getArtifactId(), "my-extension1");
        assertEquals(artifactId1.getVersion(), "1.2.3");

        Extension extension2 = extensions.getByName("my-extension2");
        assertNotNull(extension2);
        assertEquals(extension2.getName(), "my-extension2");
        assertEquals(extension2.getState(), ExtensionState.REQUIRED);
        assertEquals(1, extension2.getArtifacts().size());

        ArtifactId artifactId2 = extension2.getArtifacts().get(0).getId();
        assertEquals(artifactId2.getGroupId(), "org.apache.sling");
        assertEquals(artifactId2.getArtifactId(), "my-extension2");
        assertEquals(artifactId2.getVersion(), "1.2.3");
    }
}
