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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ExecutionEnvironmentExtensionTest {

    @Test public void testNullFeature() {
        assertNull(ExecutionEnvironmentExtension.getExecutionEnvironmentExtension((Feature)null));
    }

    @Test public void testNullExtension() {
        assertNull(ExecutionEnvironmentExtension.getExecutionEnvironmentExtension((Extension)null));
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        assertNull(ExecutionEnvironmentExtension.getExecutionEnvironmentExtension(f));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongExtensionType() {
        final Feature f = new Feature(ArtifactId.parse("g:a:1.0"));
        final Extension e = new Extension(ExtensionType.TEXT, ExecutionEnvironmentExtension.EXTENSION_NAME, ExtensionState.OPTIONAL);
        f.getExtensions().add(e);
        ExecutionEnvironmentExtension.getExecutionEnvironmentExtension(f);
    }

    public void testNoFramework() {
        final Extension e = new Extension(ExtensionType.TEXT, ExecutionEnvironmentExtension.EXTENSION_NAME, ExtensionState.OPTIONAL);
        e.setJSON("{}");

        assertNull(ExecutionEnvironmentExtension.getExecutionEnvironmentExtension(e).getFramework());
    }

    public void testFrameworkAsString() {
        final Extension e = new Extension(ExtensionType.TEXT, ExecutionEnvironmentExtension.EXTENSION_NAME, ExtensionState.OPTIONAL);
        e.setJSON("{ \"framework\" : \"g:a:1\" }");

        final ExecutionEnvironmentExtension eee = ExecutionEnvironmentExtension.getExecutionEnvironmentExtension(e);
        assertNotNull(eee.getFramework());
        assertEquals(ArtifactId.parse("g:a:1"), eee.getFramework().getId());
    }

    public void testFrameworkAsObject() {
        final Extension e = new Extension(ExtensionType.TEXT, ExecutionEnvironmentExtension.EXTENSION_NAME, ExtensionState.OPTIONAL);
        e.setJSON("{ \"framework\" : { \"id\" : \"g:a:1\", \"p\" : \"v\" }");

        final ExecutionEnvironmentExtension eee = ExecutionEnvironmentExtension.getExecutionEnvironmentExtension(e);
        assertNotNull(eee.getFramework());
        assertEquals(ArtifactId.parse("g:a:1"), eee.getFramework().getId());
        assertEquals("v", eee.getFramework().getMetadata().get("p"));
    }
}
