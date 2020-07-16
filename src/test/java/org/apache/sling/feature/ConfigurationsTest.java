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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ConfigurationsTest {

    @Test
    public void testGetFactoryConfigurations() {
        final Configurations cfgs = new Configurations();

        cfgs.add(new Configuration("factory~a"));
        cfgs.add(new Configuration("factory~b"));
        cfgs.add(new Configuration("pid"));

        final Collection<Configuration> result = cfgs.getFactoryConfigurations("factory");
        assertEquals(2, result.size());
        final Set<String> names = new HashSet<>();
        for(final Configuration c : result) {
            names.add(c.getName());
        }
        assertTrue(names.contains("a"));
        assertTrue(names.contains("b"));
    }
}
