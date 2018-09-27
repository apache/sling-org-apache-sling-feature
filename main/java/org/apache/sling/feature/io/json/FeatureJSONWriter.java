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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.json.stream.JsonGenerator;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Include;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * Simple JSON writer for a feature
 */
public class FeatureJSONWriter extends JSONWriterBase {
    private FeatureJSONWriter() {}

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

    private void writeProperty(final JsonGenerator generator, final String key, final String value) {
        if ( value != null ) {
            generator.write(key, value);
        }
    }

    private <T> void writeList(final JsonGenerator generator, final String name, final Collection<T> values) {
        if (!values.isEmpty()) {
            generator.writeStartArray(name);
            for (T value : values) {
                generator.write(value.toString());
            }
            generator.writeEnd();
        }
    }

    private void writeInclude(final JsonGenerator generator, final Include inc) {
        if (inc == null) {
            return;
        }

        if ( inc.getArtifactExtensionRemovals().isEmpty()
             && inc.getBundleRemovals().isEmpty()
             && inc.getConfigurationRemovals().isEmpty()
             && inc.getFrameworkPropertiesRemovals().isEmpty() ) {

            generator.write(JSONConstants.FEATURE_INCLUDE, inc.getId().toMvnId());
        } else {
            generator.writeStartObject(JSONConstants.FEATURE_INCLUDE);
            writeProperty(generator, JSONConstants.ARTIFACT_ID, inc.getId().toMvnId());

            generator.writeStartObject(JSONConstants.INCLUDE_REMOVALS);

            if ( !inc.getArtifactExtensionRemovals().isEmpty()
                 || inc.getExtensionRemovals().isEmpty() ) {
                generator.writeStartArray(JSONConstants.INCLUDE_EXTENSION_REMOVALS);

                for(final String id : inc.getExtensionRemovals()) {
                    generator.write(id);
                }
                for(final Map.Entry<String, List<ArtifactId>> entry : inc.getArtifactExtensionRemovals().entrySet()) {
                    generator.writeStartObject();

                    writeList(generator, entry.getKey(), entry.getValue());

                    generator.writeEnd();
                }

                generator.writeEnd();
            }
            writeList(generator, JSONConstants.FEATURE_CONFIGURATIONS, inc.getConfigurationRemovals());
            writeList(generator, JSONConstants.FEATURE_BUNDLES, inc.getBundleRemovals());
            writeList(generator, JSONConstants.FEATURE_FRAMEWORK_PROPERTIES, inc.getFrameworkPropertiesRemovals());

            generator.writeEnd();
        }
    }

    private void writeRequirements(final JsonGenerator generator, final List<Requirement> requirements) {
        if (requirements.isEmpty()) {
            return;
        }

        generator.writeStartArray(JSONConstants.FEATURE_REQUIREMENTS);

        for(final Requirement req : requirements) {
            generator.writeStartObject();
            writeProperty(generator, JSONConstants.REQCAP_NAMESPACE, req.getNamespace());
            if ( !req.getAttributes().isEmpty() ) {
                generator.writeStartObject(JSONConstants.REQCAP_ATTRIBUTES);
                req.getAttributes().forEach((key, value) -> ManifestUtils.marshalAttribute(key, value, generator::write));
                generator.writeEnd();
            }
            if ( !req.getDirectives().isEmpty() ) {
                generator.writeStartObject(JSONConstants.REQCAP_DIRECTIVES);
                req.getDirectives().forEach((key, value) -> ManifestUtils.marshalDirective(key, value, generator::write));
                generator.writeEnd();
            }
            generator.writeEnd();
        }

        generator.writeEnd();
    }

    private void writeCapabilities(final JsonGenerator generator, final List<Capability> capabilities) {
        if (capabilities.isEmpty()) {
            return;
        }

        generator.writeStartArray(JSONConstants.FEATURE_CAPABILITIES);

        for(final Capability cap : capabilities) {
            generator.writeStartObject();
            writeProperty(generator, JSONConstants.REQCAP_NAMESPACE, cap.getNamespace());
            if ( !cap.getAttributes().isEmpty() ) {
                generator.writeStartObject(JSONConstants.REQCAP_ATTRIBUTES);
                cap.getAttributes().forEach((key, value) -> ManifestUtils.marshalAttribute(key, value, generator::write));
                generator.writeEnd();
            }
            if ( !cap.getDirectives().isEmpty() ) {
                generator.writeStartObject(JSONConstants.REQCAP_DIRECTIVES);
                cap.getDirectives().forEach((key, value) -> ManifestUtils.marshalDirective(key, value, generator::write));
                generator.writeEnd();
            }
            generator.writeEnd();
        }

        generator.writeEnd();
    }

    private void writeFeature(final Writer writer, final Feature feature)
    throws IOException {
        JsonGenerator generator = newGenerator(writer);
        generator.writeStartObject();

        writeProperty(generator, JSONConstants.FEATURE_ID, feature.getId().toMvnId());

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
}
