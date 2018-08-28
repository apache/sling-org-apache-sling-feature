/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature;

import org.apache.sling.feature.builder.BuilderContext;

public abstract class FeatureConstants {

    /**
     * Common extension name to specify the repoinit part for Apache Sling.
     * This extension is of type {@link ExtensionType#TEXT} and is
     * required.
     */
    public static final String EXTENSION_NAME_REPOINIT = "repoinit";

    /**
     * Common extension name to specify the content packages for Apache Sling.
     * This extension is of type {@link ExtensionType#ARTIFACTS} and is
     * required.
     */
    public static final String EXTENSION_NAME_CONTENT_PACKAGES = "content-packages";

    /**
     * Extension name containing the assembled features as produced
     * by {@link org.apache.sling.feature.builder.FeatureBuilder#assemble(ArtifactId, BuilderContext, Feature...)}.
     * This extension is of type {@link ExtensionType#ARTIFACTS} and is
     * optional.
     */
    public static final String EXTENSION_NAME_ASSEMBLED_FEATURES = "assembled-features";
}
