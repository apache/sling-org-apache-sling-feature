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

import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.KeyValueMap;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * This class offers a method to read an {@code Application} using a {@code Reader} instance.
 */
public class ApplicationJSONReader extends JSONReaderBase {

    /**
     * Read a new application from the reader
     * The reader is not closed. It is up to the caller to close the reader.
     *
     * @param reader The reader for the feature
     * @return The application
     * @throws IOException If an IO errors occurs or the JSON is invalid.
     */
    public static Application read(final Reader reader) throws IOException {
        return read(reader, Collections.emptyMap());
    }

    /**
     * Read a new application from the reader
     * The reader is not closed. It is up to the caller to close the reader.
     *
     * @param reader The reader for the feature
     * @param overriddenVariables Map of variables that override the variable
     * values as in the application JSON
     * @return The application
     * @throws IOException If an IO errors occurs or the JSON is invalid.
     */
    public static Application read(final Reader reader, Map<String, String> overriddenVariables)
    throws IOException {
        try {
            final ApplicationJSONReader mr = new ApplicationJSONReader();

            mr.readApplication(reader, overriddenVariables);
            return mr.app;
        } catch (final IllegalStateException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    /** The read application. */
    private final Application app;

    /** This holds the application variables including any overrides provided,
     * e.g. via the launcher commandline.
     */
    private volatile KeyValueMap variables;

    /**
     * Private constructor
     */
    private ApplicationJSONReader() {
        super(null);
        this.app = new Application();
    }

    /**
     * Read a full application
     * @param reader The reader
     * @param overriddenVariables
     * @throws IOException If an IO error occurs or the JSON is not valid.
     */
    private void readApplication(final Reader reader, Map<String, String> overriddenVariables)
    throws IOException {
        final JsonObject json = Json.createReader(new StringReader(minify(reader))).readObject();

        @SuppressWarnings("unchecked")
        final Map<String, Object> map = (Map<String, Object>) JSONUtil.getValue(json);

        final String frameworkId = this.getProperty(map, JSONConstants.APP_FRAMEWORK);
        if ( frameworkId != null ) {
            app.setFramework(ArtifactId.parse(frameworkId));
        }
        this.readVariables(map, app.getVariables());

        this.variables = new KeyValueMap();
        this.variables.putAll(app.getVariables());

        // Apply the overrides
        for (Map.Entry<String, String> entry : overriddenVariables.entrySet()) {
            variables.put(entry.getKey(), entry.getValue());
        }

        this.readBundles(map, app.getBundles(), app.getConfigurations());
        this.readFrameworkProperties(map, app.getFrameworkProperties());
        this.readConfigurations(map, app.getConfigurations());

        this.readExtensions(map,
                JSONConstants.APP_KNOWN_PROPERTIES,
                this.app.getExtensions(), this.app.getConfigurations());
    }

    @Override
    protected Object handleResolveVars(Object val) {
        return handleVars(val, variables);
    }

    @Override
    protected Object handleLaunchVars(Object val) {
        return handleVars(val, variables);
    }
}


