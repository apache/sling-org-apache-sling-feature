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

import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.KeyValueMap;

/**
 * Common functionality for writing JSON
 */
abstract class JSONWriterBase {

    private final JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    protected final JsonGenerator newGenerator(final Writer writer) {
        return generatorFactory.createGenerator(writer);
    }

    protected void writeBundles(final JsonGenerator generator,
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
                KeyValueMap md = artifact.getMetadata();
                if ( md.isEmpty() && cfgs.isEmpty() ) {
                    generator.write(artifact.getId().toMvnId());
                } else {
                    generator.writeStartObject();
                    generator.write(JSONConstants.ARTIFACT_ID, artifact.getId().toMvnId());

                    Object runmodes = md.remove("runmodes");
                    if (runmodes instanceof String) {
                        md.put("run-modes", (String) runmodes);
                    }

                    for(final Map.Entry<String, String> me : md) {
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
     * @param ob The json generator
     * @param cfgs The list of configurations
     */
    protected void writeConfigurations(final JsonGenerator generator, final Configurations cfgs) {
        if ( cfgs.isEmpty() ) {
            return;
        }

        generator.writeStartObject(JSONConstants.FEATURE_CONFIGURATIONS);

        for(final Configuration cfg : cfgs) {
            final String key;
            if ( cfg.isFactoryConfiguration() ) {
                key = cfg.getFactoryPid() + "~" + cfg.getName();
            } else {
                key = cfg.getPid();
            }

            generator.writeStartObject(key);

            final Enumeration<String> e = cfg.getProperties().keys();
            while ( e.hasMoreElements() ) {
                final String name = e.nextElement();
                if ( Configuration.PROP_ARTIFACT_ID.equals(name) ) {
                    continue;
                }

                final Object val = cfg.getProperties().get(name);

                String typePostFix = null;
                final Object typeCheck;
                if ( val.getClass().isArray() ) {
                    if ( Array.getLength(val) > 0 ) {
                        typeCheck = Array.get(val, 0);
                    } else {
                        typeCheck = null;
                    }
                } else {
                    typeCheck = val;
                }

                if ( typeCheck instanceof Integer ) {
                    typePostFix = ":Integer";
                } else if ( typeCheck instanceof Byte ) {
                    typePostFix = ":Byte";
                } else if ( typeCheck instanceof Character ) {
                    typePostFix = ":Character";
                } else if ( typeCheck instanceof Float ) {
                    typePostFix = ":Float";
                }

                if ( val.getClass().isArray() ) {
                    generator.writeStartArray(name);
                    for(int i=0; i<Array.getLength(val);i++ ) {
                        final Object obj = Array.get(val, i);
                        if ( typePostFix == null ) {
                            if ( obj instanceof String ) {
                                generator.write((String)obj);
                            } else if ( obj instanceof Boolean ) {
                                generator.write((Boolean)obj);
                            } else if ( obj instanceof Long ) {
                                generator.write((Long)obj);
                            } else if ( obj instanceof Double ) {
                                generator.write((Double)obj);
                            }
                        } else {
                            generator.write(obj.toString());
                        }
                    }

                    generator.writeEnd();
                } else {
                    if ( typePostFix == null ) {
                        if ( val instanceof String ) {
                            generator.write(name, (String)val);
                        } else if ( val instanceof Boolean ) {
                            generator.write(name, (Boolean)val);
                        } else if ( val instanceof Long ) {
                            generator.write(name, (Long)val);
                        } else if ( val instanceof Double ) {
                            generator.write(name, (Double)val);
                        }
                    } else {
                        generator.write(name + typePostFix, val.toString());
                    }
                }
            }

            generator.writeEnd();
        }

        generator.writeEnd();
    }

    protected void writeVariables(final JsonGenerator generator, final KeyValueMap vars) {
        if ( !vars.isEmpty()) {
            generator.writeStartObject(JSONConstants.FEATURE_VARIABLES);

            for (final Map.Entry<String, String> entry : vars) {
                String val = entry.getValue();
                if (val != null)
                    generator.write(entry.getKey(), val);
                else
                    generator.writeNull(entry.getKey());
            }

            generator.writeEnd();
        }
    }

    protected void writeFrameworkProperties(final JsonGenerator generator, final KeyValueMap props) {
        // framework properties
        if ( !props.isEmpty() ) {
            generator.writeStartObject(JSONConstants.FEATURE_FRAMEWORK_PROPERTIES);
            for(final Map.Entry<String, String> entry : props) {
                generator.write(entry.getKey(), entry.getValue());
            }
            generator.writeEnd();
        }
    }

    protected void writeExtensions(final JsonGenerator generator,
            final List<Extension> extensions,
            final Configurations allConfigs) {
        for(final Extension ext : extensions) {
            final String key = ext.getName() + ":" + ext.getType().name() + "|" + ext.isRequired();
            if ( ext.getType() == ExtensionType.JSON ) {
                final JsonStructure struct;
                try ( final StringReader reader = new StringReader(ext.getJSON()) ) {
                    struct = Json.createReader(reader).read();
                }
                generator.write(key, struct);
            } else if ( ext.getType() == ExtensionType.TEXT ) {
                generator.write(key, ext.getText());
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

                        for(final Map.Entry<String, String> me : artifact.getMetadata()) {
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
}
