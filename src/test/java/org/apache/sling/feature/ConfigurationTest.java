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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class ConfigurationTest {

    @Test
    public void testNullArgument() {
        try {
            new Configuration(null);
            fail();
        } catch (final IllegalArgumentException iae) {
            // expected
        }
    }

    @Test
    public void testFactoryPid() {
        final Configuration fc = new Configuration("org.apache.sling.factory~script");
        assertTrue(fc.isFactoryConfiguration());
        assertEquals("org.apache.sling.factory", fc.getFactoryPid());
        assertEquals("script", fc.getName());
        assertEquals("org.apache.sling.factory~script", fc.getPid());
    }

    @Test
    public void testPid() {
        final Configuration c = new Configuration("org.apache.sling.script");
        assertFalse(c.isFactoryConfiguration());
        assertNull(c.getFactoryPid());
        assertNull(c.getName());
        assertEquals("org.apache.sling.script", c.getPid());
    }

    @Test
    public void testStaticFactoryPidMethods() {
        final String factoryPid = "org.apache.sling.factory~script";
        final String pid = "org.apache.sling.script";

        assertTrue(Configuration.isFactoryConfiguration(factoryPid));
        assertEquals("org.apache.sling.factory", Configuration.getFactoryPid(factoryPid));
        assertEquals("script", Configuration.getName(factoryPid));

        assertFalse(Configuration.isFactoryConfiguration(pid));
        assertNull(Configuration.getFactoryPid(pid));
        assertNull(Configuration.getName(pid));
    }
}
