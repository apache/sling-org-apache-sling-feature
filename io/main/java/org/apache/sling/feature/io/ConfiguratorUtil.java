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
package org.apache.sling.feature.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Dictionary;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.apache.felix.cm.json.Configurations;
import org.apache.sling.feature.Configuration;

/**
 * Helper class to write JSON structures as defined in
 * <a href="https://osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html#d0e131765">OSGi Configurator Specification 1.0</a>.
 * @deprecated Use {@link org.apache.felix.cm.json.Configurations} instead.
 */
@Deprecated
public class ConfiguratorUtil {

    private ConfiguratorUtil() {
    }

    protected static final JsonGenerator newGenerator(final Writer writer) {
        JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

        // prevent closing of the underlying writer
        Writer closeShieldWriter = new CloseShieldWriter(writer);
        return generatorFactory.createGenerator(closeShieldWriter);
    }

    /** Write the OSGi configuration to a JSON structure.
     * The writer is not closed.
     *
     * @param writer Writer
     * @param props The configuration properties to write */
    public static void writeConfiguration(final Writer writer, final Dictionary<String, Object> props) {
        final Object artifactId = props.remove(Configuration.PROP_ARTIFACT_ID);
        try {
            Configurations.buildWriter().build(writer).writeConfiguration(props);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write configuration.", e);
        } finally {
            if ( artifactId != null ) {
                props.put(Configuration.PROP_ARTIFACT_ID, artifactId);
            }
        }
    }

    public static void writeConfiguration(final JsonGenerator generator, final Dictionary<String, Object> props) {
        final Object artifactId = props.remove(Configuration.PROP_ARTIFACT_ID);
        try {
            Configurations.buildWriter().build(generator).writeConfiguration(props);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write configuration.", e);
        } finally {
            if ( artifactId != null ) {
                props.put(Configuration.PROP_ARTIFACT_ID, artifactId);
            }
        }
    }
}
