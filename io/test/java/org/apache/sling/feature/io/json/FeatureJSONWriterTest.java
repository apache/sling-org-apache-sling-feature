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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.sling.feature.Feature;
import org.junit.Assert;
import org.junit.Test;

public class FeatureJSONWriterTest {

    @Test public void testWrite() throws Exception {
        final Feature f = U.readFeature("test");
        final Feature rf;
        try ( final StringWriter writer = new StringWriter() ) {
            FeatureJSONWriter.write(writer, f);
            try ( final StringReader reader = new StringReader(writer.toString()) ) {
                rf = FeatureJSONReader.read(reader, null);
            }
        }
        assertEquals(f.getId(), rf.getId());
        assertEquals("org.apache.sling:test-feature:1.1", rf.getId().toMvnId());
        assertEquals("The feature description", rf.getDescription());

        assertEquals(Arrays.asList("org.osgi.service.http.runtime.HttpServiceRuntime"),
                U.findCapability(rf.getCapabilities(), "osgi.service").getAttributes().get("objectClass"));
    }

    @Test public void testWrite2() throws Exception {
        final Feature f = U.readFeature("test2");

        final Feature rf;
        try ( final StringWriter writer = new StringWriter() ) {
            FeatureJSONWriter.write(writer, f);
            try ( final StringReader reader = new StringReader(writer.toString()) ) {
                rf = FeatureJSONReader.read(reader, null);
            }
        }

        assertEquals(f.getVariables(), rf.getVariables());
    }

    @Test public void testExtensionsWriteRead() throws Exception {
        final Feature f = U.readFeature("artifacts-extension");
        final Feature rf;
        try ( final StringWriter writer = new StringWriter() ) {
            FeatureJSONWriter.write(writer, f);
            try ( final StringReader reader = new StringReader(writer.toString()) ) {
                rf = FeatureJSONReader.read(reader, null);
            }
        }

        ArtifactsExtensions.testReadArtifactsExtensions(rf);
    }

    @Test public void testPrototypeWriteRead() throws Exception {
        final Feature f = U.readFeature("test");
        assertNotNull(f.getPrototype());

        final Feature rf;
        try ( final StringWriter writer = new StringWriter() ) {
            FeatureJSONWriter.write(writer, f);
            try ( final StringReader reader = new StringReader(writer.toString()) ) {
                rf = FeatureJSONReader.read(reader, null);
            }
        }
        assertEquals(f.getPrototype().getId(), rf.getPrototype().getId());
    }

    @Test public void testRepoInitWrite() throws Exception {
        final Feature f = U.readFeature("repoinit2");
        try ( final StringWriter writer = new StringWriter() ) {
            FeatureJSONWriter.write(writer, f);
            final JsonObject refJson = Json.createReader(
                    new InputStreamReader(U.class.getResourceAsStream("/features/repoinit2.json"))
                ).readObject();
            final JsonObject resultJson = Json.createReader(new StringReader(writer.toString())).readObject();

            JsonArray refJsonArray = refJson.getJsonArray("repoinit:TEXT|false");
            JsonArray resultJsonArray = resultJson.getJsonArray("repoinit:TEXT|false");
            Assert.assertEquals(refJsonArray, resultJsonArray);
        }
    }

    @Test
    public void testFinalFlag() throws Exception {
        // no final flag set in test feature
        final Feature featureA = U.readFeature("test");
        try (final StringWriter writer = new StringWriter()) {
            FeatureJSONWriter.write(writer, featureA);
            final JsonObject resultJson = Json.createReader(new StringReader(writer.toString())).readObject();

            assertNull(resultJson.get(JSONConstants.FEATURE_FINAL));
        }

        // final flag set in final feature
        final Feature featureB = U.readFeature("final");
        try (final StringWriter writer = new StringWriter()) {
            FeatureJSONWriter.write(writer, featureB);
            final JsonObject resultJson = Json.createReader(new StringReader(writer.toString())).readObject();

            final JsonValue val = resultJson.get(JSONConstants.FEATURE_FINAL);
            assertNotNull(val);
            assertEquals(ValueType.TRUE, val.getValueType());
        }
    }
}
