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

import org.apache.sling.feature.Application;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.Feature;

/**
 * Build an application based on features.
 */
public class ApplicationBuilder {

    /**
     * Assemble an application based on the provided features.
     *
     * The features are processed in the order they are provided.
     * If the same feature is included more than once only the feature with
     * the highest version is used. The others are ignored.
     *
     * @param app The optional application to use as a base.
     * @param context The builder context
     * @param features The features
     * @return The application
     * throws IllegalArgumentException If context or featureIds is {@code null}
     * throws IllegalStateException If a feature can't be provided
     */
    public static Application assemble(
            Application app,
            final BuilderContext context,
            final Feature... features) {
        if ( features == null || context == null ) {
            throw new IllegalArgumentException("Features and/or context must not be null");
        }

        if ( app == null ) {
            app = new Application();
        }

        final Feature[] assembledFeatures = FeatureBuilder.deduplicate(context, features);

        // assemble application
        for(final Feature assembled : assembledFeatures) {
            app.getFeatureIds().add(assembled.getId());

            for (Artifact a : assembled.getBundles()) {
                int so = a.getMetadata().get("start-level") != null ? Integer.parseInt(a.getMetadata().get("start-level")) : 1;
                a.setStartOrder(so);
            }

            merge(app, assembled);
        }

        return app;
    }

    private static void merge(final Application target, final Feature source) {
        BuilderUtil.mergeVariables(target.getVariables(), source.getVariables());
        BuilderUtil.mergeBundles(target.getBundles(), source.getBundles(), BuilderUtil.ArtifactMerge.HIGHEST);
        BuilderUtil.mergeConfigurations(target.getConfigurations(), source.getConfigurations());
        BuilderUtil.mergeFrameworkProperties(target.getFrameworkProperties(), source.getFrameworkProperties());
        BuilderUtil.mergeExtensions(target, source, BuilderUtil.ArtifactMerge.HIGHEST);
    }
}
