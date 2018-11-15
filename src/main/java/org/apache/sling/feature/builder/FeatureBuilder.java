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

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.FeatureConstants;
import org.apache.sling.feature.Include;
import org.osgi.framework.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FeatureBuilder {

    /** Pattern for using variables. */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{[a-zA-Z0-9.-_]+\\}");

    /**
     * Assemble the full feature by processing all includes.
     *
     * @param feature The feature to start
     * @param context The builder context
     * @return The assembled feature.
     * @throws IllegalArgumentException If feature or context is {@code null}
     * @throws IllegalStateException If an included feature can't be provided or merged.
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
                throw new IllegalStateException("Unable to find included feature " + id);
            }
            index++;
        }
        return features;
    }

    /**
     * Remove duplicate and included features.
     * If a feature with the same id but different version is contained several times,
     * only the one with the highest version is kept in the result list.
     * If a feature includes another feature from the provided set, the included feature
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

        final Set<ArtifactId> usedFeatures = new HashSet<>();

        // assemble feature
        boolean targetIsComplete = true;
        for(final Feature assembled : assembledFeatures) {
            if (!assembled.isComplete()) {
                targetIsComplete = false;
            }
            usedFeatures.add(assembled.getId());

            merge(target, assembled, context, context.getArtifactOverrides(), true);
        }

        // append feature list in extension
        final Extension list = new Extension(ExtensionType.ARTIFACTS, FeatureConstants.EXTENSION_NAME_ASSEMBLED_FEATURES, false);
        for(final ArtifactId id : usedFeatures) {
            list.getArtifacts().add(new Artifact(id));
        }
        target.getExtensions().add(list);

        // check complete flag
        if (targetIsComplete) {
            target.setComplete(true);
        }

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
                cfg.getProperties().put(key, replaceVariables(value, additionalVariables, feature));
            }
        }
        for(final Map.Entry<String, String> entry : feature.getFrameworkProperties().entrySet()) {
            // the  value is always a string
            entry.setValue((String)replaceVariables(entry.getValue(), additionalVariables, feature));
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
    static Object replaceVariables(final Object value, final Map<String,String> additionalVariables, final Feature feature) {
        if (!(value instanceof String)) {
            return value;
        }

        final String textWithVars = (String) value;

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

        if ( result.getInclude() != null) {
            // clear everything in the result, will be added in the process
            result.getVariables().clear();
            result.getBundles().clear();
            result.getFrameworkProperties().clear();
            result.getConfigurations().clear();
            result.getRequirements().clear();
            result.getCapabilities().clear();
            result.setInclude(null);
            result.getExtensions().clear();

            final Include i = feature.getInclude();

            final Feature f = context.getFeatureProvider().provide(i.getId());
            if ( f == null ) {
                throw new IllegalStateException("Unable to find included feature " + i.getId());
            }
            if (f.isFinal()) {
                throw new IllegalStateException(
                        "Included feature " + i.getId() + " is marked as final and can't be used in an include.");
            }
            final Feature includedFeature = internalAssemble(processedFeatures, f, context);

            // process include instructions
            processInclude(includedFeature, i);

            // and now merge
            merge(result, includedFeature, context, context.getArtifactOverrides(), true);
            merge(result, feature, context, context.getArtifactOverrides(), false);
        }
        processedFeatures.remove(feature.getId().toMvnId());

        result.setAssembled(true);
        return result;
    }

    private static void merge(final Feature target,
            final Feature source,
            final BuilderContext context,
            final List<String> artifactOverrides,
            final boolean recordOrigin) {
        BuilderUtil.mergeVariables(target.getVariables(), source.getVariables(), context);
        BuilderUtil.mergeBundles(target.getBundles(), source.getBundles(), recordOrigin ? source : null, artifactOverrides);
        BuilderUtil.mergeConfigurations(target.getConfigurations(), source.getConfigurations());
        BuilderUtil.mergeFrameworkProperties(target.getFrameworkProperties(), source.getFrameworkProperties(), context);
        BuilderUtil.mergeRequirements(target.getRequirements(), source.getRequirements());
        BuilderUtil.mergeCapabilities(target.getCapabilities(), source.getCapabilities());
        BuilderUtil.mergeExtensions(target, source, context, recordOrigin, artifactOverrides);
    }

    /**
     * Process the include statement Process all the removals contained in the
     * include
     *
     * @param feature The feature
     * @param include The include
     */
    private static void processInclude(final Feature feature, final Include include) {
        // process bundles removals
        for (final ArtifactId a : include.getBundleRemovals()) {
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

        // process configuration removals
        for (final String c : include.getConfigurationRemovals()) {
            final int attrPos = c.indexOf('@');
            final String val = (attrPos == -1 ? c : c.substring(0, attrPos));
            final String attr = (attrPos == -1 ? null : c.substring(attrPos + 1));

            final int sepPos = val.indexOf('~');
            Configuration found = null;
            if ( sepPos == -1 ) {
                found = feature.getConfigurations().getConfiguration(val);

            } else {
                final String factoryPid = val.substring(0, sepPos);
                final String name = val.substring(sepPos + 1);

                found = feature.getConfigurations().getFactoryConfiguration(factoryPid, name);
            }
            if ( found != null ) {
                if ( attr == null ) {
                    feature.getConfigurations().remove(found);
                } else {
                    found.getProperties().remove(attr);
                }
            }
        }

        // process framework properties removals
        for (final String p : include.getFrameworkPropertiesRemovals()) {
            feature.getFrameworkProperties().remove(p);
        }

        // process extensions removals
        for (final String name : include.getExtensionRemovals()) {
            for (final Extension ext : feature.getExtensions()) {
                if ( ext.getName().equals(name) ) {
                    feature.getExtensions().remove(ext);
                    break;
                }
            }
        }
        // process artifact extensions removals
        for (final Map.Entry<String, List<ArtifactId>> entry : include.getArtifactExtensionRemovals().entrySet()) {
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
    }
}
