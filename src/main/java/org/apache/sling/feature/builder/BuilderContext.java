/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;

/**
 * Builder context holds services and configuration used by {@link FeatureBuilder}
 * and controls how features are assembled and aggregated.
 *
 * <p>
 * When two features are merged, being it a prototype with the feature using the
 * prototype or two features, there might be a clash with bundles or artifacts.
 * A clash occurs when there is an artifact in the source and the target with
 * different versions. If the version is the same, the source artifact will
 * override the target artifact. However, all other cases need instructions on
 * how to proceed.
 * <p>
 * An override rule is an artifact id. As the version for the rule, one of
 * {@link BuilderContext#VERSION_OVERRIDE_ALL},
 * {@link BuilderContext#VERSION_OVERRIDE_LATEST} or
 * {@link BuilderContext#VERSION_OVERRIDE_FIRST} or
 * {@link BuilderContext#VERSION_OVERRIDE_HIGHEST} as well as any version can be
 * specified. If the artifact id should match more than a single artifact
 * {@link BuilderContext#COORDINATE_MATCH_ALL} can be specified as group id,
 * artifact id, type and/or classifier.
 * <p>
 * A clash might also happen with framework properties or variables. In this case
 * an override must be provided for that variable or framework property as well.
 * <p>
 * This class is not thread-safe.
 */
public class BuilderContext {

    /** Used in override rule to select all candidates. */
    public static final String VERSION_OVERRIDE_ALL = "ALL";

    /**
     * Used in override rule to select the candidate with the highest version (OSGi
     * version comparison rules).
     */
    public static final String VERSION_OVERRIDE_HIGHEST = "HIGHEST";

    /** Used in override rule to select the last candidate applied. */
    public static final String VERSION_OVERRIDE_LATEST = "LATEST";

    /**
     * Used in override rule to select the first candidate applied.
     * @since 1.3.0
     */
    public static final String VERSION_OVERRIDE_FIRST = "FIRST";

    /** Used in override rule to match all coordinates */
    public static final String COORDINATE_MATCH_ALL = "*";

    /** Used to handle configuration merging - fail the merge when there is a clash for a PID - this is the default */
    public static final String CONFIG_FAIL_ON_CLASH = "CLASH";

    /** Used to handle configuration merging - fail the merge only when there is a clash on a property level */
    public static final String CONFIG_FAIL_ON_PROPERTY_CLASH = "PROPERTY_CLASH";

    /** Used to handle configuration merging - use the latest configuration, but don't merge */
    public static final String CONFIG_USE_LATEST = "USE_LATEST";

    /** Used to handle configuration merging - use the first configuration, don't merge */
    public static final String CONFIG_USE_FIRST = "USE_FIRST";

    /** Used to handle configuration merging - merge the latest configuration in, latest props might override previous values */
    public static final String CONFIG_MERGE_LATEST = "MERGE_LATEST";

    /** Used to handle configuration merging - merge the first configuration in, latest props will not override previous values */
    public static final String CONFIG_MERGE_FIRST = "MERGE_FIRST";

    /** Configuration key for configuration for all handlers */
    static final String CONFIGURATION_ALL_HANDLERS_KEY = "all";

    /** The required feature provider */
    private final FeatureProvider provider;

    /** The optional artifact provider. */
    private ArtifactProvider artifactProvider;

    private final Map<String, Map<String, String>> extensionConfiguration = new HashMap<>();
    private final List<MergeHandler> mergeExtensions = new ArrayList<>();
    private final List<PostProcessHandler> postProcessExtensions = new ArrayList<>();
    private final List<ArtifactId> artifactsOverrides = new ArrayList<>();
    private final Map<String, String> variables = new HashMap<>();
    private final Map<String, String> frameworkProperties = new HashMap<>();
    private final Map<String, String> configOverrides = new LinkedHashMap<>();

    /**
     * Create a new context.
     * The feature provider is for example used to get a prototype feature.
     *
     * @param provider A provider providing required features for processing
     * @throws IllegalArgumentException If feature provider is {@code null}
     */
    public BuilderContext(final FeatureProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Provider must not be null");
        }
        this.provider = provider;
    }

    /**
     * Set the artifact provider. While the artifact provider is not required by the
     * general assembly and merging algorithms, handlers for extensions might need
     * it.
     *
     * @param ap An ArtifactProvider to resolve artifact IDs to URLs
     * @return The builder context
     */
    public BuilderContext setArtifactProvider(final ArtifactProvider ap) {
        this.artifactProvider = ap;
        return this;
    }

    /**
     * Add overrides for the variables.
     * Variables can be overridden if any feature in the aggregation/assembly process
     * contains an overriden variable. If multiple definitions of the same variable
     * are found in the features that are to be aggregated and the values for these
     * variables are different, they must be overridden, otherwise the aggregation will
     * fail.
     *
     * @param vars The overrides keyed by variable name
     * @return The builder context
     */
    public BuilderContext addVariablesOverrides(final Map<String, String> vars) {
        this.variables.putAll(vars);
        return this;
    }

    /**
     * Add overrides for the framework properties.
     * Framework properties can be overridden if any feature in the aggregation/assembly process
     * contains an overriden framework property. If multiple definitions of the same framework
     * property are found in the features that are to be aggregated and the values for these
     * properties are different, they must be overridden, otherwise the aggregation will fail.
     *
     * @param props The overrides keyed by framework property name
     * @return The builder context
     */
    public BuilderContext addFrameworkPropertiesOverrides(final Map<String, String> props) {
        this.frameworkProperties.putAll(props);
        return this;
    }

    /**
     * Add an override for artifact clashes.
     *
     * @param override The override
     * @return The builder context
     */
    public BuilderContext addArtifactsOverride(final ArtifactId override) {
        this.artifactsOverrides.add(override);
        return this;
    }

    /**
     * Add merge policies for configuration clashes.
     *
     * @param overrides The overrides
     * @return The builder context
     */
    public BuilderContext addConfigsOverrides(final Map<String, String> overrides) {
        this.configOverrides.putAll(overrides);
        return this;
    }

    /**
     * Add merge extensions
     *
     * @param extensions A list of merge extensions.
     * @return The builder context
     */
    public BuilderContext addMergeExtensions(final MergeHandler... extensions) {
        mergeExtensions.addAll(Arrays.asList(extensions));
        return this;
    }

    /**
     * Add post process extensions
     *
     * @param extensions A list of extensions
     * @return The builder context
     */
    public BuilderContext addPostProcessExtensions(final PostProcessHandler... extensions) {
        postProcessExtensions.addAll(Arrays.asList(extensions));
        return this;
    }

    /**
     * Set a handler configuration. A configuration can be set for both
     * {@link MergeHandler}s and {@link PostProcessHandler}s. The name of a handler
     * is the simple class name of the class implementing the handler. To pass the
     * same configuration to all handlers, use the
     * {@link #CONFIGURATION_ALL_HANDLERS_KEY} name.
     *
     * @param name The name of the handler
     * @param cfg  The configuration for the handler
     * @return The builder context
     */
    public BuilderContext setHandlerConfiguration(final String name, final Map<String, String> cfg) {
        this.extensionConfiguration.put(name, cfg);
        return this;
    }

    /**
     * Obtain the handler configuration.
     *
     * @return The current handler configuration object. The key is the handler name
     *         and the value is a map of configuration values.
     */
    Map<String, Map<String, String>> getHandlerConfigurations() {
        return this.extensionConfiguration;
    }

    ArtifactProvider getArtifactProvider() {
        return this.artifactProvider;
    }

    List<ArtifactId> getArtifactOverrides() {
        return this.artifactsOverrides;
    }

    Map<String, String> getConfigOverrides() {
        return this.configOverrides;
    }

    Map<String, String> getVariablesOverrides() {
        return this.variables;
    }

    Map<String, String> getFrameworkPropertiesOverrides() {
        return this.frameworkProperties;
    }

    /**
     * Get the feature provider.
     * @return The feature provider
     */
    FeatureProvider getFeatureProvider() {
        return this.provider;
    }

    /**
     * Get the list of merge extensions
     * @return The list of merge extensions
     */
    List<MergeHandler> getMergeExtensions() {
        return this.mergeExtensions;
    }

    /**
     * Get the list of extension post processors
     * @return The list of post processors
     */
    List<PostProcessHandler> getPostProcessExtensions() {
        return this.postProcessExtensions;
    }

    /**
     * Clone the context and replace the feature provider
     * @param featureProvider The new feature provider
     * @return Cloned context
     */
    BuilderContext clone(final FeatureProvider featureProvider) {
        final BuilderContext ctx = new BuilderContext(featureProvider);
        ctx.setArtifactProvider(this.artifactProvider);
        ctx.artifactsOverrides.addAll(this.artifactsOverrides);
        ctx.variables.putAll(this.variables);
        ctx.frameworkProperties.putAll(this.frameworkProperties);
        ctx.extensionConfiguration.putAll(this.extensionConfiguration);
        ctx.mergeExtensions.addAll(mergeExtensions);
        ctx.postProcessExtensions.addAll(postProcessExtensions);
        return ctx;
    }
}
