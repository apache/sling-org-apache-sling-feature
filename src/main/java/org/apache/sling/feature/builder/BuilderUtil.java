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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Artifacts;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.MatchingRequirement;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;

/**
 * Utility methods for the builders
 */
class BuilderUtil {

    /** Used in override rule to have it apply to all artifacts. */
    static final String CATCHALL_OVERRIDE = "*:*:";

    /**
     * Merge the variables
     * @param targetFeature The target feature
     * @param sourceFeature The source feature
     * @param context Non-null map of overrides
     */
    static void mergeVariables(final Feature targetFeature, final Feature sourceFeature, final Map<String,String> overrides) {
        final Map<String,String> result = new LinkedHashMap<>();
        final Map<String, Map<String, Object>> metadata = new HashMap<>();
        // keep values from target features, no need to update origin
        for (Map.Entry<String, String> entry : targetFeature.getVariables().entrySet()) {
            result.put(entry.getKey(), overrides.containsKey(entry.getKey()) ? overrides.get(entry.getKey()) : entry.getValue());
            metadata.put(entry.getKey(), targetFeature.getVariableMetadata(entry.getKey()));
        }
        // merge in from source feature
        for (Map.Entry<String, String> entry : sourceFeature.getVariables().entrySet()) {
            final List<ArtifactId> targetIds = (metadata.containsKey(entry.getKey())) ?
                  new ArrayList<>(targetFeature.getFeatureOrigins(metadata.get(entry.getKey()))) : new ArrayList<>();
            targetIds.add(sourceFeature.getId());
            if ( overrides.containsKey(entry.getKey()) ) {
                result.put(entry.getKey(), overrides.get(entry.getKey()));
            } else {
                String value = entry.getValue();
                if (value != null) {
                    String targetValue = targetFeature.getVariables().get(entry.getKey());
                    if (targetValue != null) {
                        if (!value.equals(targetValue)) {
                            throw new IllegalStateException(String.format("Can't merge variable '%s' defined twice (as '%s' v.s. '%s') and not overridden.", entry.getKey(), value, targetValue));
                        }
                    } else {
                        result.put(entry.getKey(), value);
                    }
                } else if (!targetFeature.getVariables().containsKey(entry.getKey())) {
                    result.put(entry.getKey(), value);
                }
            }
            metadata.put(entry.getKey(), new LinkedHashMap<>(sourceFeature.getVariableMetadata(entry.getKey())));
            targetFeature.setFeatureOrigins(metadata.get(entry.getKey()), targetIds);
        }
        targetFeature.getVariables().clear();
        targetFeature.getVariables().putAll(result);
        for(final Map.Entry<String, Map<String, Object>> entry : metadata.entrySet()) {
            targetFeature.getVariableMetadata(entry.getKey()).putAll(entry.getValue());
        }
    }

    /**
     * Merge bundles from source into target
     *
     * @param target            The target bundles
     * @param source            The source bundles
     * @param sourceFeature     Optional, if set origin will be recorded
     * @param artifactOverrides Artifact override instructions
     * @param originKey         An optional key used to track origins of merged
     *                          bundles
     * @throws IllegalStateException If bundles can't be merged, for example if no
     *                               override is specified for a clash.
     */
    static void mergeArtifacts(final Artifacts target,
        final Artifacts source,
        final Feature sourceFeature,
        final List<ArtifactId> artifactOverrides,
        final String originKey) {

        for (final Artifact artifactFromSource : source) {

            // set of artifacts in target, matching the artifact from source
            // the artifacts are kept in the order of the target - hence the linked hash set.
            final Set<Artifact> allExistingInTarget = new LinkedHashSet<>();
            for (final ArtifactId id : artifactFromSource.getAliases(true)) {
                for (Artifact targetArtifact : target) {
                    // Find aliased bundles in target
                    if (id.isSame(targetArtifact.getId())) {
                        allExistingInTarget.add(targetArtifact);
                    }
                }

                findAliasedArtifacts(id, target, allExistingInTarget);
            }

            final List<Artifact> selectedArtifacts = new ArrayList<>();
            if (allExistingInTarget.isEmpty()) {
                selectedArtifacts.add(artifactFromSource);
            }

            int insertPos = target.size();
            int count = 0;
            for (final Artifact existing : allExistingInTarget) {
                if (sourceFeature.getId().toMvnId().equals(existing.getMetadata().get(originKey))) {
                    // If the source artifact came from the same feature, keep them side-by-side
                    // but make sure we add the target ones first - hence, the count
                    selectedArtifacts.add(count++, existing);
                    selectedArtifacts.add(artifactFromSource);
                } else {
                    List<Artifact> artifacts = selectArtifactOverride(sourceFeature, existing, artifactFromSource, artifactOverrides, sourceFeature.getId());
                    // if we have an all policy we might have more then one artifact - we put the target one first
                    if (artifacts.size() > 1) {
                        selectedArtifacts.add(count++, artifacts.remove(0));
                    }
                    selectedArtifacts.addAll(artifacts);
                    Artifact same = null;
                    while ((same = target.getSame(existing.getId())) != null) {
                        // Keep executing removeSame() which ignores the version until last one was
                        // removed
                        final int p = target.indexOf(same);
                        if (p < insertPos) {
                            insertPos = p;
                        }
                        target.remove(p);
                    }
                }
            }

            for (final Artifact sa : new LinkedHashSet<>(selectedArtifacts)) {
                // create a copy to detach artifact from source
                final Artifact cp = addFeatureOrigin(sa.copy(sa.getId()), sourceFeature, sa);
                cp.getMetadata().put(BuilderUtil.class.getName() + "added", "true");
                // Record the original feature of the bundle, if needed
                if (originKey != null) {
                    if (sourceFeature != null && source.contains(sa) && sa.getMetadata().get(originKey) == null) {
                        cp.getMetadata().put(originKey, sourceFeature.getId().toMvnId());
                    }
                }
                if (insertPos == target.size()) {
                    target.add(cp);
                    insertPos = target.size();
                } else {
                    target.add(insertPos, cp);
                    insertPos++;
                }
            }
        }

        for (Artifact artifact : target) {
            artifact.getMetadata().remove(BuilderUtil.class.getName() + "added");
        }
    }

    static List<Artifact> selectArtifactOverride(Artifact fromTarget, Artifact fromSource,
        List<ArtifactId> artifactOverrides) {
        return selectArtifactOverride(null, fromTarget, fromSource, artifactOverrides, null);
    }

    static List<Artifact> selectArtifactOverride(Feature source, Artifact fromTarget, Artifact fromSource,
            List<ArtifactId> artifactOverrides, ArtifactId sourceID) {
        if (fromTarget.getId().equals(fromSource.getId())) {
            // They're the same so return the source (latest)
            return Collections.singletonList(addFeatureOrigin(selectStartOrder(fromTarget, fromSource, fromSource), source, fromSource, fromTarget));
        }

        final Set<ArtifactId> commonPrefixes = getCommonArtifactIds(fromTarget, fromSource);
        if (commonPrefixes.isEmpty()) {
            throw new IllegalStateException(
                    "Internal error selecting override. No common prefix between " + fromTarget + " and " + fromSource);
        }

        final List<Artifact> result = new ArrayList<>();
        if (artifactOverrides.isEmpty() && fromTarget.getMetadata().containsKey(BuilderUtil.class.getName() + "added")) {
            result.add(fromTarget);
            result.add(addFeatureOrigin(fromSource, source, fromSource));
            return result;
        }
        outer: for (ArtifactId prefix : commonPrefixes) {
            for (final ArtifactId override : artifactOverrides) {
                if (match(prefix, override)) {
                    String rule = override.getVersion();

                    if (BuilderContext.VERSION_OVERRIDE_ALL.equalsIgnoreCase(rule)) {
                        result.add(fromTarget);
                        result.add(fromSource);
                    } else if (BuilderContext.VERSION_OVERRIDE_HIGHEST.equalsIgnoreCase(rule)) {
                        Version a1v = fromTarget.getAliases(true).stream().filter(prefix::isSame).findFirst().get().getOSGiVersion();
                        Version a2v = fromSource.getAliases(true).stream().filter(prefix::isSame).findFirst().get().getOSGiVersion();
                        result.add(
                            addFeatureOrigin(
                                selectStartOrder(fromTarget, fromSource, a1v.compareTo(a2v) > 0 ? fromTarget : fromSource),
                                source, fromSource, fromTarget));
                    } else if (BuilderContext.VERSION_OVERRIDE_FIRST.equalsIgnoreCase(rule)) {
                        result.add(addFeatureOrigin(selectStartOrder(fromTarget, fromSource, fromTarget), source, fromSource, fromTarget));
                    } else if (BuilderContext.VERSION_OVERRIDE_LATEST.equalsIgnoreCase(rule)) {
                        result.add(addFeatureOrigin(selectStartOrder(fromTarget, fromSource, fromSource), source, fromSource, fromTarget));
                    } else {

                        // The rule must represent a version
                        // See if its one of the existing artifact. If so use those, as they may have
                        // additional metadata
                        if (fromTarget.getId().getVersion().equals(rule)) {
                            result.add(addFeatureOrigin(selectStartOrder(fromTarget, fromSource, fromTarget), source, fromSource, fromTarget));
                        } else if (fromSource.getId().getVersion().equals(rule)) {
                            result.add(addFeatureOrigin(selectStartOrder(fromTarget, fromSource, fromSource), source, fromSource, fromTarget));
                        } else {
                            // It's a completely new artifact
                            result.add(addFeatureOrigin(selectStartOrder(fromTarget, fromSource, new Artifact(override)), source, fromSource, fromTarget));
                        }
                    }
                    break outer;
                }
            }
        }
        if (!result.isEmpty()) {
            return result;
        }

        throw new IllegalStateException("Artifact override rule required to select between these two artifacts " +
                fromTarget + " and " + fromSource + ". The rule must be specified for " + commonPrefixes);
    }

    private static Artifact selectStartOrder(Artifact a, Artifact b, Artifact target) {
        int startOrderA = a.getStartOrder();
        int startOrderB = b.getStartOrder();
        int startOrderNew;
        if (startOrderA == 0) {
            startOrderNew = startOrderB;
        } else if (startOrderB == 0) {
            startOrderNew = startOrderA;
        } else if (startOrderA < startOrderB) {
            startOrderNew = startOrderA;
        } else {
            startOrderNew = startOrderB;
        }
        if (startOrderNew != target.getStartOrder()) {
            Artifact result = target.copy(target.getId());
            result.setStartOrder(startOrderNew);
            return result;
        }
        else {
            return target;
        }
    }

    private static Artifact addFeatureOrigin(Artifact result, Feature sourceFeature, Artifact sourceArtifact, Artifact targetArtifact) {
        LinkedHashSet<ArtifactId> originFeatures = new LinkedHashSet<>();

        List<ArtifactId> targetOrigins = Arrays.asList(targetArtifact.getFeatureOrigins());
        originFeatures.addAll(targetOrigins);

        List<ArtifactId> sourceOrigings = Arrays.asList(sourceArtifact.getFeatureOrigins());
        if (sourceFeature != null && sourceOrigings.isEmpty()) {
            originFeatures.add(sourceFeature.getId());
        }
        else {
            originFeatures.addAll(sourceOrigings);
        }

        ArtifactId[] origins = originFeatures.toArray(new ArtifactId[0]);
        if (Arrays.equals(origins,result.getFeatureOrigins())) {
            return result;
        }
        else {
            result = result.copy(result.getId());
            result.setFeatureOrigins(origins);
            return result;
        }
    }

    private static Artifact addFeatureOrigin(Artifact result, Feature sourceFeature, Artifact sourceArtifact) {
        LinkedHashSet<ArtifactId> originFeatures = new LinkedHashSet<>();

        List<ArtifactId> sourceOrigins = Arrays.asList(sourceArtifact.getFeatureOrigins());
        if (sourceFeature != null && sourceOrigins.isEmpty()) {
            originFeatures.add(sourceFeature.getId());
        }
        else if (!sourceOrigins.isEmpty()){
            originFeatures.addAll(sourceOrigins);
        }

        ArtifactId[] origins = originFeatures.toArray(new ArtifactId[0]);
        if (Arrays.equals(origins,result.getFeatureOrigins())) {
            return result;
        }
        else {
            result = result.copy(result.getId());
            result.setFeatureOrigins(origins);
            return result;
        }
    }

    private static boolean match(final ArtifactId id, final ArtifactId override) {
        int matchCount = 0;
        // check group id
        if (BuilderContext.COORDINATE_MATCH_ALL.equals(override.getGroupId())) {
            matchCount++;
        } else if (id.getGroupId().equals(override.getGroupId())) {
            matchCount++;
        }
        // check artifact id
        if (BuilderContext.COORDINATE_MATCH_ALL.equals(override.getArtifactId())) {
            matchCount++;
        } else if (id.getArtifactId().equals(override.getArtifactId())) {
            matchCount++;
        }
        // check type
        if (BuilderContext.COORDINATE_MATCH_ALL.equals(override.getType())) {
            matchCount++;
        } else if (id.getType().equals(override.getType())) {
            matchCount++;
        }
        // check classifier
        if (BuilderContext.COORDINATE_MATCH_ALL.equals(override.getClassifier())) {
            matchCount++;
        } else if (Objects.equals(id.getClassifier(), override.getClassifier())) {
            matchCount++;
        }
        return matchCount == 4;
    }

    private static boolean match(final Configuration config, String override) {
        boolean result;
        if (override.equals("*")) {
            result = true;
        }
        else if (Configuration.isFactoryConfiguration(config.getPid())) {
            if (Configuration.isFactoryConfiguration(override)) {
                String configPID = Configuration.getFactoryPid(config.getPid());
                String overridePID = Configuration.getFactoryPid(override);
                if (match(configPID, overridePID)) {
                    String configName = Configuration.getName(config.getPid());
                    String overrideName = Configuration.getName(override);
                    result = match(configName, overrideName);
                }
                else {
                    result = false;
                }
            }
            else {
                result = false;
            }
        }
        else {
            result = match(config.getPid(), override);
        }
        return result;
    }

    private static boolean match(String value, String override) {
        boolean result;
        if (override.endsWith("*")) {
            override = override.substring(0, override.length() - 1);
            result = value.startsWith(override);
        }
        else {
            result = value.equals(override);
        }
        return result;
    }
    private static Set<ArtifactId> getCommonArtifactIds(Artifact a1, Artifact a2) {
        final Set<ArtifactId> result = new HashSet<>();
        for (final ArtifactId id : a1.getAliases(true)) {
            for (final ArtifactId c : a2.getAliases(true)) {
                if (id.isSame(c)) {
                    result.add(id);
                }
            }
        }

        return result;
    }

    private static void findAliasedArtifacts(final ArtifactId id, final Artifacts targetBundles,
            final Set<Artifact> result) {
        for (final Artifact a : targetBundles) {
            for (final ArtifactId aid : a.getAliases(false)) {
                if (aid.isSame(id)) {
                    result.add(a);
                }
            }
        }
    }

    // configurations - merge / override
    static void mergeConfigurations(final Configurations target, 
        final Configurations source, 
        final Map<String, String> overrides,
        final ArtifactId sourceFeatureId) {

        for(final Configuration cfg : source) {
            final List<ArtifactId> sourceOrigins = cfg.getFeatureOrigins().isEmpty() ? Collections.singletonList(sourceFeatureId) : cfg.getFeatureOrigins();
            Configuration found = target.getConfiguration(cfg.getPid());

            if ( found != null ) {
                boolean handled = false;
                outer:
                for (Map.Entry<String, String> override : overrides.entrySet()) {
                    if (match(cfg, override.getKey())) {
                        if (BuilderContext.CONFIG_USE_LATEST.equals(override.getValue())) {
                            int idx = target.indexOf(found);
                            target.remove(found);
                            found = cfg.copy(cfg.getPid());
                            target.add(idx, found);
                            setPropertyFeatureOrigins(found, sourceFeatureId);
                            handled = true;
                        } else if (BuilderContext.CONFIG_FAIL_ON_PROPERTY_CLASH.equals(override.getValue())){
                            final List<ArtifactId> origins = found.getFeatureOrigins();
                            for (Enumeration<String> i = cfg.getProperties().keys(); i.hasMoreElements(); ) {
                                final String key = i.nextElement();
                                if (found.getProperties().get(key) != null) {
                                    break outer;
                                } else {
                                    found.getProperties().put(key, cfg.getProperties().get(key));
                                    cfg.setFeatureOrigins(key, cfg.getFeatureOrigins(key, sourceFeatureId));
                                }
                            }
                            // restore origin
                            found.setFeatureOrigins(origins);
                            handled = true;
                        } else if (BuilderContext.CONFIG_MERGE_LATEST.equals(override.getValue())) {
                            final List<ArtifactId> origins = found.getFeatureOrigins();
                            for (Enumeration<String> i = cfg.getProperties().keys(); i.hasMoreElements(); ) {
                                final String key = i.nextElement();
                                found.getProperties().put(key, cfg.getProperties().get(key));
                                final List<ArtifactId> propOrigins = new ArrayList<>(found.getFeatureOrigins(key));
                                propOrigins.addAll(cfg.getFeatureOrigins(key, sourceFeatureId));
                                found.setFeatureOrigins(key, propOrigins);
                            }
                            // restore origin
                            found.setFeatureOrigins(origins);
                            handled = true;
                        } else if (BuilderContext.CONFIG_USE_FIRST.equals(override.getValue())) {
                            // no need to update property origins
                            handled = true;
                            found = null;
                        } else if (BuilderContext.CONFIG_MERGE_FIRST.equals(override.getValue())) {
                            final List<ArtifactId> origins = found.getFeatureOrigins();
                            for (Enumeration<String> i = cfg.getProperties().keys(); i.hasMoreElements(); ) {
                                final String key = i.nextElement();
                                if (found.getProperties().get(key) == null) {
                                    found.getProperties().put(key, cfg.getProperties().get(key));
                                    cfg.setFeatureOrigins(key, cfg.getFeatureOrigins(key, sourceFeatureId));
                                }
                            }
                            // restore origin
                            found.setFeatureOrigins(origins);
                            handled = true;
                        }
                        break outer;
                    }
                }
                if (!handled) {
                    throw new IllegalStateException("Configuration override rule required to select between configurations for " +
                        cfg.getPid());
                }
            } else {
                // create new configuration
                found = cfg.copy(cfg.getPid());
                target.add(found);
                setPropertyFeatureOrigins(found, sourceFeatureId);
                if ( !found.getFeatureOrigins().isEmpty() ) {
                    found = null;
                }
            }
            if ( found != null ) {
                // update origin
                final List<ArtifactId> origins = new ArrayList<>(found.getFeatureOrigins());
                origins.addAll(sourceOrigins);
                found.setFeatureOrigins(origins);    
            }
        }
    }

    private static void setPropertyFeatureOrigins(final Configuration cfg, final ArtifactId sourceFeatureId) {
        for(final String propName : Collections.list(cfg.getConfigurationProperties().keys())) {
            cfg.setFeatureOrigins(propName, cfg.getFeatureOrigins(propName, sourceFeatureId));
        }
    }

   /**
     * Merge the framework properties
     * @param targetFeature The target feature
     * @param sourceFeature The source feature
     * @param context Non-null map of overrides
     */
    static void mergeFrameworkProperties(final Feature targetFeature, final Feature sourceFeature, final Map<String,String> overrides) {
        final Map<String,String> result = new LinkedHashMap<>();
        final Map<String, Map<String, Object>> metadata = new HashMap<>();
        // keep values from target features, no need to update origin
        for (Map.Entry<String, String> entry : targetFeature.getFrameworkProperties().entrySet()) {
            result.put(entry.getKey(), overrides.containsKey(entry.getKey()) ? overrides.get(entry.getKey()) : entry.getValue());
            metadata.put(entry.getKey(), targetFeature.getFrameworkPropertyMetadata(entry.getKey()));
        }
        // merge in from source feature
        for (Map.Entry<String, String> entry : sourceFeature.getFrameworkProperties().entrySet()) {
            final List<ArtifactId> targetIds = (metadata.containsKey(entry.getKey())) ?
                  new ArrayList<>(targetFeature.getFeatureOrigins(metadata.get(entry.getKey()))) : new ArrayList<>();
            targetIds.add(sourceFeature.getId());
            if ( overrides.containsKey(entry.getKey()) ) {
                result.put(entry.getKey(), overrides.get(entry.getKey()));
            } else {
                String value = entry.getValue();
                if (value != null) {
                    String targetValue = targetFeature.getFrameworkProperties().get(entry.getKey());
                    if (targetValue != null) {
                        if (!value.equals(targetValue)) {
                            throw new IllegalStateException(String.format("Can't merge framework property '%s' defined twice (as '%s' v.s. '%s') and not overridden.", entry.getKey(), value, targetValue));
                        }
                    } else {
                        result.put(entry.getKey(), value);
                    }
                } else if (!targetFeature.getFrameworkProperties().containsKey(entry.getKey())) {
                    result.put(entry.getKey(), value);
                }
            }
            metadata.put(entry.getKey(), new HashMap<>(sourceFeature.getFrameworkPropertyMetadata(entry.getKey())));
            targetFeature.setFeatureOrigins(metadata.get(entry.getKey()), targetIds);
        }
        targetFeature.getFrameworkProperties().clear();
        targetFeature.getFrameworkProperties().putAll(result);
        for(final Map.Entry<String, Map<String, Object>> entry : metadata.entrySet()) {
            targetFeature.getFrameworkPropertyMetadata(entry.getKey()).putAll(entry.getValue());
        }
    }

    // requirements (add)
    static void mergeRequirements(final List<MatchingRequirement> target, final List<MatchingRequirement> source) {
        for (final MatchingRequirement req : source) {
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

    /**
     * Merge an extension from source into target
     *
     * @param target             The target extension
     * @param source             The source extension
     * @param sourceFeature      Optional, if set origin will be recorded for artifacts
     * @param artifactOverrides  The merge algorithm for artifacts
     * @param originKey
     */
    static void mergeExtensions(final Extension target,
            final Extension source,
            final Feature sourceFeature,
            final List<ArtifactId> artifactOverrides,
            final String originKey) {
        switch ( target.getType() ) {
            case TEXT : // simply append
                target.setText((target.getText() != null && !target.getText().trim().isEmpty() ? (target.getText() + "\n") : "") + source.getText());
                break;
            case JSON : JsonStructure struct1;
                if (target.getJSON() != null) {
                    try (final StringReader reader = new StringReader(target.getJSON())) {
                        struct1 = Json.createReader(reader).read();
                    }
                    JsonStructure struct2;
                    try (final StringReader reader = new StringReader(source.getJSON())) {
                        struct2 = Json.createReader(reader).read();
                    }

                    if (struct1.getValueType() != struct2.getValueType()) {
                        throw new IllegalStateException("Found different JSON types for extension " + target.getName()
                                + " : " + struct1.getValueType() + " and " + struct2.getValueType());
                    }
                    if (struct1.getValueType() == ValueType.ARRAY) {
                        final JsonArrayBuilder builder = Json.createArrayBuilder();

                        Stream.concat(
                                ((JsonArray) struct1).stream(),
                                ((JsonArray) struct2).stream()
                        ).forEachOrdered(builder::add);

                        struct1 = builder.build();
                    } else {
                        // object is merge
                        struct1 = merge((JsonObject) struct1, (JsonObject) struct2);
                    }
                    StringWriter buffer = new StringWriter();
                    try (JsonWriter writer = Json.createWriter(buffer)) {
                        writer.write(struct1);
                    }
                    target.setJSON(buffer.toString());
                } else {
                    target.setJSON(source.getJSON());
                }
                break;

        case ARTIFACTS:
            mergeArtifacts(target.getArtifacts(), source.getArtifacts(), sourceFeature, artifactOverrides, originKey);
            break;
        }
    }

    // extensions (add/merge)
    static void mergeExtensions(final Feature target,
            final Feature source,
            final BuilderContext context,
            final List<ArtifactId> artifactOverrides,
            final String originKey,
            final boolean prototypeMerge,
            final boolean initialMerge) {
        for(final Extension ext : source.getExtensions()) {
            boolean found = false;

            // Make a defensive copy of the extensions, as the handlers may modify the extensions on the target
            for(final Extension current : new ArrayList<>(target.getExtensions())) {
                if ( current.getName().equals(ext.getName()) ) {
                    found = true;
                    if ( current.getType() != ext.getType() ) {
                        throw new IllegalStateException("Found different types for extension " + current.getName()
                            + " : " + current.getType() + " and " + ext.getType());
                    }
                    boolean handled = false;
                    for(final MergeHandler me : context.getMergeExtensions()) {
                        if ( me.canMerge(current) ) {
                            me.merge(new HandlerContextImpl(context, me, prototypeMerge, initialMerge), target, source, current, ext);
                            handled = true;
                            break;
                        }
                    }
                    if ( !handled ) {
                        // default merge
                        mergeExtensions(current, ext, source, artifactOverrides, originKey);
                    }
                }
            }
            if ( !found ) {
                // The extension isn't found in the target, still call merge to allow handlers to operate on the
                // first extension being aggregated
                boolean handled = false;
                for (final MergeHandler mh : context.getMergeExtensions()) {
                    if (mh.canMerge(ext)) {
                        mh.merge(new HandlerContextImpl(context, mh, prototypeMerge, initialMerge), target, source, null, ext);
                        handled = true;
                        break;
                    }
                }
                if ( !handled ) {
                    // no merge handler, just merge with an empty target
                    Extension current = new Extension(ext.getType(), ext.getName(), ext.getState());
                    target.getExtensions().add(current);
                    mergeExtensions(current, ext, source, artifactOverrides, originKey);
                }
            }
        }

        // post processing
        // Make a defensive copy of the extensions, as the handlers may modify the extensions on the target
        for(final Extension ext : new ArrayList<>(target.getExtensions())) {
            for(final PostProcessHandler ppe : context.getPostProcessExtensions()) {
                ppe.postProcess(new HandlerContextImpl(context, ppe, prototypeMerge, initialMerge), target, ext);
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

    static class HandlerContextImpl implements HandlerContext {

        private final ArtifactProvider artifactProvider;
        
        private final Map<String,String> configuration;

        private final boolean prototypeMerge;

        private final boolean initialMerge;

        HandlerContextImpl(BuilderContext bc, MergeHandler handler, final boolean prototype, final boolean initial) {
            artifactProvider = bc.getArtifactProvider();
            configuration = getHandlerConfiguration(bc, handler);
            this.prototypeMerge = prototype;
            this.initialMerge = initial;
        }

        HandlerContextImpl(BuilderContext bc, PostProcessHandler handler, final boolean prototype, final boolean initial) {
            artifactProvider = bc.getArtifactProvider();
            configuration = getHandlerConfiguration(bc, handler);
            this.prototypeMerge = prototype;
            this.initialMerge = initial;
        }

        private Map<String,String> getHandlerConfiguration(BuilderContext bc, Object handler) {
            final Map<String,String> result = new HashMap<>();

            Map<String, String> overall = bc.getHandlerConfigurations()
                    .get(BuilderContext.CONFIGURATION_ALL_HANDLERS_KEY);
            if (overall != null)
                result.putAll(overall);
            final String name = getHandlerName(handler);
            if (name != null) {
                Map<String, String> handlerSpecific = bc.getHandlerConfigurations().get(name);
                if (handlerSpecific != null)
                    result.putAll(handlerSpecific);
            }
            return result;
        }

        private static String getHandlerName(Object handler) {
            return handler.getClass().getSimpleName();
        }

        @Override
        public ArtifactProvider getArtifactProvider() {
            return artifactProvider;
        }

        @Override
        public Map<String,String> getConfiguration() {
            return configuration;
        }

		@Override
		public boolean isInitialMerge() {
			return this.initialMerge;
		}

		@Override
		public boolean isPrototypeMerge() {
			return this.prototypeMerge;
		}
    }
}
