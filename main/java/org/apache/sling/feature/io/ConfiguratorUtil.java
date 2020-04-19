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

import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

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
        try (JsonGenerator generator = newGenerator(writer)) {
            generator.writeStartObject();
            writeConfiguration(generator, props);
            generator.writeEnd();
        }
    }

    public static void writeConfiguration(final JsonGenerator generator, final Dictionary<String, Object> props) {
        final Enumeration<String> e = props.keys();
        while (e.hasMoreElements()) {
            final String name = e.nextElement();
            if (Configuration.PROP_ARTIFACT_ID.equals(name)) {
                continue;
            }
            final Object val = props.get(name);
            writeConfigurationProperty(generator, name, val);
        }
    }

    private static void writeConfigurationProperty(JsonGenerator generator, String name, Object val) {
        String dataType = getDataType(val);
        writeConfigurationProperty(generator, name, dataType, val);
    }

    private static void writeConfigurationProperty(JsonGenerator generator, String name, String dataType, Object val) {
        String nameWithDataPostFix = name;
        if (dataType != null) {
            nameWithDataPostFix += ":" + dataType;
        }
        if (val.getClass().isArray()) {
            generator.writeStartArray(nameWithDataPostFix);
            for (int i = 0; i < Array.getLength(val); i++) {
                writeArrayItem(generator, Array.get(val, i));
            }
            generator.writeEnd();
        } else if (val instanceof Collection) {
            generator.writeStartArray(nameWithDataPostFix);
            for (Object item : Collection.class.cast(val)) {
                writeArrayItem(generator, item);
            }
            generator.writeEnd();
        } else {
            writeNameValuePair(generator, nameWithDataPostFix, val);
        }
    }

    private static void writeNameValuePair(JsonGenerator generator, String name, Object item) {
        if (item instanceof Boolean) {
            generator.write(name, (Boolean) item);
        } else if (item instanceof Long || item instanceof Integer || item instanceof Byte || item instanceof Short) {
            generator.write(name, ((Number)item).longValue());
        } else if (item instanceof Double) {
            generator.write(name, (Double) item);
        } else if (item instanceof Float) {
            generator.write(name, (Float) item);
        } else {
            generator.write(name, item.toString());
        }
    }

    private static void writeArrayItem(JsonGenerator generator, Object item) {
        if (item instanceof Boolean) {
            generator.write((Boolean) item);
        } else if (item instanceof Long || item instanceof Integer || item instanceof Byte || item instanceof Short) {
            generator.write(((Number)item).longValue());
        } else if (item instanceof Double) {
            generator.write((Double) item);
        } else if (item instanceof Float) {
            generator.write((Float) item);
        } else {
            generator.write(item.toString());
        }
    }

    private static String getDataType(Object object) {
        if (object instanceof Collection) {
            // check class of first item
            Iterator<?> it = ((Collection<?>) object).iterator();
            if (it.hasNext()) {
                Class<?> itemClass = it.next().getClass();
                return "Collection<" + getDataType(itemClass, false) + ">";
            } else {
                throw new IllegalStateException("Empty collections are invalid");
            }
        } else {
            return getDataType(object.getClass(), true);
        }
    }

    private static String getDataType(Class<?> clazz, boolean allowEmpty) {
        if (clazz.isArray()) {
            String dataType = getDataType(clazz.getComponentType(), false);
            if (dataType != null) {
                return dataType + "[]";
            } else {
                return null;
            }
        }
        // default classes used by native JSON types
        else if (clazz.isAssignableFrom(Boolean.class) || clazz.isAssignableFrom(boolean.class) || clazz.isAssignableFrom(Long.class) || clazz.isAssignableFrom(long.class) ||
                clazz.isAssignableFrom(Double.class) || clazz.isAssignableFrom(double.class) || clazz.isAssignableFrom(String.class)) {
            // no data type necessary except when being used in an array/collection
            if (!allowEmpty) {
                // for all other cases just use the simple name
                return clazz.getSimpleName();
            }

        } else if (clazz.isAssignableFrom(Integer.class) || clazz.isAssignableFrom(int.class) || clazz.isAssignableFrom(Float.class) || clazz.isAssignableFrom(float.class)
                || clazz.isAssignableFrom(Byte.class) || clazz.isAssignableFrom(byte.class) || clazz.isAssignableFrom(Short.class) || clazz.isAssignableFrom(short.class)
                || clazz.isAssignableFrom(Character.class) || clazz.isAssignableFrom(char.class)) {
            return clazz.getSimpleName();
        }
        if (!allowEmpty) {
            throw new IllegalStateException("Class does not have a valid type " + clazz);
        }
        return null;

    }
}
