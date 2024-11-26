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
package org.apache.sling.feature.io.json;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Feature;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import static org.junit.Assert.fail;

/** Test utilities */
public class U {

    /** Read the feature from the provided resource
     */
    public static Feature readFeature(final String name) throws Exception {
        try (final Reader reader =
                new InputStreamReader(U.class.getResourceAsStream("/features/" + name + ".json"), "UTF-8")) {
            return FeatureJSONReader.read(reader, name);
        }
    }

    public static Configuration findConfiguration(final List<Configuration> cfgs, final String pid) {
        for (final Configuration c : cfgs) {
            if (pid.equals(c.getPid())) {
                return c;
            }
        }
        fail("Configuration not found " + pid);
        return null;
    }

    public static Capability findCapability(List<Capability> capabilities, final String namespace) {
        for (Capability capability : capabilities) {
            if (capability.getNamespace().equals(namespace)) {
                return capability;
            }
        }

        fail(String.format("No Capability with namespace '%s' found", namespace));
        return null;
    }

    public static Requirement findRequirement(List<Requirement> requirements, final String namespace) {
        for (Requirement requirement : requirements) {
            if (requirement.getNamespace().equals(namespace)) {
                return requirement;
            }
        }

        fail(String.format("No Requirement with namespace '%s' found", namespace));
        return null;
    }
}
