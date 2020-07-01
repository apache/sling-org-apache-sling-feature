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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionState;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.MatchingRequirement;
import org.apache.sling.feature.Prototype;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;

public abstract class FeatureBuilder {
    /** This key is used to track origins while a prototype is merged in */
    private static final String TRACKING_KEY = "tracking-key";

    /** Pattern for using variables. */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{[a-zA-Z0-9.\\-_]+\\}");

    /**
     * Assemble the full feature by processing its prototype.
     *
     * @param feature The feature to start
     * @param context The builder context
     * @return The assembled feature.
     * @throws IllegalArgumentException If feature or context is {@code null}
     * @throws IllegalStateException If a prototype feature can't be provided or merged.
     */
    public static Feature assemble(final Feature feature,
            final BuilderContext context) {
        if ( feature == null || context == null ) {
            throw new IllegalArgumentException("Feature and/or context must not be null");
        }
        return internalAssemble(new ArrayList<>(), feature, context);
    }

    /**
     * Resolve a set of features based on their ids.
     *
     * @param context The builder context
     * @param featureIds The feature ids
     * @return An array of features, the array has the same order as the provided ids
     * throws IllegalArgumentException If context or featureIds is {@code null}
     * throws IllegalStateException If the provided ids are invalid, or the feature can't be provided
     */
    public static Feature[] resolve(final BuilderContext context,
            final String... featureIds) {
        if ( featureIds == null || context == null ) {
            throw new IllegalArgumentException("Features and/or context must not be null");
        }

        final Feature[] features = new Feature[featureIds.length];
        int index = 0;
        for(final String id : featureIds) {
            features[index] = context.getFeatureProvider().provide(ArtifactId.parse(id));
            if ( features[index] == null ) {
                throw new IllegalStateException("Unable to find prototype feature " + id);
            }
            index++;
        }
        return features;
    }

    /**
     * Remove duplicate and prototype features.
     * If a feature with the same id but different version is contained several times,
     * only the one with the highest version is kept in the result list.
     * If a feature has another feature as prototype from the provided set, the prototype feature
     * is removed from the set.
     *
     * @param context The builder context
     * @param features A list of features
     * @return A list of features without duplicates.
     */
    public static Feature[] deduplicate(final BuilderContext context,
            final Feature... features) {
        if ( features == null || context == null ) {
            throw new IllegalArgumentException("Features and/or context must not be null");
        }

        // Remove duplicate features by selecting the one with the highest version
        final List<Feature> featureList = new ArrayList<>();
        for(final Feature f : features) {
            Feature found = null;
            for(final Feature s : featureList) {
                if ( s.getId().isSame(f.getId()) ) {
                    found = s;
                    break;
                }
            }
            boolean add = true;
            // feature with different version found
            if ( found != null ) {
                if ( f.getId().getOSGiVersion().compareTo(found.getId().getOSGiVersion()) <= 0 ) {
                    // higher version already included
                    add = false;
                } else {
                    // remove lower version, higher version will be added
                    featureList.remove(found);
                }
            }
            if ( add ) {
                featureList.add(f);
            }
        }

        // assemble each features
        final List<Feature> assembledFeatures = new ArrayList<>();
        final Set<ArtifactId> included = new HashSet<>();
        for(final Feature f : featureList) {
            final Feature assembled = FeatureBuilder.assemble(f, context.clone(new FeatureProvider() {

                @Override
                public Feature provide(final ArtifactId id) {
                    included.add(id);
                    for(final Feature f : features) {
                        if ( f.getId().equals(id) ) {
                            return f;
                        }
                    }
                    return context.getFeatureProvider().provide(id);
                }
            }));
            assembledFeatures.add(assembled);
        }

        // filter out included features
        final Iterator<Feature> iter = assembledFeatures.iterator();
        while ( iter.hasNext() ) {
            final Feature f = iter.next();
            if ( included.contains(f.getId())) {
                iter.remove();
            }
        }
        return assembledFeatures.toArray(new Feature[assembledFeatures.size()]);
    }

    /**
     * Assemble a feature based on the provided features.
     *
     * The features are processed in the order they are provided.
     * If the same feature is included more than once only the feature with
     * the highest version is used. The others are ignored.
     *
     * @param featureId The feature id to use.
     * @param context The builder context
     * @param features The features
     * @return The application
     * throws IllegalArgumentException If featureId, context or featureIds is {@code null}
     * throws IllegalStateException If a feature can't be provided
     */
    public static Feature assemble(
            final ArtifactId featureId,
            final BuilderContext context,
            final Feature... features) {
        if ( featureId == null || features == null || context == null ) {
            throw new IllegalArgumentException("Features and/or context must not be null");
        }

        final Feature target = new Feature(featureId);

        final Feature[] assembledFeatures = FeatureBuilder.deduplicate(context, features);

        // append feature list in extension
        final Extension list = new Extension(ExtensionType.ARTIFACTS, Extension.EXTENSION_NAME_ASSEMBLED_FEATURES,
                ExtensionState.TRANSIENT);
        for(final Feature feature : assembledFeatures) {
            list.getArtifacts().add(new Artifact(feature.getId()));
        }
        target.getExtensions().add(list);

        // assemble feature
        boolean targetIsComplete = true;
        for(final Feature assembled : assembledFeatures) {
            if (!assembled.isComplete()) {
                targetIsComplete = false;
            }

            merge(target, assembled, context, context.getArtifactOverrides(), context.getConfigOverrides(),null);
        }

        // check complete flag
        if (targetIsComplete) {
            target.setComplete(true);
        }

        target.setAssembled(true);

        return target;
    }

    /**
     * Resolve variables in the feature.
     * Variables are allowed in the values of framework properties and in the values of
     * configuration properties.
     * @param feature The feature
     * @param additionalVariables Optional additional variables
     */
    public static void resolveVariables(final Feature feature, final Map<String,String> additionalVariables) {
        for(final Configuration cfg : feature.getConfigurations()) {
        	final Set<String> keys = new HashSet<>(Collections.list(cfg.getProperties().keys()));
        	for(final String key : keys) {
                final Object value = cfg.getProperties().get(key);
                if ( value instanceof String ) {
                    cfg.getProperties().put(key, replaceVariables((String)value, additionalVariables, feature));
                } else if ( value instanceof String[]) {
                    final String[] values = (String[]) value;
                    for(int i=0;i<values.length;i++) {
                        values[i] = replaceVariables(values[i], additionalVariables, feature);
                    }
                    cfg.getProperties().put(key, values);
                }
            }
        }
        for(final Map.Entry<String, String> entry : feature.getFrameworkProperties().entrySet()) {
            // the  value is always a string
            entry.setValue(replaceVariables(entry.getValue(), additionalVariables, feature));
        }
    }

    /**
     * Substitute variables in the provided value. The variables must follow the
     * syntax ${variable_name} and are looked up in the provided variables and in
     * the feature variables. The provided variables are looked up first, potentially
     * overwriting variables defined in the feature.
     * If the provided value contains no variables, it will be returned as-is.
     *
     * @param value The value that can contain variables
     * @param additionalVariables The optional variables that can be substituted (might be {@code null})
     * @param feature The feature containing variables
     * @return The value with the variables substituted.
     */
    static String replaceVariables(final String value, final Map<String,String> additionalVariables, final Feature feature) {
        final String textWithVars = value;

        final Matcher m = VARIABLE_PATTERN.matcher(textWithVars.toString());
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String var = m.group();

            final int len = var.length();
            final String name = var.substring(2, len - 1);
            if (BuilderUtil.contains(name, feature.getVariables().entrySet())) {
                String val = null;
                if (additionalVariables != null)
                    val = BuilderUtil.get(name, additionalVariables.entrySet());
                if (val == null) {
                    val = feature.getVariables().get(name);
                }

                if (val != null) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(val));
                }
                else {
                    throw new IllegalStateException("Undefined variable: " + name);
                }
            }
        }
        m.appendTail(sb);

        return sb.toString();
    }

    private static Feature internalAssemble(final List<String> processedFeatures,
            final Feature feature,
            final BuilderContext context) {
        if ( feature.isAssembled() ) {
            return feature;
        }
        if ( processedFeatures.contains(feature.getId().toMvnId()) ) {
            throw new IllegalStateException("Recursive inclusion of " + feature.getId().toMvnId() + " via " + processedFeatures);
        }
        processedFeatures.add(feature.getId().toMvnId());

        // we copy the feature as we set the assembled flag on the result
        final Feature result = feature.copy();

        if ( result.getPrototype() != null) {
            // clear everything in the result, will be added in the process
            result.getVariables().clear();
            result.getBundles().clear();
            result.getFrameworkProperties().clear();
            result.getConfigurations().clear();
            result.getRequirements().clear();
            result.getCapabilities().clear();
            result.setPrototype(null);
            result.getExtensions().clear();

            final Prototype i = feature.getPrototype();

            final Feature f = context.getFeatureProvider().provide(i.getId());
            if ( f == null ) {
                throw new IllegalStateException("Unable to find prototype feature " + i.getId());
            }
            if (f.isFinal()) {
                throw new IllegalStateException(
                        "Prototype feature " + i.getId() + " is marked as final and can't be used in a prototype.");
            }
            final Feature prototypeFeature = internalAssemble(processedFeatures, f, context);

            // process prototype instructions
            processPrototype(prototypeFeature, i);

            // and now merge the prototype feature into the result. No overrides should be needed since the result is empty before
            merge(result, prototypeFeature, context, Collections.emptyList(), Collections.emptyMap(), TRACKING_KEY);

            // and merge the current feature over the prototype feature into the result
            merge(result, feature, context, Collections.singletonList(
                    ArtifactId.parse(BuilderUtil.CATCHALL_OVERRIDE + BuilderContext.VERSION_OVERRIDE_ALL)),
                    Collections.singletonMap("*", BuilderContext.CONFIG_MERGE_LATEST),
                    TRACKING_KEY);

            for (Artifact a : result.getBundles()) {
                a.getMetadata().remove(TRACKING_KEY);
                LinkedHashSet<ArtifactId> originList = new LinkedHashSet<>(Arrays.asList(a.getFeatureOrigins()));
                originList.remove(prototypeFeature.getId());
                originList.add(feature.getId());
                a.setFeatureOrigins(originList.toArray(new ArtifactId[0]));
            }
            for (Extension e : result.getExtensions()) {
                if (ExtensionType.ARTIFACTS == e.getType()) {
                    for (Artifact a : e.getArtifacts()) {
                        a.getMetadata().remove(TRACKING_KEY);
                    }
                }
            }
        }

        result.setAssembled(true);

        processedFeatures.remove(feature.getId().toMvnId());

        return result;
    }

    private static void merge(final Feature target,
            final Feature source,
            final BuilderContext context,
            final List<ArtifactId> artifactOverrides,
            final Map<String, String> configOverrides,
            final String originKey) {
        BuilderUtil.mergeVariables(target.getVariables(), source.getVariables(), context);
        BuilderUtil.mergeArtifacts(target.getBundles(), source.getBundles(), source, artifactOverrides, originKey);
        BuilderUtil.mergeConfigurations(target.getConfigurations(), source.getConfigurations(), configOverrides);
        BuilderUtil.mergeFrameworkProperties(target.getFrameworkProperties(), source.getFrameworkProperties(), context);
        BuilderUtil.mergeRequirements(target.getRequirements(), source.getRequirements());
        BuilderUtil.mergeCapabilities(target.getCapabilities(), source.getCapabilities());
        BuilderUtil.mergeExtensions(target, source, context, artifactOverrides, originKey);
    }

    /**
     * Process all the removals contained in the prototype
     *
     * @param feature The feature
     * @param prototype The prototype
     */
    private static void processPrototype(final Feature feature, final Prototype prototype) {
        // process bundles removals
        for (final ArtifactId a : prototype.getBundleRemovals()) {
            boolean removed = false;
            final boolean ignoreVersion = a.getOSGiVersion().equals(Version.emptyVersion);
            if ( ignoreVersion ) {
                // remove any version of that bundle
                while (feature.getBundles().removeSame(a)) {
                    // continue to remove
                    removed = true;
                }
            } else {
                // remove exact version
                removed = feature.getBundles().removeExact(a);
            }
            if ( !removed ) {
                throw new IllegalStateException("Bundle " + a + " can't be removed from feature " + feature.getId()
                        + " as it is not part of that feature.");
            }
            final Iterator<Configuration> iter = feature.getConfigurations().iterator();
            while ( iter.hasNext() ) {
                final Configuration cfg = iter.next();
                final String bundleId = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT_ID);
                if (bundleId != null) {
                    final ArtifactId bundleArtifactId = ArtifactId.fromMvnId(bundleId);
                    boolean remove = false;
                    if ( ignoreVersion ) {
                        remove = bundleArtifactId.isSame(a);
                    } else {
                        remove = bundleArtifactId.equals(a);
                    }
                    if (  remove) {
                        iter.remove();
                    }
                }
            }
        }

        // process configuration removals
        for (final String c : prototype.getConfigurationRemovals()) {
            final int attrPos = c.indexOf('@');
            final String pid = (attrPos == -1 ? c : c.substring(0, attrPos));
            final String attr = (attrPos == -1 ? null : c.substring(attrPos + 1));

            final Configuration found = feature.getConfigurations().getConfiguration(pid);
            if ( found != null ) {
                if ( attr == null ) {
                    feature.getConfigurations().remove(found);
                } else {
                    found.getProperties().remove(attr);
                }
            }
        }

        // process framework properties removals
        for (final String p : prototype.getFrameworkPropertiesRemovals()) {
            feature.getFrameworkProperties().remove(p);
        }

        // process extensions removals
        for (final String name : prototype.getExtensionRemovals()) {
            for (final Extension ext : feature.getExtensions()) {
                if ( ext.getName().equals(name) ) {
                    feature.getExtensions().remove(ext);
                    break;
                }
            }
        }
        // process artifact extensions removals
        for (final Map.Entry<String, List<ArtifactId>> entry : prototype.getArtifactExtensionRemovals().entrySet()) {
            for (final Extension ext : feature.getExtensions()) {
                if ( ext.getName().equals(entry.getKey()) ) {
                    for(final ArtifactId toRemove : entry.getValue() ) {
                        boolean removed = false;
                        final boolean ignoreVersion = toRemove.getOSGiVersion().equals(Version.emptyVersion);
                        final Iterator<Artifact> iter = ext.getArtifacts().iterator();
                        while ( iter.hasNext() ) {
                            final Artifact a = iter.next();

                            boolean remove = false;
                            if ( ignoreVersion ) {
                                // remove any version of that bundle
                                if ( a.getId().isSame(toRemove) ) {
                                    remove = true;
                                }
                            } else {
                                // remove exact version

                                remove = a.getId().equals(toRemove);
                            }
                            if ( remove ) {
                                iter.remove();
                                removed = true;
                            }
                            if ( remove && !ignoreVersion ) {
                                break;
                            }
                        }
                        if ( !removed ) {
                            throw new IllegalStateException("Artifact " + toRemove + " can't be removed from feature "
                                    + feature.getId() + " as it is not part of that feature.");
                        }
                    }
                    break;
                }
            }
        }

        // process requirement removals
        for (final MatchingRequirement req : prototype.getRequirementRemovals()) {
            feature.getRequirements().remove(req);
        }

        // process capability removals
        for (final Capability cap : prototype.getCapabilityRemovals()) {
            feature.getCapabilities().remove(cap);
        }
    }
}
