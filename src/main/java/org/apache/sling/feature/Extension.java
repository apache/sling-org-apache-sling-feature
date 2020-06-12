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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonWriter;

import org.apache.sling.feature.builder.BuilderContext;

/**
 * An Extension can either be of type
 * <ul>
 * <li>Artifacts : it contains a list of artifacts
 * <li>Text : it contains text
 * <li>JSON : it contains a blob of JSON
 * </ul>
 * <p>
 * An extension can be in one of these states
 * <ul>
 * <li>Required : Required extensions need to be processed by tooling
 * <li>Optional : Optional extensions might be processed by tooling, for example
 * they might contain environment specific parts
 * <li>Transient: Transient extensions are cache like extensions where tooling
 * can store additional information to avoid reprocessing of down stream
 * tooling. However such tooling must work without the transient extension being
 * available.
 * </ul>
 * <p>
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
    private volatile String text;

    /** The json structure (if corresponding type) */
    private volatile JsonStructure json;

    /** Extension state. */
    private final ExtensionState state;

    /**
     * Create a new extension
     *
     * @param t        The type of the extension
     * @param name     The name of the extension
     * @param required Whether the extension is required or optional
     * @throws IllegalArgumentException If name or t are {@code null}
     * @deprecated Use {@link #Extension(ExtensionType, String, ExtensionState)}
     */
    @Deprecated
    public Extension(final ExtensionType t,
            final String name,
            final boolean required) {
        this(t, name, required ? ExtensionState.REQUIRED : ExtensionState.OPTIONAL);
    }

    /**
     * Create a new extension
     *
     * @param type  The type of the extension
     * @param name  The name of the extension
     * @param state The state of the extension
     * @throws IllegalArgumentException If name, type or state is {@code null}
     * @since 1.1
     */
    public Extension(final ExtensionType type, final String name, final ExtensionState state) {
        if (type == null || name == null || state == null) {
            throw new IllegalArgumentException("Argument must not be null");
        }
        this.type = type;
        this.name = name;
        this.state = state;
        if (type == ExtensionType.ARTIFACTS) {
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
     * Get the extension state
     *
     * @return The state
     * @since 1.1
     */
    public ExtensionState getState() {
        return this.state;
    }

    /**
     * Get the extension name
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Return whether the extension is required or optional
     *
     * @return Return {@code true} if the extension is required.
     * @deprecated Use {@link #getState()}
     */
    @Deprecated
    public boolean isRequired() {
        return this.state == ExtensionState.REQUIRED;
    }

    /**
     * Return whether the extension is required or optional
     *
     * @return Return {@code true} if the extension is optional.
     * @deprecated Use {@link #getState()}
     */
    @Deprecated
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
     *
     * @return The JSON or {@code null}
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
     *
     * @param text The JSON
     * @throws IllegalStateException    if the type is not
     *                                  {@code ExtensionType#JSON}
     * @throws IllegalArgumentException If the structure is not valid
     */
    public void setJSON(String text) {
        if ( type != ExtensionType.JSON ) {
            throw new IllegalStateException();
        }
        this.text = text;
        try (final StringReader reader = new StringReader(text)) {
            this.json = Json.createReader(reader).read();
        }
    }

    /**
     * Get the JSON structure of the extension
     *
     * @return The JSON object or {@code null}
     * @throws IllegalStateException if the type is not {@code ExtensionType#JSON}
     * @since 1.1
     */
    public JsonStructure getJSONStructure() {
        if (type != ExtensionType.JSON) {
            throw new IllegalStateException();
        }
        return json;
    }

    /**
     * Set the JSON structure of the extension
     *
     * @param struct The JSON structure
     * @throws IllegalStateException    if the type is not
     *                                  {@code ExtensionType#JSON}
     * @throws IllegalArgumentException If the structure is not valid
     * @since 1.1
     */
    public void setJSONStructure(JsonStructure struct) {
        if (type != ExtensionType.JSON) {
            throw new IllegalStateException();
        }
        this.json = struct;
        try (final StringWriter w = new StringWriter()) {
            final JsonWriter jw = Json.createWriter(w);
            jw.write(struct);
            w.flush();
            this.text = w.toString();
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Not a json structure: " + struct, ioe);
        }
    }

    /**
     * Get the artifacts of the extension
     *
     * @return The artifacts
     * @throws IllegalStateException if the type is not
     *                               {@code ExtensionType#ARTIFACTS}
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
        Extension c = new Extension(type, name, state);
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
