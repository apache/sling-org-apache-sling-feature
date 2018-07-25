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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Include;
import org.osgi.framework.Version;

public class FeatureBuilder {

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

        if ( !result.getIncludes().isEmpty() ) {

            final List<Include> includes = new ArrayList<>(result.getIncludes());

            // clear everything in the result, will be added in the process
            result.getVariables().clear();
            result.getBundles().clear();
            result.getFrameworkProperties().clear();
            result.getConfigurations().clear();
            result.getRequirements().clear();
            result.getCapabilities().clear();
            result.getIncludes().clear();
            result.getExtensions().clear();

            for(final Include i : includes) {
                final Feature f = context.getFeatureProvider().provide(i.getId());
                if ( f == null ) {
                    throw new IllegalStateException("Unable to find included feature " + i.getId());
                }
                final Feature af = internalAssemble(processedFeatures, f, context);

                // process include instructions
                include(af, i);

                // and now merge
                merge(result, af, context);
            }
            merge(result, feature, context);
        }
        processedFeatures.remove(feature.getId().toMvnId());

        result.setAssembled(true);
        return result;
    }

    private static void merge(final Feature target,
            final Feature source,
            final BuilderContext context) {
        BuilderUtil.mergeVariables(target.getVariables(), source.getVariables());
        BuilderUtil.mergeBundles(target.getBundles(), source.getBundles(), BuilderUtil.ArtifactMerge.LATEST);
        BuilderUtil.mergeConfigurations(target.getConfigurations(), source.getConfigurations());
        BuilderUtil.mergeFrameworkProperties(target.getFrameworkProperties(), source.getFrameworkProperties());
        BuilderUtil.mergeRequirements(target.getRequirements(), source.getRequirements());
        BuilderUtil.mergeCapabilities(target.getCapabilities(), source.getCapabilities());
        BuilderUtil.mergeExtensions(target,
                source,
                BuilderUtil.ArtifactMerge.LATEST,
                context);
    }

    private static void include(final Feature base, final Include i) {
        // process removals
        // bundles
        for(final ArtifactId a : i.getBundleRemovals()) {
            boolean removed = false;
            final boolean ignoreVersion = a.getOSGiVersion().equals(Version.emptyVersion);
            if ( ignoreVersion ) {
                // remove any version of that bundle
                while (base.getBundles().removeSame(a)) {
                    // continue to remove
                    removed = true;
                }
            } else {
                // remove exact version
                removed = base.getBundles().removeExact(a);
            }
            if ( !removed ) {
                throw new IllegalStateException("Bundle " + a + " can't be removed from feature " + base.getId() + " as it is not part of that feature.");
            }
            final Iterator<Configuration> iter = base.getConfigurations().iterator();
            while ( iter.hasNext() ) {
                final Configuration cfg = iter.next();
                final String bundleId = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
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
        // configurations
        for(final String c : i.getConfigurationRemovals()) {
            final int attrPos = c.indexOf('@');
            final String val = (attrPos == -1 ? c : c.substring(0, attrPos));
            final String attr = (attrPos == -1 ? null : c.substring(attrPos + 1));

            final int sepPos = val.indexOf('~');
            Configuration found = null;
            if ( sepPos == -1 ) {
                found = base.getConfigurations().getConfiguration(val);

            } else {
                final String factoryPid = val.substring(0, sepPos);
                final String name = val.substring(sepPos + 1);

                found = base.getConfigurations().getFactoryConfiguration(factoryPid, name);
            }
            if ( found != null ) {
                if ( attr == null ) {
                    base.getConfigurations().remove(found);
                } else {
                    found.getProperties().remove(attr);
                }
            }
        }

        // framework properties
        for(final String p : i.getFrameworkPropertiesRemovals()) {
            base.getFrameworkProperties().remove(p);
        }

        // extensions
        for(final String name : i.getExtensionRemovals()) {
            for(final Extension ext : base.getExtensions()) {
                if ( ext.getName().equals(name) ) {
                    base.getExtensions().remove(ext);
                    break;
                }
            }
        }
        for(final Map.Entry<String, List<ArtifactId>> entry : i.getArtifactExtensionRemovals().entrySet()) {
            for(final Extension ext : base.getExtensions()) {
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
                            throw new IllegalStateException("Artifact " + toRemove + " can't be removed from feature " + base.getId() + " as it is not part of that feature.");
                        }
                    }
                    break;
                }
            }
        }
    }
}
