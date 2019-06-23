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
 * A merge handler can merge an extension of a particular type. The handlers are
 * passed in to the {@link FeatureBuilder} via
 * {@link BuilderContext#addMergeExtensions(MergeHandler...)}. When the feature
 * builder is merging features, the first handler that returns {@code true} for
 * an extension in {@link #canMerge(Extension)} merges the extension. Further
 * handlers are not tested anymore.
 */
@ConsumerType
public interface MergeHandler {
    /**
     * Checks whether this merger can merge the given extension.
     *
     * @param extension The extension
     * @return {@code true} if merger can handle this
     */
    boolean canMerge(Extension extension);

    /**
     * Merge the source extension into the target extension.
     *
     * Only called if {@link #canMerge(Extension)} for the extension returned
     * {@code true}. If the target does not yet contain this extension, then the
     * targetEx argument is {@code null}. In that case the handler should add the
     * extension to the target.
     *
     * @param context  Context for the handler
     * @param target   The target feature
     * @param source   The source feature
     * @param targetEx The target extension or {@code null} if the extension does
     *                 not exist in the target.
     * @param sourceEx The source extension
     * @throws IllegalStateException If the extensions can't be merged
     */
    void merge(HandlerContext context, Feature target, Feature source, Extension targetEx, Extension sourceEx);
}
