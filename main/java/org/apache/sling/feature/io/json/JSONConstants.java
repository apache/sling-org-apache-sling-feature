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
package org.apache.sling.feature.io.json;

import java.util.Arrays;
import java.util.List;

import org.apache.sling.feature.Configuration;

public abstract class JSONConstants {

    static final String FEATURE_ID = "id";

    static final String FEATURE_VARIABLES = "variables";

    static final String FEATURE_BUNDLES = "bundles";

    static final String FEATURE_FRAMEWORK_PROPERTIES = "framework-properties";

    static final String FEATURE_CONFIGURATIONS = "configurations";

    static final String FEATURE_PROTOTYPE = "prototype";

    static final String FEATURE_REQUIREMENTS = "requirements";

    static final String FEATURE_CAPABILITIES = "capabilities";

    static final String FEATURE_TITLE = "title";

    static final String FEATURE_DESCRIPTION = "description";

    static final String FEATURE_VENDOR = "vendor";

    static final String FEATURE_LICENSE = "license";

    static final String FEATURE_FINAL = "final";

    static final String FEATURE_COMPLETE = "complete";

    static final String FEATURE_MODEL_VERSION = "model-version";

    static final List<String> FEATURE_KNOWN_PROPERTIES = Arrays.asList(FEATURE_ID,
            FEATURE_MODEL_VERSION,
            FEATURE_VARIABLES,
            FEATURE_BUNDLES,
            FEATURE_FRAMEWORK_PROPERTIES,
            FEATURE_CONFIGURATIONS,
            FEATURE_PROTOTYPE,
            FEATURE_REQUIREMENTS,
            FEATURE_CAPABILITIES,
            FEATURE_TITLE,
            FEATURE_DESCRIPTION,
            FEATURE_VENDOR,
            FEATURE_FINAL,
            FEATURE_COMPLETE,
            FEATURE_LICENSE);

    static final String ARTIFACT_ID = "id";

    static final List<String> ARTIFACT_KNOWN_PROPERTIES = Arrays.asList(ARTIFACT_ID,
            Configuration.PROP_ARTIFACT_ID,
            FEATURE_CONFIGURATIONS);

    static final String PROTOTYPE_REMOVALS = "removals";

    static final String PROTOTYPE_EXTENSION_REMOVALS = "extensions";

    static final String REQCAP_NAMESPACE = "namespace";
    static final String REQCAP_ATTRIBUTES = "attributes";
    static final String REQCAP_DIRECTIVES = "directives";
}
