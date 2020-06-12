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

import javax.json.JsonStructure;
import javax.json.JsonValue;

/**
 * Execution environment extension
 * @since 1.4.0
 */
public class ExecutionEnvironmentExtension {

    /**
     * Extension name containing the execution environment. The execution
     * environment can specify the framework to launch
     * This extension is of type {@link ExtensionType#JSON} and is optional.
     */
    public static final String EXTENSION_NAME = "execution-environment";

    /**
     * Get the execution environment from the feature - if it exists.
     * @param feature The feature
     * @return The execution environment or {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     */
    public static ExecutionEnvironmentExtension getExecutionEnvironmentExtension(final Feature feature) {
        final Extension ext = feature == null ? null : feature.getExtensions().getByName(EXTENSION_NAME);
        return getExecutionEnvironmentExtension(ext);
    }

    /**
     * Get the execution environment from the extension.
     * @param ext The extension
     * @return The execution environment or {@code null}.
     * @throws IllegalArgumentException If the extension is wrongly formatted
     */
    public static ExecutionEnvironmentExtension getExecutionEnvironmentExtension(final Extension ext) {
        if ( ext == null ) {
            return null;
        }
        if ( ext.getType() != ExtensionType.JSON ) {
            throw new IllegalArgumentException("Extension " + ext.getName() + " must have JSON type");
        }
        return new ExecutionEnvironmentExtension(ext.getJSONStructure());
    }

    private final Artifact framework;

    private ExecutionEnvironmentExtension(final JsonStructure structure) {
        // get framework
        final JsonValue fwk = structure.asJsonObject().getOrDefault("framework", null);
        if ( fwk != null ) {
            this.framework = new Artifact(fwk);
        } else {
            this.framework = null;
        }
    }

    /**
     * Get the specified framework
     * @return The framework or {@code null}
     */
    public Artifact getFramework() {
        return this.framework;
    }
}
