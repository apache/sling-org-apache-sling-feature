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
import java.util.Hashtable;
import java.util.Map;

import org.apache.felix.cm.json.ConfigurationReader;
import org.apache.felix.cm.json.ConfigurationResource;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;

/**
 * JSON Reader for configurations.
 */
public class ConfigurationJSONReader {

    /**
     * Read a map of configurations from the reader
     * The reader is not closed. It is up to the caller to close the reader.
     *
     * @param reader The reader for the configuration
     * @param location Optional location
     * @return The read configurations
     * @throws IOException If an IO errors occurs or the JSON is invalid.
     */
    public static Configurations read(final Reader reader, final String location)
    throws IOException {
        try {
            final ConfigurationJSONReader mr = new ConfigurationJSONReader();
            return mr.readConfigurations(location, reader);
        } catch (final IllegalStateException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    Configurations readConfigurations(final String location, final Reader reader) throws IOException {
        final Configurations result = new Configurations();

        final ConfigurationReader cfgReader = org.apache.felix.cm.json.Configurations
            .buildReader()
            .withIdentifier(location)
            .verifyAsBundleResource(true)
            .build(reader);
        final ConfigurationResource rsrc = cfgReader.readConfigurationResource();
        for(Map.Entry<String, Hashtable<String, Object>> entry : rsrc.getConfigurations().entrySet() ) {
            final Configuration cf = new Configuration(entry.getKey());
            for(final Map.Entry<String, Object> prop : entry.getValue().entrySet()) {
                cf.getProperties().put(prop.getKey(), prop.getValue());
            }
        }

        return result;
    }
}


