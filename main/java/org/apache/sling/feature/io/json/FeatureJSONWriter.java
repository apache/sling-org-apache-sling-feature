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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.apache.felix.cm.json.ConfigurationResource;
import org.apache.felix.cm.json.ConfigurationWriter;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.MatchingRequirement;
import org.apache.sling.feature.Prototype;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * This class offers a static method to write a feature using a writer.
 */
public class FeatureJSONWriter {

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

    private final JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    private final JsonGenerator newGenerator(final Writer writer) {
        // prevent closing of the underlying writer
        return generatorFactory.createGenerator(new FilterWriter(writer) {

            @Override
            public void close() throws IOException {
                super.flush();
            }
        });
    }

    private void writeBundles(final JsonGenerator generator,
            final Bundles bundles,
            final Configurations allConfigs) {
        // bundles
        if ( !bundles.isEmpty() ) {
            generator.writeStartArray(JSONConstants.FEATURE_BUNDLES);

            for(final Artifact artifact : bundles) {
                final Configurations cfgs = new Configurations();
                for(final Configuration cfg : allConfigs) {
                    final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT_ID);
                    if ( artifact.getId().toMvnId().equals(artifactProp) ) {
                        cfgs.add(cfg);
                    }
                }
                Map<String,String> md = artifact.getMetadata();
                if ( md.isEmpty() && cfgs.isEmpty() ) {
                    generator.write(artifact.getId().toMvnId());
                } else {
                    generator.writeStartObject();
                    generator.write(JSONConstants.ARTIFACT_ID, artifact.getId().toMvnId());

                    Object runmodes = md.remove("runmodes");
                    if (runmodes instanceof String) {
                        md.put("run-modes", (String) runmodes);
                    }

                    for(final Map.Entry<String, String> me : md.entrySet()) {
                        generator.write(me.getKey(), me.getValue());
                    }

                    generator.writeEnd();
                }
            }

            generator.writeEnd();
        }
    }

    /**
     * Write the list of configurations into a "configurations" element
     * @param generator The json generator
     * @param cfgs The list of configurations
     * @throws IOException
     */
    private void writeConfigurations(final JsonGenerator generator, final Configurations cfgs) throws IOException {
        if ( cfgs.isEmpty() ) {
            return;
        }

        generator.writeKey(JSONConstants.FEATURE_CONFIGURATIONS);

        final ConfigurationWriter cfgWriter = org.apache.felix.cm.json.Configurations
                .buildWriter()
                .build(generator);

        final ConfigurationResource rsrc = new ConfigurationResource();
        for(final Configuration cfg : cfgs) {
            final Hashtable<String, Object> properties;
            if ( cfg.getProperties() instanceof Hashtable && cfg.getProperties().get(Configuration.PROP_ARTIFACT_ID) == null ) {
                properties = (Hashtable<String, Object>)cfg.getProperties();
            } else {
                properties = org.apache.felix.cm.json.Configurations.newConfiguration();
                for(final String name : Collections.list(cfg.getProperties().keys()) ) {
                    if ( !Configuration.PROP_ARTIFACT_ID.equals(name) ) {
                        properties.put(name, cfg.getProperties().get(name));
                    }
                }
            }
            rsrc.getConfigurations().put(cfg.getPid(), properties);
        }
        cfgWriter.writeConfigurationResource(rsrc);
    }

    private void writeVariables(final JsonGenerator generator, final Map<String,String> vars) {
        if ( !vars.isEmpty()) {
            generator.writeStartObject(JSONConstants.FEATURE_VARIABLES);

            for (final Map.Entry<String, String> entry : vars.entrySet()) {
                String val = entry.getValue();
                if (val != null)
                    generator.write(entry.getKey(), val);
                else
                    generator.writeNull(entry.getKey());
            }

            generator.writeEnd();
        }
    }

    private void writeFrameworkProperties(final JsonGenerator generator, final Map<String,String> props) {
        // framework properties
        if ( !props.isEmpty() ) {
            generator.writeStartObject(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES);
            for(final Map.Entry<String, String> entry : props.entrySet()) {
                generator.write(entry.getKey(), entry.getValue());
            }
            generator.writeEnd();
        }
    }

    private void writeExtensions(final JsonGenerator generator,
            final List<Extension> extensions,
            final Configurations allConfigs) throws IOException {
        for(final Extension ext : extensions) {
            final String state;
            switch (ext.getState()) {
            case OPTIONAL:
                state = "false";
                break;
            case REQUIRED:
                state = "true";
                break;
            default:
                state = ext.getState().name();
            }
            final String key = ext.getName().concat(":").concat(ext.getType().name()).concat("|").concat(state);
            if ( ext.getType() == ExtensionType.JSON ) {
                generator.write(key, ext.getJSONStructure());
            } else if ( ext.getType() == ExtensionType.TEXT ) {
                generator.writeStartArray(key);
                for(String line : ext.getText().split("\n")) {
                    generator.write(line);
                }
                generator.writeEnd();
            } else {
                generator.writeStartArray(key);
                for(final Artifact artifact : ext.getArtifacts()) {
                    final Configurations artifactCfgs = new Configurations();
                    for(final Configuration cfg : allConfigs) {
                        final String artifactProp = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT_ID);
                        if (  artifact.getId().toMvnId().equals(artifactProp) ) {
                            artifactCfgs.add(cfg);
                        }
                    }
                    if ( artifact.getMetadata().isEmpty() && artifactCfgs.isEmpty() ) {
                        generator.write(artifact.getId().toMvnId());
                    } else {
                        generator.writeStartObject();
                        generator.write(JSONConstants.ARTIFACT_ID, artifact.getId().toMvnId());

                        for(final Map.Entry<String, String> me : artifact.getMetadata().entrySet()) {
                            generator.write(me.getKey(), me.getValue());
                        }

                        writeConfigurations(generator, artifactCfgs);

                        generator.writeEnd();
                    }
                }
                generator.writeEnd();
            }
        }
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

    private void writePrototype(final JsonGenerator generator, final Prototype inc) {
        if (inc == null) {
            return;
        }

        if ( inc.getArtifactExtensionRemovals().isEmpty()
             && inc.getBundleRemovals().isEmpty()
             && inc.getConfigurationRemovals().isEmpty()
             && inc.getFrameworkPropertiesRemovals().isEmpty()
             && inc.getRequirementRemovals().isEmpty()
             && inc.getCapabilityRemovals().isEmpty() ) {

            generator.write(JSONConstants.FEATURE_PROTOTYPE, inc.getId().toMvnId());
        } else {
            generator.writeStartObject(JSONConstants.FEATURE_PROTOTYPE);
            writeProperty(generator, JSONConstants.ARTIFACT_ID, inc.getId().toMvnId());

            generator.writeStartObject(JSONConstants.PROTOTYPE_REMOVALS);

            if ( !inc.getArtifactExtensionRemovals().isEmpty()
                 || inc.getExtensionRemovals().isEmpty() ) {
                generator.writeStartArray(JSONConstants.PROTOTYPE_EXTENSION_REMOVALS);

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

            writeRequirements(generator, inc.getRequirementRemovals());
            writeCapabilities(generator, inc.getCapabilityRemovals());

            generator.writeEnd().writeEnd();
        }
    }

    private void writeRequirements(final JsonGenerator generator, final List<MatchingRequirement> requirements) {
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

    /**
     * Writes the feature to the writer.
     * The writer is not closed.
     * @param writer Writer
     * @param feature Feature
     * @throws IOException If writing fails
     */
    private void writeFeature(final Writer writer, final Feature feature)
    throws IOException {
        JsonGenerator generator = newGenerator(writer);
        generator.writeStartObject();

        writeFeatureId(generator, feature);

        if (feature.isFinal()) {
            generator.write(JSONConstants.FEATURE_FINAL, true);
        }

        if (feature.isComplete()) {
            generator.write(JSONConstants.FEATURE_COMPLETE, true);
        }

        // title, description, vendor, license
        writeProperty(generator, JSONConstants.FEATURE_TITLE, feature.getTitle());
        writeProperty(generator, JSONConstants.FEATURE_DESCRIPTION, feature.getDescription());
        writeProperty(generator, JSONConstants.FEATURE_VENDOR, feature.getVendor());
        writeProperty(generator, JSONConstants.FEATURE_LICENSE, feature.getLicense());

        // variables
        writeVariables(generator, feature.getVariables());

        // prototype
        writePrototype(generator, feature.getPrototype());

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

    private void writeFeatureId(final JsonGenerator generator,
    		final Feature feature) {
        writeProperty(generator, JSONConstants.FEATURE_ID, feature.getId().toMvnId());
    }

}
