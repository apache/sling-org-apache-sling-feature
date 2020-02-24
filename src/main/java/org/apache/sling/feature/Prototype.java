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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;

/**
 * A prototype is a blueprint of a feature with optional removals of
 * <ul>
 * <li>Configurations / configuration properties
 * <li>Bundles
 * <li>Framework properties
 * <li>Extensions or artifacts from extensions
 * <li>Capabilities
 * <li>Requirements
 * </ul>
 *
 * This class is not thread-safe.
 */
public class Prototype implements Comparable<Prototype> {

    private final ArtifactId id;

    private final List<String> configurationRemovals = new ArrayList<>();

    private final List<ArtifactId> bundleRemovals = new ArrayList<>();

    private final List<String> frameworkPropertiesRemovals = new ArrayList<>();

    private final List<String> extensionRemovals = new ArrayList<>();

    private final Map<String, List<ArtifactId>> artifactExtensionRemovals = new HashMap<>();

    private final List<MatchingRequirement> requirementRemovals = new ArrayList<>();

    private final List<Capability> capabilityRemovals = new ArrayList<>();

    /**
     * Construct a new Include.
     * @param id The id of the feature.
     * @throws IllegalArgumentException If id is {@code null}.
     */
    public Prototype(final ArtifactId id) {
        if ( id == null ) {
            throw new IllegalArgumentException("id must not be null.");
        }
        this.id = id;
    }

    /**
     * Get the id of the artifact.
     * @return The id.
     */
    public ArtifactId getId() {
        return this.id;
    }

    /**
     * Get the list of configuration removals The returned object is modifiable.
     * 
     * @return List of {@code PID}s.
     */
    public List<String> getConfigurationRemovals() {
        return configurationRemovals;
    }

    /**
     * Get the list of artifact removals The returned object is modifiable.
     * 
     * @return List of artifact ids.
     */
    public List<ArtifactId> getBundleRemovals() {
        return bundleRemovals;
    }

    /**
     * Get the list of framework property removals The returned object is
     * modifiable.
     * 
     * @return List of property names
     */
    public List<String> getFrameworkPropertiesRemovals() {
        return frameworkPropertiesRemovals;
    }

    /**
     * Get the list of extension removals The returned object is modifiable.
     * 
     * @return List of extension names
     */
    public List<String> getExtensionRemovals() {
        return extensionRemovals;
    }

    /**
     * Get the list of artifacts removed from extensions The returned object is
     * modifiable.
     * 
     * @return Map where the extension name is the key, and the value is a list of
     *         artifact ids
     */
    public Map<String, List<ArtifactId>> getArtifactExtensionRemovals() {
        return artifactExtensionRemovals;
    }

    /**
     * Get the list of requirement removals. The returned object is modifiable.
     * 
     * @return The list of requirements
     * @since 1.3
     */
    public List<MatchingRequirement> getRequirementRemovals() {
        return requirementRemovals;
    }

    /**
     * Get the list of capability removals. The returned object is modifiable.
     * 
     * @return The list of capabilities
     */
    public List<Capability> getCapabilityRemovals() {
        return capabilityRemovals;
    }

    @Override
    public int compareTo(final Prototype o) {
        return this.id.compareTo(o.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.id.equals(((Prototype)obj).id);
    }

    @Override
    public String toString() {
        return "Include [id=" + id.toMvnId()
                + "]";
    }
}
