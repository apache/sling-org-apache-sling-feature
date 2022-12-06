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
package org.apache.sling.feature.osgi;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.felix.cm.json.io.Configurations;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.osgi.service.feature.Feature;
import org.osgi.service.feature.FeatureArtifact;
import org.osgi.service.feature.FeatureArtifactBuilder;
import org.osgi.service.feature.FeatureBuilder;
import org.osgi.service.feature.FeatureBundle;
import org.osgi.service.feature.FeatureBundleBuilder;
import org.osgi.service.feature.FeatureConfiguration;
import org.osgi.service.feature.FeatureConfigurationBuilder;
import org.osgi.service.feature.FeatureExtension;
import org.osgi.service.feature.FeatureExtensionBuilder;
import org.osgi.service.feature.FeatureService;
import org.osgi.service.feature.ID;
import org.osgi.service.feature.FeatureExtension.Kind;
import org.osgi.service.feature.FeatureExtension.Type;

/**
 * Utility class to convert Apache Sling features to OSGi feature and vice versa.
 */
public class Converters {

    /** Use the first available feature service */
    private static final FeatureService service = ServiceLoader.load(FeatureService.class).iterator().next();

    /** Constant for framework launching properties extension */
    private static final String FRAMEWORK_PROPERTIES_EXTENSION = "framework-launching-properties";

    /** Constant for framework properties metadata */
    private static final String FRAMEWORK_PROPERTIES_METADATA = "framework-properties-metadata";

    /** Constant for metadata of variables */
    private static final String VARIABLES_METADATA = "variables-metadata";

    /**
     * Convert an Apache Sling feature into an OSGi feature
     * @param feature The feature to convert
     * @return The OSGi feature or {@code null} if feature is {@code null}
     * @throws IOException If the conversion fails
     */
    public static Feature convert(final org.apache.sling.feature.Feature feature) throws IOException {
        if ( feature == null ) {
            return null;
        }
        final ID id = service.getIDfromMavenCoordinates(feature.getId().toMvnId());
        final FeatureBuilder builder = service.getBuilderFactory().newFeatureBuilder(id);

        // metadata
        builder.setComplete(feature.isComplete());
        builder.setDescription(feature.getDescription());
        builder.setLicense(feature.getLicense());
        builder.setName(feature.getTitle());
        builder.setVendor(feature.getVendor());
        builder.setDocURL(feature.getDocURL());
        builder.setSCM(feature.getSCMInfo());
        for(final String v : feature.getCategories()) {
            builder.addCategories(v);
        }

        // variables
        feature.getVariables().entrySet().stream().forEach(entry -> builder.addVariable(entry.getKey(), entry.getValue()));

        // bundles
        for(final Artifact bundle : feature.getBundles()) {
            final FeatureBundleBuilder b = service.getBuilderFactory().newBundleBuilder(service.getIDfromMavenCoordinates(bundle.getId().toMvnId()));
            bundle.getMetadata().entrySet().stream().forEach(entry -> b.addMetadata(entry.getKey(), entry.getValue()));
            builder.addBundles(b.build());
        }

        // configurations
        for(final Configuration cfg : feature.getConfigurations()) {
            final FeatureConfigurationBuilder b;
            if ( cfg.isFactoryConfiguration() ) {
                b = service.getBuilderFactory().newConfigurationBuilder(cfg.getFactoryPid(), cfg.getName());
            } else {
                b = service.getBuilderFactory().newConfigurationBuilder(cfg.getPid());
            }
            for(final String name : Collections.list(cfg.getProperties().keys()) ) {
                b.addValue(name, cfg.getProperties().get(name));
            }
            builder.addConfigurations(b.build());
        }

        // extensions
        for(final Extension ext : feature.getExtensions()) {
            FeatureExtension.Type type;
            if ( ext.getType() == ExtensionType.ARTIFACTS ) {
                type = Type.ARTIFACTS;
            } else if ( ext.getType() == ExtensionType.TEXT ) {
                type = Type.TEXT;
            } else {
                type = Type.JSON;
            }
            FeatureExtension.Kind kind;
            if ( ext.getState() == ExtensionState.OPTIONAL ) {
                kind = Kind.OPTIONAL;
            } else if ( ext.getState() == ExtensionState.REQUIRED ) {
                kind = Kind.MANDATORY;
            } else {
                kind = Kind.TRANSIENT;
            }
            final FeatureExtensionBuilder b = service.getBuilderFactory().newExtensionBuilder(ext.getName(), type, kind);
            if ( ext.getType() == ExtensionType.ARTIFACTS ) {
                for(final Artifact artifact : ext.getArtifacts()) {
                    final FeatureArtifactBuilder ab = service.getBuilderFactory().newArtifactBuilder(service.getIDfromMavenCoordinates(artifact.getId().toMvnId()));
                    artifact.getMetadata().entrySet().stream().forEach(entry -> ab.addMetadata(entry.getKey(), entry.getValue()));
                    b.addArtifact(ab.build());
                }
            } else if ( ext.getType() == ExtensionType.TEXT ) {
                if ( ext.getText() != null ) {
                    for(final String t : ext.getText().split("\n")) {
                        b.addText(t);
                    }
                }
            } else {
                if ( ext.getJSON() != null ) {
                    b.setJSON(ext.getJSON());
                } else {
                    b.setJSON("{}");
                }
            }
            builder.addExtensions(b.build());            
        }

        // framework properties
        if ( ! feature.getFrameworkProperties().isEmpty() ) {
            final FeatureExtensionBuilder b = service.getBuilderFactory().newExtensionBuilder(FRAMEWORK_PROPERTIES_EXTENSION, Type.JSON, Kind.MANDATORY);
            final Dictionary<String, Object> properties = new Hashtable<>();
            feature.getFrameworkProperties().entrySet().stream().forEach(entry -> properties.put(entry.getKey(), entry.getValue()));
            try ( final Writer writer = new StringWriter()) {
                Configurations.buildWriter().build(writer).writeConfiguration(properties);
                writer.flush();
                b.setJSON(writer.toString());
            }
            builder.addExtensions(b.build());
        }
        
        // Write metadata for variables and framework properties in the internal extension
        final Hashtable<String, Object> output = Configurations.newConfiguration();
        if ( !feature.getFrameworkProperties().isEmpty() ) {
            final Map<String, Object> fwkMetadata = Configurations.newConfiguration();
            for(final String fwkPropName : feature.getFrameworkProperties().keySet()) {
                final Map<String, Object> metadata = feature.getFrameworkPropertyMetadata(fwkPropName);
                if ( !metadata.isEmpty() ) {
                    fwkMetadata.put(fwkPropName, metadata);
                }
            }
            if ( !fwkMetadata.isEmpty() ) {
                output.put(FRAMEWORK_PROPERTIES_METADATA, fwkMetadata);
            }
        }
        if ( !feature.getVariables().isEmpty() ) {
            final Map<String, Object> varMetadata = Configurations.newConfiguration();
            for(final String varName : feature.getVariables().keySet()) {
                final Map<String, Object> metadata = feature.getVariableMetadata(varName);
                if ( !metadata.isEmpty() ) {
                    varMetadata.put(varName, metadata);
                }
            }
            if ( !varMetadata.isEmpty() ) {
                output.put(VARIABLES_METADATA, varMetadata);
            }
        }
        if ( !output.isEmpty() ) {
            final FeatureExtensionBuilder b = service.getBuilderFactory().newExtensionBuilder(Extension.EXTENSION_NAME_INTERNAL_DATA, 
                    Type.JSON, Kind.OPTIONAL);
            try ( final Writer writer = new StringWriter()) {
                Configurations.buildWriter().build(writer).writeConfiguration(output);
                writer.flush();
                b.setJSON(writer.toString());
            }
            builder.addExtensions(b.build());
        }

        return builder.build();
    }

    /**
     * Convert an OSGi feature into an Apache Sling feature
     * @param feature The feature to convert
     * @return The Apache Sling feature or {@code null} if feature is {@code null}
     * @throws IOException If the conversion fails
     */
    public static org.apache.sling.feature.Feature convert(final Feature feature) throws IOException {
        if ( feature == null ) {
            return null;
        }
        final org.apache.sling.feature.Feature f = new org.apache.sling.feature.Feature(ArtifactId.parse(feature.getID().toString()));

        // metadata
        f.setComplete(feature.isComplete());
        f.setDescription(feature.getDescription().orElse(null));
        f.setLicense(feature.getLicense().orElse(null));
        f.setTitle(feature.getName().orElse(null));
        f.setVendor(feature.getVendor().orElse(null));
        f.setDocURL(feature.getDocURL().orElse(null));
        f.setSCMInfo(feature.getSCM().orElse(null));
        f.getCategories().addAll(feature.getCategories());

        // variables
        feature.getVariables().entrySet().stream().forEach(entry -> f.getVariables().put(entry.getKey(), entry.getValue().toString()));

        // bundles
        for(final FeatureBundle bundle : feature.getBundles()) {
            final Artifact b = new Artifact(ArtifactId.parse(bundle.getID().toString()));
            bundle.getMetadata().entrySet().stream().forEach(entry -> b.getMetadata().put(entry.getKey(), entry.getValue().toString()));
            f.getBundles().add(b);
        }

        // configurations
        for(final FeatureConfiguration cfg : feature.getConfigurations().values()) {
            final Configuration c = new Configuration(cfg.getPid());
            cfg.getValues().entrySet().stream().forEach(entry -> c.getProperties().put(entry.getKey(), entry.getValue()));
            f.getConfigurations().add(c);
        }

        // extensions
        for(final FeatureExtension ext : feature.getExtensions().values()) {
            if ( FRAMEWORK_PROPERTIES_EXTENSION.equals(ext.getName()) ) {
                // framework properties
                try ( final Reader reader = new StringReader(ext.getJSON())) {
                    Configurations.buildReader().build(reader).readConfiguration().entrySet()
                            .stream().forEach(entry -> f.getFrameworkProperties().put(entry.getKey(), entry.getValue().toString()));
                }
            } else if ( Extension.EXTENSION_NAME_INTERNAL_DATA.equals(ext.getName()) ) {
                // metadata for variables and framework properties
                try ( final Reader reader = new StringReader(ext.getJSON())) {
                    final Hashtable<String, Object> md = Configurations.buildReader().build(reader).readConfiguration();

                    final String varMetadata = (String) md.get(VARIABLES_METADATA);
                    if ( varMetadata != null ) {
                        try ( final StringReader r = new StringReader(varMetadata)) {
                            for(final Map.Entry<String, Hashtable<String, Object>> entry : Configurations.buildReader()
                                    .verifyAsBundleResource(true)
                                    .build(r)
                                    .readConfigurationResource().getConfigurations().entrySet()) {
                                f.getVariableMetadata(entry.getKey()).putAll(entry.getValue());
                            }
                        }
                    }
        
                    final String fwkMetadata = (String) md.get(FRAMEWORK_PROPERTIES_METADATA);
                    if ( fwkMetadata != null ) {
                        try ( final StringReader r = new StringReader(fwkMetadata)) {
                            for(final Map.Entry<String, Hashtable<String, Object>> entry : Configurations.buildReader()
                                    .verifyAsBundleResource(true)
                                    .build(r)
                                    .readConfigurationResource().getConfigurations().entrySet()) {
                                f.getFrameworkPropertyMetadata(entry.getKey()).putAll(entry.getValue());
                            }
                        }
                    }
                }
            } else {
                ExtensionType type;
                if ( ext.getType() == Type.ARTIFACTS ) {
                    type = ExtensionType.ARTIFACTS;
                } else if ( ext.getType() == Type.TEXT ) {
                    type = ExtensionType.TEXT;
                } else {
                    type = ExtensionType.JSON;
                }
                ExtensionState state;
                if ( ext.getKind() == Kind.OPTIONAL ) {
                    state = ExtensionState.OPTIONAL;
                } else if ( ext.getKind() == Kind.MANDATORY ) {
                    state = ExtensionState.REQUIRED;
                } else {
                    state = ExtensionState.TRANSIENT;
                }
                final Extension e = new Extension(type, ext.getName(), state);
                if ( ext.getType() == Type.ARTIFACTS ) {
                    for(final FeatureArtifact artifact : ext.getArtifacts()) {
                        final Artifact a = new Artifact(ArtifactId.parse(artifact.getID().toString()));
                        artifact.getMetadata().entrySet().stream().forEach(entry -> a.getMetadata().put(entry.getKey(), entry.getValue().toString()));
                        e.getArtifacts().add(a);
                    }
                } else if ( ext.getType() == Type.TEXT ) {
                    e.setText(String.join("\n", ext.getText()));
                } else {
                    e.setJSON(ext.getJSON());
                }
                f.getExtensions().add(e);       
            }
        }
        return f;
    }
}
