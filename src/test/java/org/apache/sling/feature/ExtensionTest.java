/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExtensionTest {
    @Test
    public void testCopyTextExtension() {
        Extension ex = new Extension(ExtensionType.TEXT, "t1", ExtensionState.OPTIONAL);
        ex.setText("foo");
        Extension ex2 = ex.copy();

        assertEquals(ex.getType(), ex2.getType());
        assertEquals("foo", ex2.getText());
    }

    @Test
    public void testCopyJSONExtension() {
        Extension ex = new Extension(ExtensionType.JSON, "t1", ExtensionState.OPTIONAL);
        ex.setJSON("[123]");
        Extension ex2 = ex.copy();

        assertEquals(ex.getType(), ex2.getType());
        assertEquals("[123]", ex2.getJSON());
    }

    @Test
    public void testCopyArtifactsExtension() {
        Extension ex = new Extension(ExtensionType.ARTIFACTS, "t1", ExtensionState.OPTIONAL);
        Artifact art = new Artifact(ArtifactId.fromMvnId("g:a:123"));
        art.getMetadata().put("test", "blah");
        ex.getArtifacts().add(art);
        Extension ex2 = ex.copy();

        assertEquals(ex.getType(), ex2.getType());
        assertEquals(1, ex2.getArtifacts().size());
        Artifact art2 = ex2.getArtifacts().iterator().next();
        assertEquals("g:a:123", art2.getId().toMvnId());
        assertEquals(1, art2.getMetadata().size());
        assertEquals("blah", art2.getMetadata().get("test"));
    }
}
