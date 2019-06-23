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

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * A Post Process Handler processes features after a merge operation. The
 * handlers are passed in to the {@link FeatureBuilder} via
 * {@link BuilderContext#addPostProcessExtensions(PostProcessHandler...)}. Once
 * all extensions are merged, all post processor handlers are called for each
 * extension in the target feature.
 */
@ConsumerType
public interface PostProcessHandler {
    /**
     * Post process the feature with respect to the extension.
     * Post processing is invoked after all extensions have been merged.
     *
     * @param context Context for the handler
     * @param feature The feature
     * @param extension The extension
     * @throws IllegalStateException If post processing failed
     */
    void postProcess(HandlerContext context, Feature feature, Extension extension);
}
