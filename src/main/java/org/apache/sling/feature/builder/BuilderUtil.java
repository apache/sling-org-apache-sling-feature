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
package org.apache.sling.feature.builder;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.JsonWriter;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.FeatureConstants;
import org.apache.sling.feature.KeyValueMap;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * Utility methods for the builders
 */
class BuilderUtil {

    enum ArtifactMerge {
        LATEST,
        HIGHEST
    }

    static boolean contains(String key, Iterable<Map.Entry<String, String>> iterable) {
        if (iterable != null) {
            for (Map.Entry<String, String> entry : iterable) {
                if (key.equals(entry.getKey())) {
                    return true;
                }
            }
        }
        return false;
    }

    static String get(String key, Iterable<Map.Entry<String, String>> iterable) {
        if (iterable != null) {
            for (Map.Entry<String, String> entry : iterable) {
                if (key.equals(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static void mergeWithContextOverwrite(String type, KeyValueMap target, KeyValueMap source, Iterable<Map.Entry<String,String>> context) {
        KeyValueMap result = new KeyValueMap();
        for (Map.Entry<String, String> entry : target) {
            result.put(entry.getKey(), contains(entry.getKey(), context) ? get(entry.getKey(), context) : entry.getValue());
        }
        for (Map.Entry<String, String> entry : source) {
            if (contains(entry.getKey(), context)) {
                result.put(entry.getKey(), get(entry.getKey(), context));
            }
            else {
                String value = source.get(entry.getKey());
                if (value != null) {
                    String targetValue = target.get(entry.getKey());
                    if (targetValue != null) {
                        if (!value.equals(targetValue)) {
                            throw new IllegalStateException(String.format("Can't merge %s '%s' defined twice (as '%s' v.s. '%s') and not overwritten.", type, entry.getKey(), value, targetValue));
                        }
                    }
                    else {
                        result.put(entry.getKey(), value);
                    }
                }
                else if (!contains(entry.getKey(), target)) {
                    result.put(entry.getKey(), value);
                }
            }
        }
        target.clear();
        target.putAll(result);
    }

    // variables
    static void mergeVariables(KeyValueMap target, KeyValueMap source, BuilderContext context) {
        mergeWithContextOverwrite("Variable", target, source, (null != context) ? context.getVariables() : null);
    }

    // bundles
    static void mergeBundles(final Bundles target,
        final Bundles source,
        final Feature originatingFeature,
        final ArtifactMerge artifactMergeAlg) {
        for(final Map.Entry<Integer, List<Artifact>> entry : source.getBundlesByStartOrder().entrySet()) {
            for(final Artifact a : entry.getValue()) {
                // Record the original feature of the bundle
                if (a.getMetadata().get(FeatureConstants.ARTIFACT_ATTR_ORIGINAL_FEATURE) == null) {
                    a.getMetadata().put(FeatureConstants.ARTIFACT_ATTR_ORIGINAL_FEATURE, originatingFeature.getId().toMvnId());
                }

                // version handling - use provided algorithm
                boolean replace = true;
                if ( artifactMergeAlg == ArtifactMerge.HIGHEST ) {
                    final Artifact existing = target.getSame(a.getId());
                    if ( existing != null && existing.getId().getOSGiVersion().compareTo(a.getId().getOSGiVersion()) > 0 ) {
                        replace = false;
                    }
                }
                if ( replace ) {
                    target.removeSame(a.getId());
                    target.add(a);
                }
            }
        }
    }

    // configurations - merge / override
    static void mergeConfigurations(final Configurations target, final Configurations source) {
        for(final Configuration cfg : source) {
            boolean found = false;
            for(final Configuration current : target) {
                if ( current.compareTo(cfg) == 0 ) {
                    found = true;
                    // merge / override properties
                    final Enumeration<String> i = cfg.getProperties().keys();
                    while ( i.hasMoreElements() ) {
                        final String key = i.nextElement();
                        current.getProperties().put(key, cfg.getProperties().get(key));
                    }
                    break;
                }
            }
            if ( !found ) {
                target.add(cfg);
            }
        }
    }

    // framework properties (add/merge)
    static void mergeFrameworkProperties(final KeyValueMap target, final KeyValueMap source, BuilderContext context) {
        mergeWithContextOverwrite("Property", target, source, context != null ? context.getProperties().entrySet() : null);
    }

    // requirements (add)
    static void mergeRequirements(final List<Requirement> target, final List<Requirement> source) {
        for(final Requirement req : source) {
            if ( !target.contains(req) ) {
                target.add(req);
            }
        }
    }

    // capabilities (add)
    static void mergeCapabilities(final List<Capability> target, final List<Capability> source) {
        for(final Capability cap : source) {
            if ( !target.contains(cap) ) {
                target.add(cap);
            }
        }
    }

    // default merge for extensions
    static void mergeExtensions(final Extension target,
        final Extension source,
        final ArtifactMerge artifactMergeAlg) {
        switch ( target.getType() ) {
            case TEXT : // simply append
                target.setText(target.getText() + "\n" + source.getText());
                break;
            case JSON : JsonStructure struct1;
                try ( final StringReader reader = new StringReader(target.getJSON()) ) {
                    struct1 = Json.createReader(reader).read();
                }
                JsonStructure struct2;
                try ( final StringReader reader = new StringReader(source.getJSON()) ) {
                    struct2 = Json.createReader(reader).read();
                }

                if ( struct1.getValueType() != struct2.getValueType() ) {
                    throw new IllegalStateException("Found different JSON types for extension " + target.getName()
                        + " : " + struct1.getValueType() + " and " + struct2.getValueType());
                }
                if ( struct1.getValueType() == ValueType.ARRAY ) {
                    final JsonArrayBuilder builder = Json.createArrayBuilder();

                    Stream.concat(
                        ((JsonArray) struct1).stream(),
                        ((JsonArray) struct2).stream()
                    ).forEachOrdered(builder::add);

                    struct1 = builder.build();
                } else {
                    // object is merge
                    struct1 = merge((JsonObject)struct1, (JsonObject)struct2);
                }
                StringWriter buffer = new StringWriter();
                try (JsonWriter writer = Json.createWriter(buffer))
                {
                    writer.write(struct1);
                }
                target.setJSON(buffer.toString());
                break;

            case ARTIFACTS : for(final Artifact a : source.getArtifacts()) {
                    // use artifactMergeAlg
                    boolean replace = true;
                    if ( artifactMergeAlg == ArtifactMerge.HIGHEST ) {
                         final Artifact existing = target.getArtifacts().getSame(a.getId());
                         if ( existing != null && existing.getId().getOSGiVersion().compareTo(a.getId().getOSGiVersion()) > 0 ) {
                            replace = false;
                         }
                    }
                    if ( replace ) {
                    target.getArtifacts().removeSame(a.getId());
                    target.getArtifacts().add(a);
                }
                }
                break;
        }
    }

    // extensions (add/merge)
    static void mergeExtensions(final Feature target,
        final Feature source,
        final ArtifactMerge artifactMergeAlg,
        final BuilderContext context) {
        for(final Extension ext : source.getExtensions()) {
            boolean found = false;
            for(final Extension current : target.getExtensions()) {
                if ( current.getName().equals(ext.getName()) ) {
                    found = true;
                    if ( current.getType() != ext.getType() ) {
                        throw new IllegalStateException("Found different types for extension " + current.getName()
                            + " : " + current.getType() + " and " + ext.getType());
                    }
                    boolean handled = false;
                    for(final MergeHandler me : context.getMergeExtensions()) {
                        if ( me.canMerge(current) ) {
                            me.merge(() -> context.getArtifactProvider(), target, source, current, ext);
                            handled = true;
                            break;
                        }
                    }
                    if ( !handled ) {
                        // default merge
                        mergeExtensions(current, ext, artifactMergeAlg);
                    }
                }
            }
            if ( !found ) {
                target.getExtensions().add(ext);
            }
        }
        // post processing
        for(final Extension ext : target.getExtensions()) {
            for(final PostProcessHandler ppe : context.getPostProcessExtensions()) {
                ppe.postProcess(() -> context.getArtifactProvider(), target, ext);
            }
        }
    }

    private static JsonObject merge(final JsonObject obj1, final JsonObject obj2) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (final Map.Entry<String, JsonValue> entry : obj1.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        for(final Map.Entry<String, JsonValue> entry : obj2.entrySet()) {
            if ( !obj1.containsKey(entry.getKey()) ) {
                builder.add(entry.getKey(), entry.getValue());
            } else {
                final JsonValue oldValue = obj1.get(entry.getKey());
                if ( oldValue.getValueType() != entry.getValue().getValueType() ) {
                    // new type wins
                    builder.add(entry.getKey(), entry.getValue());
                } else if ( oldValue.getValueType() == ValueType.ARRAY ) {
                    final JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

                    Stream.concat(
                        ((JsonArray) oldValue).stream(),
                        ((JsonArray)entry.getValue()).stream()
                    ).forEachOrdered(arrayBuilder::add);

                    builder.add(entry.getKey(), arrayBuilder.build());
                } else if ( oldValue.getValueType() == ValueType.OBJECT ) {
                    builder.add(entry.getKey(), merge((JsonObject)oldValue, (JsonObject)entry.getValue()));
                } else {
                    builder.add(entry.getKey(), entry.getValue());
                }
            }
        }
        return builder.build();
    }
}
