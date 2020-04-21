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
package org.apache.sling.feature.io.json;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.felix.cm.json.ConfigurationReader;
import org.apache.felix.cm.json.ConfigurationResource;
import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.MatchingRequirement;
import org.apache.sling.feature.Prototype;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

/**
 * This class offers a static method to read a {@code Feature} using a {@code Reader} instance.
 */
public class FeatureJSONReader {

    /**
     * Read a new feature from the reader
     * The reader is not closed. It is up to the caller to close the reader.
     *
     * @param reader The reader for the feature
     * @param location Optional location
     * @return The read feature
     * @throws IOException If an IO errors occurs or the JSON is invalid.
     */
    public static Feature read(final Reader reader, final String location)
    throws IOException {
        try {
            final FeatureJSONReader mr = new FeatureJSONReader(location);
            return mr.readFeature(reader);
        } catch (final IllegalStateException | IllegalArgumentException | JsonException e) {
            throw new IOException(e);
        }
    }

    /** The read feature. */
    private Feature feature;

    /** The optional location. */
    private final String location;

    /** Exception prefix containing the location (if set) */
    private final String exceptionPrefix;

    /**
     * private constructor
     * @param location Optional location
     */
    private FeatureJSONReader(final String location) {
        this.location = location;
        if ( location == null ) {
            exceptionPrefix = "";
        } else {
            exceptionPrefix = location.concat(" : ");
        }
    }

    /**
     * Get the feature id
     * @param json The feature json
     * @return The artifact id
     * @throws IOException If the id is missing
     */
    private ArtifactId getFeatureId(final JsonObject json) throws IOException {
        if ( !json.containsKey(JSONConstants.FEATURE_ID) ) {
            throw new IOException(this.exceptionPrefix.concat("Feature id is missing"));
        }
        return checkTypeArtifactId(JSONConstants.FEATURE_ID, json.get(JSONConstants.FEATURE_ID));
    }

    private String getProperty(final JsonObject json, final String key) throws IOException {
        final JsonValue val = json.get(key);
        if ( val != null ) {
            return checkTypeString(key, val);
        }
        return null;
    }

    /**
     * Read the variables section
     * @param json The json describing the feature or application
     * @param kvMap The variables will be written to this Key Value Map
     * @throws IOException If the json is invalid.
     */
    private void readVariables(JsonObject json, Map<String,String> kvMap) throws IOException {
        if (json.containsKey(JSONConstants.FEATURE_VARIABLES)) {
            final JsonValue variablesObj = json.get(JSONConstants.FEATURE_VARIABLES);

            for (final Map.Entry<String, JsonValue> entry : checkTypeObject(JSONConstants.FEATURE_VARIABLES, variablesObj).entrySet()) {
                final String key = entry.getKey();
                // skip comments
                if ( !key.startsWith("#") ) {
                    if (kvMap.get(key) != null) {
                        throw new IOException(this.exceptionPrefix.concat("Duplicate variable ").concat(key));
                    }
                    final String value = checkScalarType("variable value", entry.getValue(), true);
                    kvMap.put(key, value);
                }
            }
        }
    }


    /**
     * Read the bundles / start levels section
     * @param json The json object describing the feature
     * @param container The bundles container
     * @param configContainer The configurations container
     * @throws IOException If the json is invalid.
     */
    private void readBundles(
            final JsonObject json,
            final Bundles container,
            final Configurations configContainer) throws IOException {
        if ( json.containsKey(JSONConstants.FEATURE_BUNDLES)) {
            final List<Artifact> list = new ArrayList<>();
            readArtifacts(JSONConstants.FEATURE_BUNDLES, "bundle", list, json.get(JSONConstants.FEATURE_BUNDLES), configContainer);

            for(final Artifact a : list) {
                if ( container.containsExact(a.getId())) {
                    throw new IOException(exceptionPrefix + "Duplicate identical bundle " + a.getId().toMvnId());
                }
                try {
                    // check start order
                    a.getStartOrder();
                } catch ( final IllegalArgumentException nfe) {
                    throw new IOException(exceptionPrefix + "Illegal start order '" + a.getMetadata().get(Artifact.KEY_START_ORDER) + "'");
                }
                container.add(a);
            }
        }
    }

    private void readArtifacts(final String section,
            final String artifactType,
            final List<Artifact> artifacts,
            final JsonValue listObj,
            final Configurations container)
    throws IOException {
        for(final JsonValue entry : checkTypeArray(section, listObj)) {
            final Artifact artifact;
            checkTypeObjectOrString(artifactType, entry);
            if ( entry.getValueType() == ValueType.STRING ) {
                // skip comments
                if ( ((JsonString)entry).getString().startsWith("#") ) {
                    continue;
                }
                artifact = new Artifact(checkTypeArtifactId(artifactType, entry));
            } else {
                final JsonObject bundleObj = (JsonObject) entry;
                if ( !bundleObj.containsKey(JSONConstants.ARTIFACT_ID) ) {
                    throw new IOException(exceptionPrefix.concat(" ").concat(artifactType).concat(" is missing required artifact id"));
                }
                final ArtifactId id = checkTypeArtifactId(artifactType.concat(" ").concat(JSONConstants.ARTIFACT_ID), bundleObj.get(JSONConstants.ARTIFACT_ID));

                artifact = new Artifact(id);
                for(final Map.Entry<String, JsonValue> metadataEntry : bundleObj.entrySet()) {
                    final String key = metadataEntry.getKey();
                    // skip comments
                    if ( key.startsWith("#") ) {
                        continue;
                    }
                    if ( JSONConstants.ARTIFACT_KNOWN_PROPERTIES.contains(key) ) {
                        continue;
                    }
                    final String mval = checkScalarType(artifactType.concat(" metadata ").concat(key), metadataEntry.getValue(), false);
                    artifact.getMetadata().put(key, mval);
                }
                if ( bundleObj.containsKey(JSONConstants.FEATURE_CONFIGURATIONS) ) {
                    final JsonObject cfgs = checkTypeObject(artifactType.concat(" configurations"), bundleObj.get(JSONConstants.FEATURE_CONFIGURATIONS));
                    addConfigurations(cfgs, artifact, container);
                }
            }
            artifacts.add(artifact);
        }
    }

    private void addConfigurations(final JsonObject json,
            final Artifact artifact,
            final Configurations container) throws IOException {
        final ConfigurationReader reader = org.apache.felix.cm.json.Configurations.buildReader()
                .verifyAsBundleResource(true)
                .withIdentifier(this.location)
                .build(json);
        final ConfigurationResource rsrc = reader.readConfigurationResource();
        if ( !reader.getIgnoredErrors().isEmpty() ) {
            final StringBuilder builder = new StringBuilder(exceptionPrefix);
            builder.append("Errors in configurations:");
            for(final String w : reader.getIgnoredErrors()) {
                builder.append("\n");
                builder.append(w);
            }
            throw new IOException(builder.toString());
        }

        for(final Map.Entry<String, Hashtable<String, Object>> c : rsrc.getConfigurations().entrySet()) {
            final Configuration config = new Configuration(c.getKey());

            for(final Map.Entry<String, Object> prop : c.getValue().entrySet()) {
                config.getProperties().put(prop.getKey(), prop.getValue());
            }
            if ( config.getProperties().get(Configuration.PROP_ARTIFACT_ID) != null ) {
                throw new IOException(exceptionPrefix.concat("Configuration must not define property ").concat(Configuration.PROP_ARTIFACT_ID));
            }
            if ( artifact != null ) {
                config.getProperties().put(Configuration.PROP_ARTIFACT_ID, artifact.getId().toMvnId());
            }
            for(final Configuration current : container) {
                if ( current.equals(config) ) {
                    throw new IOException(exceptionPrefix.concat("Duplicate configuration ").concat(config.getPid()));
                }
            }
            container.add(config);
        }
    }


    private void readConfigurations(final JsonObject json,
            final Configurations container) throws IOException {
        if ( json.containsKey(JSONConstants.FEATURE_CONFIGURATIONS) ) {
            final JsonObject cfgs = checkTypeObject(JSONConstants.FEATURE_CONFIGURATIONS, json.get(JSONConstants.FEATURE_CONFIGURATIONS));
            addConfigurations(cfgs, null, container);
        }
    }

    private void readFrameworkProperties(final JsonObject json,
            final Map<String,String> container) throws IOException {
        if ( json.containsKey(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES) ) {
            final JsonValue propsObj= json.get(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES);

            for(final Map.Entry<String, JsonValue> entry : checkTypeObject(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES, propsObj).entrySet()) {
                // skip comments
                if ( entry.getKey().startsWith("#") ) {
                    continue;
                }
                if ( container.get(entry.getKey()) != null ) {
                    throw new IOException(this.exceptionPrefix.concat("Duplicate framework property ").concat(entry.getKey()));
                }
                final String value = checkScalarType("framework property value", entry.getValue(), false);
                container.put(entry.getKey(), value);
            }

        }
    }

    private void readExtensions(final JsonObject json,
            final List<String> keywords,
            final Extensions container,
            final Configurations configContainer) throws IOException {
        final Set<String> keySet = new HashSet<>(json.keySet());
        keySet.removeAll(keywords);
        // the remaining keys are considered extensions!
        for(final String key : keySet) {
            if ( key.startsWith("#") ) {
                // skip comments
                continue;
            }
            final int pos = key.indexOf(':');
            final String postfix = pos == -1 ? null : key.substring(pos + 1);
            final int sep = (postfix == null ? key.indexOf('|') : postfix.indexOf('|'));
            final String name;
            final String type;
            final String state;
            if ( pos == -1 ) {
                type = ExtensionType.ARTIFACTS.name();
                if ( sep == -1 ) {
                    name = key;
                    state = ExtensionState.OPTIONAL.name();
                } else {
                    name = key.substring(0, sep);
                    state = key.substring(sep + 1);
                }
            } else {
                name = key.substring(0, pos);
                if ( sep == -1 ) {
                    type = postfix;
                    state = ExtensionState.OPTIONAL.name();
                } else {
                    type = postfix.substring(0, sep);
                    state = postfix.substring(sep + 1);
                }
            }
            if ( JSONConstants.FEATURE_KNOWN_PROPERTIES.contains(name) ) {
                throw new IOException(this.exceptionPrefix.concat("Extension is using reserved name : ").concat(name));
            }
            if ( container.getByName(name) != null ) {
                throw new IOException(exceptionPrefix.concat("Duplicate extension with name ").concat(name));
            }

            final ExtensionType extType = ExtensionType.valueOf(type);
            final ExtensionState extState;
            if (ExtensionState.OPTIONAL.name().equalsIgnoreCase(state)) {
                extState = ExtensionState.OPTIONAL;
            } else if (ExtensionState.REQUIRED.name().equalsIgnoreCase(state)) {
                extState = ExtensionState.REQUIRED;
            } else if (ExtensionState.TRANSIENT.name().equalsIgnoreCase(state)) {
                extState = ExtensionState.TRANSIENT;
            } else {
                final boolean opt = Boolean.valueOf(state).booleanValue();
                extState = opt ? ExtensionState.REQUIRED : ExtensionState.OPTIONAL;
            }

            final Extension ext = new Extension(extType, name, extState);
            final JsonValue value = json.get(key);
            switch ( extType ) {
                case ARTIFACTS : final List<Artifact> list = new ArrayList<>();
                                 readArtifacts("Extension ".concat(name), "artifact", list, value, configContainer);
                                 for(final Artifact a : list) {
                                     if ( ext.getArtifacts().contains(a) ) {
                                         throw new IOException(exceptionPrefix.concat("Duplicate artifact in extension ").concat(name).concat(" : ").concat(a.getId().toMvnId()));
                                     }
                                     ext.getArtifacts().add(a);
                                 }
                                 break;
                case JSON : if ( value.getValueType() != ValueType.ARRAY && value.getValueType() != ValueType.OBJECT ) {
                                throw new IOException(this.exceptionPrefix.concat("JSON Extension ").concat(name).concat(" is neither an object nor an array : ").concat(value.getValueType().name()));
                            }
                            ext.setJSONStructure((JsonStructure)value);
                            break;
                case TEXT : if ( value.getValueType() != ValueType.ARRAY && value.getValueType() != ValueType.STRING ) {
                                throw new IOException(this.exceptionPrefix.concat("Text Extension ").concat(name).concat(" is neither a string nor an array : ").concat(value.getValueType().name()));
                            }
                            if ( value.getValueType() == ValueType.STRING ) {
                                // string
                                ext.setText(((JsonString)value).getString());
                            } else {
                                // list (array of strings)
                                final StringBuilder sb = new StringBuilder();
                                for(final JsonValue o : value.asJsonArray()) {
                                    final String textValue = checkTypeString("Text Extension ".concat(name).concat(", value ").concat(o.toString()), o);
                                    sb.append(textValue);
                                    sb.append('\n');
                                }
                                ext.setText(sb.toString());
                            }
                            break;
            }

            container.add(ext);
        }
    }

    /**
     * Check if the value is a scalar type
     * @param key A key for the error message
     * @param value The value to check
     * @param allowNull Whether null is allowed as value
     * @return A string representing the value or {@code null}
     * @throws IOException If the value is not of the specified types
     */
    private String checkScalarType(final String key, final JsonValue value, boolean allowNull) throws IOException {
        if ( allowNull && value.getValueType() == ValueType.NULL ) {
            return null;
        }
        if ( value.getValueType() == ValueType.STRING || value.getValueType() == ValueType.NUMBER || value.getValueType() == ValueType.FALSE || value.getValueType() == ValueType.TRUE ) {
            return org.apache.felix.cm.json.Configurations.convertToObject(value).toString();
        }
        throw new IOException(this.exceptionPrefix.concat("Key ").concat(key).concat(" is not one of the allowed types string, number or boolean : ").concat(value.getValueType().name()));
    }

    /**
     * Check if the value is an object or a string
     * @param key A key for the error message
     * @param value The value to check
     * @throws IOException If the value is not of the specified types
     */
    private void checkTypeObjectOrString(final String key, final JsonValue value) throws IOException {
        if ( value.getValueType() != ValueType.STRING && value.getValueType() != ValueType.OBJECT ) {
            throw new IOException(this.exceptionPrefix.concat("Key ").concat(key).concat(" is neither a string nor an object : ").concat(value.getValueType().name()));
        }
    }

    /**
     * Check if the value is a boolean
     * @param key A key for the error message
     * @param value The value to check
     * @return The boolean value
     * @throws IOException If the value is not of the specified types
     */
    private boolean checkTypeBoolean(final String key, final JsonValue value) throws IOException {
        if ( value.getValueType() == ValueType.TRUE || value.getValueType() == ValueType.FALSE ) {
            return (Boolean)org.apache.felix.cm.json.Configurations.convertToObject(value);
        }
        throw new IOException(this.exceptionPrefix.concat("Key ").concat(key).concat(" is not of type boolean : ").concat(value.getValueType().name()));
    }

    /**
     * Check if the value is an artifact id
     * @param key A key for the error message
     * @param value The value to check
     * @return The artifact id
     * @throws IOException If the value is not a string and not a valid artifact id
     */
    private ArtifactId checkTypeArtifactId(final String key, final JsonValue value) throws IOException {
        final String textValue = checkTypeString(key, value);
        try {
            return ArtifactId.parse(textValue);
        } catch ( final IllegalArgumentException iae) {
            throw new IOException(this.exceptionPrefix.concat("Key ").concat(key).concat(" is not a valid artifact id : ").concat(textValue));
        }
    }

    /**
     * Check if the value is a string
     * @param key A key for the error message
     * @param value The value to check
     * @return The string value
     * @throws IOException If the value is not a string
     */
    private String checkTypeString(final String key, final JsonValue value) throws IOException {
        if ( value.getValueType() == ValueType.STRING) {
            return ((JsonString)value).getString();
        }
        throw new IOException(this.exceptionPrefix.concat("Key ").concat(key).concat(" is not of type string : ").concat(value.getValueType().name()));
    }

    /**
     * Check if the value is an object
     * @param key A key for the error message
     * @param value The value to check
     * @return The object
     * @throws IOException If the value is not an object
     */
    private JsonObject checkTypeObject(final String key, final JsonValue value) throws IOException {
        if ( value.getValueType() == ValueType.OBJECT) {
            return value.asJsonObject();
        }
        throw new IOException(this.exceptionPrefix.concat("Key ").concat(key).concat(" is not of type object : ").concat(value.getValueType().name()));
    }

    /**
     * Check if the value is an array
     * @param key A key for the error message
     * @param value The value to check
     * @return The array
     * @throws IOException If the value is not of the specified types
     */
    private JsonArray checkTypeArray(final String key, final JsonValue value) throws IOException {
        if ( value.getValueType() == ValueType.ARRAY) {
            return value.asJsonArray();
        }
        throw new IOException(this.exceptionPrefix.concat("Key ").concat(key).concat(" is not of type array : ").concat(value.getValueType().name()));
    }

    private Prototype readPrototype(final JsonObject json) throws IOException {
        if ( json.containsKey(JSONConstants.FEATURE_PROTOTYPE)) {
            final JsonValue prototypeObj = json.get(JSONConstants.FEATURE_PROTOTYPE);
            checkTypeObjectOrString(JSONConstants.FEATURE_PROTOTYPE, prototypeObj);

            final Prototype prototype;
            if ( prototypeObj.getValueType() == ValueType.STRING ) {
                prototype = new Prototype(checkTypeArtifactId(JSONConstants.FEATURE_PROTOTYPE, prototypeObj));
            } else {
                final JsonObject obj = (JsonObject) prototypeObj;
                if ( !obj.containsKey(JSONConstants.ARTIFACT_ID) ) {
                    throw new IOException(exceptionPrefix.concat(" prototype is missing required artifact id"));
                }
                prototype = new Prototype(checkTypeArtifactId("Prototype ".concat(JSONConstants.ARTIFACT_ID), obj.get(JSONConstants.ARTIFACT_ID)));

                if ( obj.containsKey(JSONConstants.PROTOTYPE_REMOVALS) ) {
                    final JsonObject removalObj = checkTypeObject("Prototype removals", obj.get(JSONConstants.PROTOTYPE_REMOVALS));
                    if ( removalObj.containsKey(JSONConstants.FEATURE_BUNDLES) ) {
                        for(final JsonValue val : checkTypeArray("Prototype removal bundles", removalObj.get(JSONConstants.FEATURE_BUNDLES))) {
                            if ( checkTypeString("Prototype removal bundles", val).startsWith("#")) {
                                continue;
                            }
                            prototype.getBundleRemovals().add(checkTypeArtifactId("Prototype removal bundles", val));
                        }
                    }
                    if ( removalObj.containsKey(JSONConstants.FEATURE_CONFIGURATIONS) ) {
                        for(final JsonValue val : checkTypeArray("Prototype removal configuration", removalObj.get(JSONConstants.FEATURE_CONFIGURATIONS))) {
                            final String propVal = checkTypeString("Prototype removal configuration", val);
                            if ( propVal.startsWith("#") ) {
                                continue;
                            }
                            prototype.getConfigurationRemovals().add(propVal);
                        }
                    }
                    if ( removalObj.containsKey(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES) ) {
                        for(final JsonValue val : checkTypeArray("Prototype removal framework properties", removalObj.get(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES))) {
                            final String propVal = checkTypeString("Prototype removal framework properties", val);
                            if ( propVal.startsWith("#") ) {
                                continue;
                            }
                            prototype.getFrameworkPropertiesRemovals().add(propVal);
                        }
                    }
                    if ( removalObj.containsKey(JSONConstants.PROTOTYPE_EXTENSION_REMOVALS) ) {
                        for(final JsonValue val : checkTypeArray("Prototype removal extensions", removalObj.get(JSONConstants.PROTOTYPE_EXTENSION_REMOVALS))) {
                            checkTypeObjectOrString("Prototype removal extension", val);
                            if ( val.getValueType() == ValueType.STRING ) {
                                final String propVal = org.apache.felix.cm.json.Configurations.convertToObject(val).toString();
                                if ( propVal.startsWith("#")) {
                                    continue;
                                }
                                prototype.getExtensionRemovals().add(propVal);
                            } else {
                                final JsonObject removalMap = (JsonObject)val;
                                final JsonValue nameObj = removalMap.get("name");
                                final String name = checkTypeString("Prototype removal extension", nameObj);
                                if ( removalMap.containsKey("artifacts") ) {
                                    final List<ArtifactId> ids = new ArrayList<>();
                                    for(final JsonValue aid : checkTypeArray("Prototype removal extension artifacts", removalMap.get("artifacts"))) {
                                        if ( checkTypeString("Prototype removal extension artifact", aid).startsWith("#")) {
                                            continue;
                                        }
                                        ids.add(checkTypeArtifactId("Prototype removal extension artifact", aid));
                                    }
                                    prototype.getArtifactExtensionRemovals().put(name, ids);
                                } else {
                                    prototype.getExtensionRemovals().add(name);
                                }
                            }
                        }
                    }
                    readRequirements(removalObj, prototype.getRequirementRemovals());
                    readCapabilities(removalObj, prototype.getCapabilityRemovals());

                }
            }
            return prototype;
        }
        return null;
    }

    private void readRequirements(final JsonObject json, final List<MatchingRequirement> container)
            throws IOException {
        if ( json.containsKey(JSONConstants.FEATURE_REQUIREMENTS)) {
            for(final JsonValue req : checkTypeArray(JSONConstants.FEATURE_REQUIREMENTS, json.get(JSONConstants.FEATURE_REQUIREMENTS))) {
                final JsonObject obj = checkTypeObject("Requirement", req);

                if ( !obj.containsKey(JSONConstants.REQCAP_NAMESPACE) ) {
                    throw new IOException(this.exceptionPrefix.concat("Namespace is missing for requirement"));
                }
                final String namespace = checkTypeString("Requirement namespace", obj.get(JSONConstants.REQCAP_NAMESPACE));

                Map<String, Object> attrMap = new HashMap<>();
                if ( obj.containsKey(JSONConstants.REQCAP_ATTRIBUTES) ) {
                    final JsonObject attrs = checkTypeObject("Requirement attributes", obj.get(JSONConstants.REQCAP_ATTRIBUTES));
                    attrs.forEach(rethrowBiConsumer((key, value) -> ManifestUtils.unmarshalAttribute(key, value, attrMap::put)));
                }

                Map<String, String> dirMap = new HashMap<>();
                if ( obj.containsKey(JSONConstants.REQCAP_DIRECTIVES) ) {
                    final JsonObject dirs = checkTypeObject("Requirement directives", obj.get(JSONConstants.REQCAP_DIRECTIVES));
                    dirs.forEach(rethrowBiConsumer((key, value) -> ManifestUtils.unmarshalDirective(key, value, dirMap::put)));
                }

                final MatchingRequirement r = new MatchingRequirementImpl(null,
                        namespace, dirMap, attrMap);
                container.add(r);
            }
        }
    }

    private void readCapabilities(final JsonObject json, final List<Capability> container) throws IOException {
        if ( json.containsKey(JSONConstants.FEATURE_CAPABILITIES)) {
            for(final JsonValue cap : checkTypeArray(JSONConstants.FEATURE_REQUIREMENTS, json.get(JSONConstants.FEATURE_CAPABILITIES))) {
                final JsonObject obj = checkTypeObject("Capability", cap);

                if ( !obj.containsKey(JSONConstants.REQCAP_NAMESPACE) ) {
                    throw new IOException(this.exceptionPrefix.concat("Namespace is missing for capability"));
                }
                final String namespace = checkTypeString("Capability namespace", obj.get(JSONConstants.REQCAP_NAMESPACE));

                Map<String, Object> attrMap = new HashMap<>();
                if ( obj.containsKey(JSONConstants.REQCAP_ATTRIBUTES) ) {
                    final JsonObject attrs = checkTypeObject("Capability attributes", obj.get(JSONConstants.REQCAP_ATTRIBUTES));
                    attrs.forEach(rethrowBiConsumer((key, value) -> ManifestUtils.unmarshalAttribute(key, value, attrMap::put)));
                }

                Map<String, String> dirMap = new HashMap<>();
                if ( obj.containsKey(JSONConstants.REQCAP_DIRECTIVES) ) {
                    final JsonObject dirs = checkTypeObject("Capability directives", obj.get(JSONConstants.REQCAP_DIRECTIVES));
                    dirs.forEach(rethrowBiConsumer((key, value) -> ManifestUtils.unmarshalDirective(key, value, dirMap::put)));
                }

                final Capability c = new CapabilityImpl(null, namespace, dirMap, attrMap);
                container.add(c);
            }
        }
    }

    @FunctionalInterface
    private interface BiConsumer_WithExceptions<T, V, E extends Exception> {
        void accept(T t, V u) throws E;
    }

    private static <T, V, E extends Exception> BiConsumer<T, V> rethrowBiConsumer(BiConsumer_WithExceptions<T, V, E> biConsumer) {
        return (t, u) -> {
            try {
                biConsumer.accept(t, u);
            } catch (Exception exception) {
                throwAsUnchecked(exception);
            }
        };
    }

    @SuppressWarnings ("unchecked")
    private static <E extends Throwable> void throwAsUnchecked(Exception exception) throws E {
        throw (E) exception;
    }

    private static class MatchingRequirementImpl extends RequirementImpl implements MatchingRequirement {

        public MatchingRequirementImpl(Resource res, String ns, Map<String, String> dirs, Map<String, Object> attrs) {
            super(res, ns, dirs, attrs);
        }
    }

    /**
     * Read a full feature
     * @param reader The reader
     * @return The feature object
     * @throws IOException If an IO error occurs or the JSON is not valid.
     */
    private Feature readFeature(final Reader reader)
    throws IOException {
        final JsonObject json = Json.createReader(org.apache.felix.cm.json.Configurations.jsonCommentAwareReader(reader)).readObject();

        checkModelVersion(json);

        final ArtifactId featureId = this.getFeatureId(json);
        this.feature = new Feature(featureId);
        this.feature.setLocation(this.location);

        // final flag
        if (json.containsKey(JSONConstants.FEATURE_FINAL)) {
            this.feature.setFinal(checkTypeBoolean(JSONConstants.FEATURE_FINAL, json.get(JSONConstants.FEATURE_FINAL)));
        }

        // complete flag
        if (json.containsKey(JSONConstants.FEATURE_COMPLETE)) {
            this.feature.setComplete(checkTypeBoolean(JSONConstants.FEATURE_COMPLETE, json.get(JSONConstants.FEATURE_COMPLETE)));
        }

        // title, description, vendor and license
        this.feature.setTitle(getProperty(json, JSONConstants.FEATURE_TITLE));
        this.feature.setDescription(getProperty(json, JSONConstants.FEATURE_DESCRIPTION));
        this.feature.setVendor(getProperty(json, JSONConstants.FEATURE_VENDOR));
        this.feature.setLicense(getProperty(json, JSONConstants.FEATURE_LICENSE));

        this.readVariables(json, feature.getVariables());
        this.readBundles(json, feature.getBundles(), feature.getConfigurations());
        this.readFrameworkProperties(json, feature.getFrameworkProperties());
        this.readConfigurations(json, feature.getConfigurations());

        this.readCapabilities(json, feature.getCapabilities());
        this.readRequirements(json, feature.getRequirements());
        this.feature.setPrototype(this.readPrototype(json));

        this.readExtensions(json,
                JSONConstants.FEATURE_KNOWN_PROPERTIES,
                this.feature.getExtensions(), this.feature.getConfigurations());

        return this.feature;
    }

    private void checkModelVersion(final JsonObject json) throws IOException {
        String modelVersion = getProperty(json, JSONConstants.FEATURE_MODEL_VERSION);
        if (modelVersion == null) {
            modelVersion = "1";
        }
        if (!"1".equals(modelVersion)) {
            throw new IOException(this.exceptionPrefix.concat("Unsupported model version: ").concat(modelVersion));
        }
    }
}


