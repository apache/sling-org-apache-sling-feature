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

import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

/**
 * A feature consists of
 * <ul>
 * <li>A unique id {@link ArtifactId}
 * <li>Bundles
 * <li>Configurations
 * <li>Framework properties
 * <li>Requirements and capabilities
 * <li>Prototype
 * <li>Extensions
 * </ul>
 *
 * This class is not thread-safe.
 */
public class Feature implements Comparable<Feature> {

    private final ArtifactId id;

    private final Bundles bundles = new Bundles();

    private final Configurations configurations = new Configurations();

    private final Map<String,String> frameworkProperties = new HashMap<>();

    private final List<MatchingRequirement> requirements = new ArrayList<>();

    private final List<Capability> capabilities = new ArrayList<>();

    private final Extensions extensions = new Extensions();

    private final Map<String,String> variables = new HashMap<>();

    /** The optional location. */
    private volatile String location;

    /** The optional title. */
    private volatile String title;

    /** The optional description. */
    private volatile String description;

    /** The optional vendor. */
    private volatile String vendor;

    /** The optional license. */
    private volatile String license;

    /** Flag indicating whether this is an assembled feature */
    private volatile boolean assembled = false;

    /** Flag indicating whether this is a final feature */
    private volatile boolean finalFlag = false;

    /** Flag indicating whether this is a complete feature */
    private volatile boolean completeFlag = false;

    /** The optional prototype. */
    private volatile Prototype prototype;

    /**
     * Construct a new feature.
     * @param id The id of the feature.
     * @throws IllegalArgumentException If id is {@code null}.
     */
    public Feature(final ArtifactId id) {
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
     * Get the location.
     * The location might be the location of the feature file or any other
     * means identifying where the object is defined.
     * @return The location or {@code null}.
     */
    public String getLocation() {
        return this.location;
    }

    /**
     * Set the location.
     * @param value The new location.
     */
    public void setLocation(final String value) {
        this.location = value;
    }

    /**
     * Get the bundles.
     * @return The bundles object.
     */
    public Bundles getBundles() {
        return this.bundles;
    }

    /**
     * Get the configurations.
     * The returned object is modifiable.
     * @return The configurations
     */
    public Configurations getConfigurations() {
        return this.configurations;
    }

    /**
     * Get the framework properties
     * The returned object is modifiable.
     * @return The framework properties
     */
    public Map<String,String> getFrameworkProperties() {
        return this.frameworkProperties;
    }

    /**
     * Get the list of requirements.
     * The returned object is modifiable.
     * @return The list of requirements
     */
    public List<MatchingRequirement> getRequirements() {
        return requirements;
    }

    /**
     * Get the list of capabilities.
     * The returned object is modifiable.
     * @return The list of capabilities
     */
    public List<Capability> getCapabilities() {
        return capabilities;
    }

    /**
     * Get the optional prototype feature.
     * @return The prototype feature or {@code null} if none.
     */
    public Prototype getPrototype() {
        return prototype;
    }

    /**
     * Set the optional prototype feature.
     * @param prototype The prototype feature or {@code null} if none.
     */
    public void setPrototype(Prototype prototype) {
        this.prototype = prototype;
    }

    /**
     * Get the list of extensions.
     * The returned object is modifiable.
     * @return The list of extensions
     */
    public Extensions getExtensions() {
        return this.extensions;
    }

    /**
     * Get the title
     * @return The title or {@code null}
     */
    public String getTitle() {
        return title;
    }

    /**
     * Set the title
     * @param title The title
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Get the description
     * @return The description or {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description
     * @param description The description
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Obtain the variables of the feature
     * @return The variables
     */
    public Map<String,String> getVariables() {
        return this.variables;
    }

    /**
     * Get the vendor
     * @return The vendor or {@code null}
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Set the vendor
     * @param vendor The vendor
     */
    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    /**
     * Get the license
     * @return The license or {@code null}
     */
    public String getLicense() {
        return license;
    }

    /**
     * Set the vendor
     * @param license The license
     */
    public void setLicense(final String license) {
        this.license = license;
    }

    /**
     * Check whether the feature is final. A final feature can't be a prototype
     * for other features.
     *
     * @return {@code true} if it is final, {@code false} otherwise
     */
    public boolean isFinal() {
        return finalFlag;
    }

    /**
     * Set the final flag
     *
     * @param flag The flag
     */
    public void setFinal(final boolean flag) {
        this.finalFlag = flag;
    }

    /**
     * Check whether the feature is complete. A complete feature has no external
     * dependencies and can run as-is.
     *
     * @return {@code true} if it is complete, {@code false} otherwise
     */
    public boolean isComplete() {
        return completeFlag;
    }

    /**
     * Set the complete flag
     *
     * @param flag The flag
     */
    public void setComplete(final boolean flag) {
        this.completeFlag = flag;
    }

    /**
     * Check whether the feature is already assembled
     *
     * @return {@code true} if it is assembled, {@code false} if it needs to be
     *         assembled
     */
    public boolean isAssembled() {
        return assembled;
    }

    /**
     * Set the assembled flag
     * @param flag The flag
     */
    public void setAssembled(final boolean flag) {
        this.assembled = flag;
    }

    /**
     * Create a copy of the feature
     * @return A copy of the feature
     */
    public Feature copy() {
        return copy(this.getId());
    }

    /**
     * Create a copy of the feature with a different id For contained items like
     * bundles, artifacts and configurations a copy is created as well.
     *
     * @param id The new id
     * @return The copy of the feature with the new id
     */
    public Feature copy(final ArtifactId id) {
        final Feature result = new Feature(id);

        // metadata
        result.setLocation(this.getLocation());
        result.setTitle(this.getTitle());
        result.setDescription(this.getDescription());
        result.setVendor(this.getVendor());
        result.setLicense(this.getLicense());
        result.setAssembled(this.isAssembled());
        result.setFinal(this.isFinal());
        result.setComplete(this.isComplete());

        // variables
        result.getVariables().putAll(this.getVariables());

        // bundles
        for(final Artifact b : this.getBundles()) {
            result.getBundles().add(b.copy(b.getId()));
        }

        // configurations
        for(final Configuration cfg : this.getConfigurations()) {
            result.getConfigurations().add(cfg.copy(cfg.getPid()));
        }

        // framework properties
        result.getFrameworkProperties().putAll(this.getFrameworkProperties());

        // requirements
        for (final MatchingRequirement r : this.getRequirements()) {
            final MatchingRequirement c = new MatchingRequirementImpl(null, r.getNamespace(), r.getDirectives(),
                    r.getAttributes());
            result.getRequirements().add(c);
        }

        // capabilities
        for(final Capability r : this.getCapabilities()) {
            final Capability c = new CapabilityImpl(null, r.getNamespace(), r.getDirectives(), r.getAttributes());
            result.getCapabilities().add(c);
        }

        // prototype
        final Prototype i = this.getPrototype();
        if (i != null) {
            final Prototype c = new Prototype(i.getId());

            c.getBundleRemovals().addAll(i.getBundleRemovals());
            c.getConfigurationRemovals().addAll(i.getConfigurationRemovals());
            c.getExtensionRemovals().addAll(i.getExtensionRemovals());
            c.getFrameworkPropertiesRemovals().addAll(i.getFrameworkPropertiesRemovals());
            c.getArtifactExtensionRemovals().putAll(i.getArtifactExtensionRemovals());

            result.setPrototype(c);
        }

        // extensions
        for(final Extension e : this.getExtensions()) {
            final Extension c = new Extension(e.getType(), e.getName(), e.getState());
            switch ( c.getType() ) {
                case ARTIFACTS : for(final Artifact a : e.getArtifacts()) {
                                    c.getArtifacts().add(a.copy(a.getId()));
                                 }
                                 break;
                case JSON : c.setJSON(e.getJSON());
                            break;
                case TEXT : c.setText(e.getText());
                            break;
            }
            result.getExtensions().add(c);
        }

        return result;
    }

    @Override
    public int compareTo(final Feature o) {
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
        return this.id.equals(((Feature)obj).id);
    }

    @Override
    public String toString() {
        return (this.isAssembled() ? "Assembled Feature" : "Feature") +
                " [id=" + this.getId().toMvnId()
                + ( this.getLocation() != null ? ", location=" + this.getLocation() : "")
                + "]";
    }

    private static class MatchingRequirementImpl extends RequirementImpl implements MatchingRequirement {

        public MatchingRequirementImpl(Resource res, String ns, Map<String, String> dirs, Map<String, Object> attrs) {
            super(res, ns, dirs, attrs);
        }
    }
}
