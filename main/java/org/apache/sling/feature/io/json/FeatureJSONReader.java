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
import java.io.StringReader;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;

/**
 * This class offers a method to read a {@code Feature} using a {@code Reader} instance.
 */
public class FeatureJSONReader extends JSONReaderBase {

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
        } catch (final IllegalStateException | IllegalArgumentException | JsonParsingException e) {
            throw new IOException(e);
        }
    }

    /** The read feature. */
    protected Feature feature;

    /**
     * Private constructor
     * @param location Optional location
     */
    protected FeatureJSONReader(final String location) {
        super(location);
    }

    /**
     * Read a full feature
     * @param reader The reader
     * @return The feature object
     * @throws IOException If an IO error occurs or the JSON is not valid.
     */
    protected Feature readFeature(final Reader reader)
    throws IOException {
        final JsonObject json = Json.createReader(new StringReader(minify(reader))).readObject();
        final Map<String, Object> map = getJsonMap(json);

        checkModelVersion(map);

        final ArtifactId featureId = this.getFeatureId(map);
        this.feature = new Feature(featureId);
        this.feature.setLocation(this.location);

        // final flag
        if (map.containsKey(JSONConstants.FEATURE_FINAL)) {
            final Object finalObj = map.get(JSONConstants.FEATURE_FINAL);
            checkType(JSONConstants.FEATURE_FINAL, finalObj, Boolean.class);
            this.feature.setFinal(((Boolean) finalObj).booleanValue());
        }

        // complete flag
        if (map.containsKey(JSONConstants.FEATURE_COMPLETE)) {
            final Object completeObj = map.get(JSONConstants.FEATURE_COMPLETE);
            checkType(JSONConstants.FEATURE_COMPLETE, completeObj, Boolean.class);
            this.feature.setComplete(((Boolean) completeObj).booleanValue());
        }

        // title, description, vendor and license
        this.feature.setTitle(getProperty(map, JSONConstants.FEATURE_TITLE));
        this.feature.setDescription(getProperty(map, JSONConstants.FEATURE_DESCRIPTION));
        this.feature.setVendor(getProperty(map, JSONConstants.FEATURE_VENDOR));
        this.feature.setLicense(getProperty(map, JSONConstants.FEATURE_LICENSE));

        this.readVariables(map, feature.getVariables());
        this.readBundles(map, feature.getBundles(), feature.getConfigurations());
        this.readFrameworkProperties(map, feature.getFrameworkProperties());
        this.readConfigurations(map, feature.getConfigurations());

        this.readCapabilities(map, feature.getCapabilities());
        this.readRequirements(map, feature.getRequirements());
        feature.setPrototype(this.readPrototype(map));

        this.readExtensions(map,
                JSONConstants.FEATURE_KNOWN_PROPERTIES,
                this.feature.getExtensions(), this.feature.getConfigurations());

        return feature;
    }

    private void checkModelVersion(final Map<String, Object> map) throws IOException {
        String modelVersion = getProperty(map, JSONConstants.FEATURE_MODEL_VERSION);
        if (modelVersion == null) {
            modelVersion = "1";
        }
        if (!"1".equals(modelVersion)) {
            throw new IOException("Unsupported model version: " + modelVersion);
        }
    }

    protected ArtifactId getFeatureId(final Map<String, Object> map) throws IOException {
        if ( !map.containsKey(JSONConstants.FEATURE_ID) ) {
            throw new IOException(this.exceptionPrefix + "Feature id is missing");
        }
        final Object idObj = map.get(JSONConstants.FEATURE_ID);
        checkType(JSONConstants.FEATURE_ID, idObj, String.class);
        return ArtifactId.parse(idObj.toString());
    }
}


