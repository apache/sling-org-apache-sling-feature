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
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.felix.configurator.impl.json.TypeConverter;
import org.apache.felix.configurator.impl.model.ConfigurationFile;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Test;

public class ConfigurationJSONWriterTest {

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
        ConfigurationJSONWriter.writeConfiguration(writer, props);
        writer.close();
        assertConfigurationJson(writer.toString(), props);
    }

    protected void assertConfigurationJson(String json, Dictionary<String, Object> expectedProps) throws MalformedURLException {
        final JSONUtil.Report report = new JSONUtil.Report();
        StringBuilder sb = new StringBuilder("{ \"");
        sb.append("myid");
        sb.append("\" : ");
        sb.append(json);
        sb.append("}");
        final ConfigurationFile configurationFile = JSONUtil.readJSON(new TypeConverter(null),"name", new URL("http://127.0.0.1/configtest"),
                0, sb.toString(), report);
        if ( !report.errors.isEmpty() || !report.warnings.isEmpty() ) {
            Assert.fail("JSON is not the right format: \nErrors: " + StringUtils.join(report.errors) + "\nWarnings: " + StringUtils.join(report.warnings));
        }
        Assert.assertThat(configurationFile.getConfigurations().get(0).getProperties(), new DictionaryMatcher(expectedProps));
    }
    
    public final static class DictionaryMatcher extends TypeSafeMatcher<Dictionary<String, Object>> {
        private final Dictionary<String, Object> expectedDictionary;

        public DictionaryMatcher(Dictionary<String, Object> dictionary) {
            this.expectedDictionary = dictionary;
        }
        
        @Override
        public void describeTo(Description description) {
            description.appendText("Dictionary with items: ").appendValueList("", ",", "", toString(expectedDictionary));
        }

        
        @Override
        protected void describeMismatchSafely(Dictionary<String, Object> item, Description mismatchDescription) {
            mismatchDescription.appendText("was Dictionary with items: ").appendValueList("", ",", "", toString(item));
        }

        static Iterable<String> toString(Dictionary<String, Object> dictionary) {
            List<String> itemList = new LinkedList<String>();
            Enumeration<String> e = dictionary.keys();
            while (e.hasMoreElements()) {
                final String key = e.nextElement();
                Object value = dictionary.get(key);
                // for arrays expand values
                final String type = value.getClass().getSimpleName();
                if (value.getClass().isArray()) {
                    value = ArrayUtils.toString(value);
                }
                StringBuilder sb = new StringBuilder();
                sb.append(key).append(":");
                sb.append(value);
                sb.append("(").append(type).append(")");
                sb.append(", ");
                itemList.add(sb.toString());
            }
            return itemList;
        }

        @Override
        protected boolean matchesSafely(Dictionary<String, Object> item) {
            return expectedDictionary.equals(item);
        }
        
    }
}
