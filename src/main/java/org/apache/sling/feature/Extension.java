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

import org.apache.sling.feature.builder.BuilderContext;

/**
 * An Extension can either be of type
 * <ul>
 * <li>Artifacts : it contains a list of artifacts
 * <li>Text : it contains text
 * <li>JSON : it contains a blob of JSON
 * </ul>
 *
 * This class is not thread-safe.
 *
 * @see ExtensionType
 */
public class Extension {

    /**
     * Common extension name to specify the repoinit part for Apache Sling. This
     * extension is of type {@link ExtensionType#TEXT} and is required.
     */
    public static final String EXTENSION_NAME_REPOINIT = "repoinit";

    /**
     * Common extension name to specify the content packages for Apache Sling. This
     * extension is of type {@link ExtensionType#ARTIFACTS} and is required.
     */
    public static final String EXTENSION_NAME_CONTENT_PACKAGES = "content-packages";

    /**
     * Extension name containing the assembled features as produced by
     * {@link org.apache.sling.feature.builder.FeatureBuilder#assemble(ArtifactId, BuilderContext, Feature...)}.
     * This extension is of type {@link ExtensionType#ARTIFACTS} and is optional.
     */
    public static final String EXTENSION_NAME_ASSEMBLED_FEATURES = "assembled-features";

    /** The extension type */
    private final ExtensionType type;

    /** The extension name. */
    private final String name;

    /** The list of artifacts (if type artifacts) */
    private final Artifacts artifacts;

    /** The text or json (if corresponding type) */
    private String text;

    /** Whether the artifact is required. */
    private final boolean required;

    /**
     * Create a new extension
     * @param t The type of the extension
     * @param name The name of the extension
     * @param required Whether the extension is required or optional
     * @throws IllegalArgumentException If name or t are {@code null}
     */
    public Extension(final ExtensionType t,
            final String name,
            final boolean required) {
        if ( t == null || name == null ) {
            throw new IllegalArgumentException("Argument must not be null");
        }
        this.type = t;
        this.name = name;
        this.required = required;
        if ( t == ExtensionType.ARTIFACTS ) {
            this.artifacts = new Artifacts();
        } else {
            this.artifacts = null;
        }
    }

    /**
     * Get the extension type
     * @return The type
     */
    public ExtensionType getType() {
        return this.type;
    }

    /**
     * Get the extension name
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Return whether the extension is required or optional
     * @return Return {@code true} if the extension is required.
     */
    public boolean isRequired() {
        return this.required;
    }

    /**
     * Return whether the extension is required or optional
     * @return Return {@code true} if the extension is optional.
     */
    public boolean isOptional() {
        return !this.isRequired();
    }

    /**
     * Get the text of the extension
     * @return The text
     * @throws IllegalStateException if the type is not {@code ExtensionType#TEXT}
     */
    public String getText() {
        if ( type != ExtensionType.TEXT ) {
            throw new IllegalStateException();
        }
        return text;
    }

    /**
     * Set the text of the extension
     * @param text The text
     * @throws IllegalStateException if the type is not {@code ExtensionType#TEXT}
     */
    public void setText(final String text) {
        if ( type != ExtensionType.TEXT ) {
            throw new IllegalStateException();
        }
        this.text = text;
    }

    /**
     * Get the JSON of the extension
     * @return The JSON
     * @throws IllegalStateException if the type is not {@code ExtensionType#JSON}
     */
    public String getJSON() {
        if ( type != ExtensionType.JSON ) {
            throw new IllegalStateException();
        }
        return text;
    }

    /**
     * Set the JSON of the extension
     * @param text The JSON
     * @throws IllegalStateException if the type is not {@code ExtensionType#JSON}
     */
    public void setJSON(String text) {
        if ( type != ExtensionType.JSON ) {
            throw new IllegalStateException();
        }
        this.text = text;
    }

    /**
     * Get the artifacts of the extension
     * @return The artifacts
     * @throws IllegalStateException if the type is not {@code ExtensionType#ARTIFACTS}
     */
    public Artifacts getArtifacts() {
        if ( type != ExtensionType.ARTIFACTS ) {
            throw new IllegalStateException();
        }
        return artifacts;
    }

    /**
     * Create a copy of the Extension
     * @return A copy of the Extension
     */
    public Extension copy() {
        Extension c = new Extension(type, name, required);
        switch(type) {
        case TEXT:
            c.setText(text);
            break;
        case JSON:
            c.setJSON(text);
            break;
        case ARTIFACTS:
            if (artifacts != null) {
                for (Artifact a : artifacts) {
                    c.getArtifacts().add(a.copy(a.getId()));
                }
            }
            break;
        }
        return c;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return name.equals(((Extension)obj).name);
    }

    @Override
    public String toString() {
        return "Extension [type=" + type + ", name=" + name + "]";
    }
}
