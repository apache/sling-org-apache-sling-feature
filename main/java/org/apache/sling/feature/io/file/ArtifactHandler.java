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
package org.apache.sling.feature.io.file;

import java.io.File;

/**
 * A handler provides a file object for an artifact.
 */
public class ArtifactHandler {

    private final String url;

    private final File file;

    /**
     * Create a new handler.
     * 
     * @param url  The url of the artifact
     * @param file The file for the artifact
     */
    public ArtifactHandler(final String url, final File file) {
        this.url = url;
        this.file = file;
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
     * Get a file for the artifact
     * 
     * @return The file
     */
    public File getFile() {
        return file;
    }
}
