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
package org.apache.sling.feature.io.json;

import java.io.IOException;
import java.io.Writer;

import javax.json.stream.JsonGenerator;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;

/**
 * Simple JSON writer for a feature
 */
public class FeatureJSONWriter extends JSONWriterBase {

    /**
     * Writes the feature to the writer.
     * The writer is not closed.
     * @param writer Writer
     * @param feature Feature
     * @throws IOException If writing fails
     */
    public static void write(final Writer writer, final Feature feature)
    throws IOException {
        final FeatureJSONWriter w = new FeatureJSONWriter();
        w.writeFeature(writer, feature);
    }

    protected FeatureJSONWriter() {
    	// protected constructor for subclassing
    }

    protected void writeFeature(final Writer writer, final Feature feature)
    throws IOException {
        JsonGenerator generator = newGenerator(writer);
        generator.writeStartObject();

        writeFeatureId(generator, feature);

        // title, description, vendor, license
        writeProperty(generator, JSONConstants.FEATURE_TITLE, feature.getTitle());
        writeProperty(generator, JSONConstants.FEATURE_DESCRIPTION, feature.getDescription());
        writeProperty(generator, JSONConstants.FEATURE_VENDOR, feature.getVendor());
        writeProperty(generator, JSONConstants.FEATURE_LICENSE, feature.getLicense());

        // variables
        writeVariables(generator, feature.getVariables());

        // include
        writeInclude(generator, feature.getInclude());

        // requirements
        writeRequirements(generator, feature.getRequirements());

        // capabilities
        writeCapabilities(generator, feature.getCapabilities());

        // bundles
        writeBundles(generator, feature.getBundles(), feature.getConfigurations());

        // configurations
        final Configurations cfgs = new Configurations();
        for(final Configuration cfg : feature.getConfigurations()) {
            final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT_ID);
            if (  artifactProp == null ) {
                cfgs.add(cfg);
            }
        }
        writeConfigurations(generator, cfgs);

        // framework properties
        writeFrameworkProperties(generator, feature.getFrameworkProperties());

        // extensions
        writeExtensions(generator, feature.getExtensions(), feature.getConfigurations());

        generator.writeEnd().close();
    }

    protected void writeFeatureId(final JsonGenerator generator,
    		final Feature feature) {
        writeProperty(generator, JSONConstants.FEATURE_ID, feature.getId().toMvnId());
    }
}
