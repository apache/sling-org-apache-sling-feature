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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A container for configurations.
 *
 * This class is not thread-safe.
 */
public class Configurations extends ArrayList<Configuration> {

    private static final long serialVersionUID = -7243822886707856704L;

    /**
     * Get the configuration
     * @param pid The pid of the configuration
     * @return The configuration or {@code null}
     */
    public Configuration getConfiguration(final String pid) {
        for (final Configuration cfg : this) {
            if (pid.equals(cfg.getPid())) {
                return cfg;
            }
        }
        return null;
    }

    /**
     * Get all factory configurations matching the factory pid.
     * @param factoryPid The factory pid of the configurations
     * @return The configurations - the collection might be empty
     * @since 1.5
     */
    public Collection<Configuration> getFactoryConfigurations(final String factoryPid) {
        final List<Configuration> result = new ArrayList<>();
        for (final Configuration cfg : this) {
            if (factoryPid.equals(cfg.getFactoryPid())) {
                result.add(cfg);
            }
        }
        return result;
    }
}
