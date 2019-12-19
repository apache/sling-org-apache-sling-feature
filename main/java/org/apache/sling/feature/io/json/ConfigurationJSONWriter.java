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
import java.lang.reflect.Array;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.json.stream.JsonGenerator;

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;


/**
 * JSON writer for configurations
 */
public class ConfigurationJSONWriter extends JSONWriterBase {

    /**
     * Writes the configurations to the writer.
     * The writer is not closed.
     * @param writer Writer
     * @param configs List of configurations
     * @throws IOException If writing fails
     */
    public static void write(final Writer writer, final Configurations configs)
    throws IOException {
        final ConfigurationJSONWriter w = new ConfigurationJSONWriter();
        w.writeConfigurations(writer, configs);
    }

    private void writeConfigurations(final Writer writer, final Configurations configs)
    throws IOException {
        JsonGenerator generator = newGenerator(writer);
        writeConfigurations(generator, configs);
        generator.close();
    }

    /**
     * Write the OSGi configuration to a JSON structure as defined in <a href="https://osgi.org/specification/osgi.cmpn/7.0.0/service.configurator.html#d0e131765">OSGi Configurator Specification 1.0</a> 
     * @param generator The json generator
     * @param props The configuration properties to write
     */
    public static void writeConfiguration(final JsonGenerator generator, final Dictionary<String, Object> props) {
        final Enumeration<String> e = props.keys();
        while ( e.hasMoreElements() ) {
            final String name = e.nextElement();
            if ( Configuration.PROP_ARTIFACT_ID.equals(name) ) {
                continue;
            }
            final Object val = props.get(name);
            writeConfigurationProperty(generator, name, val);
        }
    }

    public static void writeConfigurationProperty(JsonGenerator generator, String name, Object val) {
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
}
