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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import org.apache.felix.cm.json.Configurations;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.core.Every;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TypeReference;

public class ConfiguratorUtilTest {

    @Test
    public void testConfigurationWriteReadRoundtrip() throws IOException {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put("Integer-simple", 1);
        props.put("Integer-array", new Integer[]{1,2});
        props.put("Integer-list", Arrays.asList(1, 2));
        props.put("int-array", new int[]{1,2});
        props.put("Long-simple", 1);
        props.put("Long-array", new Long[]{1L,2L});
        props.put("Long-list", Arrays.asList(1l, 2l));
        props.put("long-array", new long[]{1,2});
        props.put("Boolean-simple", Boolean.TRUE);
        props.put("Boolean-array", new Boolean[] {Boolean.TRUE, Boolean.FALSE});
        props.put("Boolean-list", Arrays.asList(Boolean.TRUE, Boolean.FALSE));
        props.put("bool-array", new boolean[] {true, false});
        props.put("Float-simple", 1.0d);
        props.put("Float-array", new Float[]{1.0f, 2.0f});
        props.put("Float-list", Arrays.asList(1.0f, 2.0f));
        props.put("float-array", new float[]{1.0f,2.0f});
        props.put("Double-simple", 1.0d);
        props.put("Double-array", new Double[]{1.0d,2.0d});
        props.put("Double-list", Arrays.asList(1.0d, 2.0d));
        props.put("double-array", new double[]{1.0d,2.0d});
        props.put("Byte-simple", new Byte((byte)1));
        props.put("Byte-array", new Byte[]{1,2});
        props.put("Byte-list", Arrays.asList((byte)1, (byte)2));
        props.put("byte-array", new byte[]{1,2});
        props.put("Short-simple", new Short((short) 1));
        props.put("Short-array", new Short[]{1,2});
        props.put("Short-list", Arrays.asList((short)1, (short)2));
        props.put("Short-array", new short[]{1,2});
        props.put("Character-simple", 1);
        props.put("Character-array", new Character[]{'a','b'});
        props.put("Character-list", Arrays.asList('a', 'b'));
        props.put("char-array", new char[]{'a','b'});
        props.put("String-simple", "test");
        props.put("String-array", new String[]{"test1", "test2"});
        props.put("String-list", Arrays.asList("test1", "test2"));
        StringWriter writer = new StringWriter();
        ConfiguratorUtil.writeConfiguration(writer, props);
        writer.close();
        assertConfigurationJson(writer.toString(), props);
    }

    protected void assertConfigurationJson(String json, Dictionary<String, Object> expectedProps) throws IOException {
        final Hashtable<String, Object> readProps = Configurations.buildReader().verifyAsBundleResource(true).build(new StringReader(json)).readConfiguration();
        // convert to maps for easier comparison
        Converter converter = Converters.standardConverter();
        Map<String, Object> expectedPropsMap = converter.convert(expectedProps).to(new TypeReference<Map<String,Object>>(){});
        Map<String, Object> actualPropsMap = converter.convert(readProps).to(new TypeReference<Map<String,Object>>(){});
        Assert.assertThat(actualPropsMap.entrySet(), Every.everyItem(new MapEntryMatcher<>(expectedPropsMap)));
    }

    public static class MapEntryMatcher<K, V> extends TypeSafeDiagnosingMatcher<Map.Entry<K, V>> {

        private final Map<K,V> expectedMap;

        public MapEntryMatcher(Map<K, V> expectedMap) {
            this.expectedMap = expectedMap;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("contained in the expected map");
        }

        @Override
        protected boolean matchesSafely(Map.Entry<K, V> item, Description description) {
            if (expectedMap.get(item.getKey()) == null){
                description.appendText("key '" + item.getKey() + "' is not present");
                return false;
            } else {
                boolean isEqual;
                if (item.getValue().getClass().isArray()) {
                    isEqual = Objects.deepEquals(expectedMap.get(item.getKey()), item.getValue());

                } else {
                    isEqual = expectedMap.get(item.getKey()).equals(item.getValue());
                }
                if (!isEqual) {
                    description.appendText("has the wrong value for key '" + item.getKey() + "': Expected=" + expectedMap.get(item.getKey()) + " (" + expectedMap.get(item.getKey()).getClass() + ")" + ", Actual=" + item.getValue() + " (" + item.getValue().getClass() + ")");
                }
                return isEqual;
            }
        }
    }
}
