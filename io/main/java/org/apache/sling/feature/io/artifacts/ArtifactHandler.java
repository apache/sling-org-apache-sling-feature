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
package org.apache.sling.feature.io.artifacts;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A handler provides a file object for an artifact.
 */
public class ArtifactHandler {

    private final String url;

    private final URL localURL;

    /**
     * Create a new handler.
     *
     * @param url  The url of the artifact
     * @param localURL The local URL for the artifact
     */
    public ArtifactHandler(final String url, final URL localURL) {
        this.url = url;
        this.localURL = localURL;
    }

    /**
     * Create a new handler.
     *
     * @param file The file for the artifact
     * @throws MalformedURLException
     * @since 1.1.0
     */
    public ArtifactHandler(final File file) throws MalformedURLException {
        this(file.toURI().toString(), file.toURI().toURL());
    }

    /**
     * Get the url of the artifact
     *
     * @return The url.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get a local url for the artifact
     *
     * @return The file
     */
    public URL getLocalURL() {
        return localURL;
    }
}
