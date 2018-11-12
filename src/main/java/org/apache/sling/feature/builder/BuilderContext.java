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

import org.apache.sling.feature.KeyValueMap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Builder context holds services used by  {@link FeatureBuilder}.
 */
public class BuilderContext {

    private final ArtifactProvider artifactProvider;
    private final FeatureProvider provider;
    private final Map<String, Map<String, String>> extensionConfiguration = new ConcurrentHashMap<>();
    private final List<MergeHandler> mergeExtensions = new CopyOnWriteArrayList<>();
    private final List<PostProcessHandler> postProcessExtensions = new CopyOnWriteArrayList<>();
    private final KeyValueMap variables = new KeyValueMap();
    private final Map<String, String> properties = new LinkedHashMap<>();

    /**
     * Create a new context
     *
     * @param provider A provider providing the included features
     * @param ap An ArtifactProvider to resolve artifact IDs to files
     * @throws IllegalArgumentException If feature provider is {@code null}
     */
    public BuilderContext(final FeatureProvider provider, final ArtifactProvider ap) {
        this(provider, ap, null, null);
    }

    /**
     * Create a new context
     *
     * @param provider A provider providing the included features
     * @param ap An ArtifactProvider to resolve artifact IDs to files
     * @param variables A map of variables to override on feature merge
     * @param properties A map of framework properties to override on feature merge
     * @throws IllegalArgumentException If feature provider is {@code null}
     */
    public BuilderContext(final FeatureProvider provider, ArtifactProvider ap, KeyValueMap variables, Map<String, String> properties) {
        if (variables != null) {
            this.variables.putAll(variables);
        }
        if (properties != null) {
            this.properties.putAll(properties);
        }
        if ( provider == null ) {
            throw new IllegalArgumentException("Provider must not be null");
        }
        this.artifactProvider = ap;
        this.provider = provider;
    }

    public BuilderContext addMergeExtensions(final MergeHandler... extensions) {
        mergeExtensions.addAll(Arrays.asList(extensions));
        return this;
    }

    public BuilderContext addPostProcessExtensions(final PostProcessHandler... extensions) {
        postProcessExtensions.addAll(Arrays.asList(extensions));
        return this;
    }

    /**
     * Obtain the handler configuration. The object returned can be modified to provide
     * additional handler configurations.
     * @return The current handler configuration object. The key is the handler name
     * and the value is a map of configuration values.
     */
    public Map<String, Map<String, String>> getHandlerConfiguration() {
        return this.extensionConfiguration;
    }

    ArtifactProvider getArtifactProvider() {
        return this.artifactProvider;
    }

    KeyValueMap getVariables() {
        return  this.variables;
    }

    Map<String, String> getProperties() {
        return this.properties;
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
        final BuilderContext ctx = new BuilderContext(featureProvider, this.artifactProvider, this.variables, this.properties);
        ctx.mergeExtensions.addAll(mergeExtensions);
        ctx.postProcessExtensions.addAll(postProcessExtensions);
        return ctx;
    }
}
