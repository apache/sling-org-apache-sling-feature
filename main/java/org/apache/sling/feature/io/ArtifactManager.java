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
package org.apache.sling.feature.io;

import java.io.IOException;

import org.apache.sling.feature.io.spi.ArtifactProvider;

/**
 * The artifact manager is the central service to get artifacts.
 * It uses {@link ArtifactProvider}s to get artifacts. The
 * providers are loaded using the service loader.
 */
public interface ArtifactManager extends AutoCloseable {

    /**
     * Get the full artifact url and file for an artifact.
     * @param url Artifact url or relative path.
     * @return Absolute url and file in the form of a handler.
     * @throws IOException If something goes wrong.
     */
    ArtifactHandler getArtifactHandler(final String url) throws IOException;

    @Override
    void close();
}
