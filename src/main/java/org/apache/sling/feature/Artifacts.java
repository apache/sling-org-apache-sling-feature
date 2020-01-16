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

import java.util.ArrayList;

/**
 * Groups a list of {@code Artifact}s. This class is not thread-safe.
 */
public class Artifacts extends ArrayList<Artifact> {

    private static final long serialVersionUID = 240141452817960076L;

    /**
     * Add an artifact. If the exact artifact is already contained in the
     * collection, it is not added again.
     *
     * @param artifact The artifact
     * @return {@code true} if this collection changed as a result of the call
     */
    @Override
    public boolean add(final Artifact artifact) {
        if (this.containsExact(artifact.getId())) {
            return false;
        }
        return super.add(artifact);
    }

    /**
     * Remove the exact artifact. The first one found is removed.
     *
     * @param id The artifact id
     * @return {@code true} if the artifact has been removed
     */
    public boolean removeExact(final ArtifactId id) {
        for (final Artifact artifact : this) {
            if ( artifact.getId().equals(id)) {
                return this.remove(artifact);
            }
        }
        return false;
    }

    /**
     * Remove the same artifact, neglecting the version. The first one found is
     * removed.
     *
     * @param id The artifact id
     * @return {@code true} if the artifact has been removed
     */
    public boolean removeSame(final ArtifactId id) {
        for (final Artifact artifact : this) {
            if ( artifact.getId().isSame(id)) {
                return this.remove(artifact);
            }
        }
        return false;
    }

    /**
     * Get the artifact for the given id, neglecting the version
     *
     * @param id The artifact id
     * @return The artifact or {@code null} otherwise
     */
    public Artifact getSame(final ArtifactId id) {
        for (final Artifact artifact : this) {
            if (artifact.getId().isSame(id)) {
                return artifact;
            }
        }
        return null;
    }

    /**
     * Get the artifact for the given id
     *
     * @param id The artifact id
     * @return The artifact or {@code null} otherwise
     */
    public Artifact getExact(final ArtifactId id) {
        for (final Artifact artifact : this) {
            if (artifact.getId().equals(id)) {
                return artifact;
            }
        }
        return null;
    }

    /**
     * Checks whether the exact artifact is available
     * @param id The artifact id.
     * @return {@code true} if the artifact exists
     */
    public boolean containsExact(final ArtifactId id) {
        for (final Artifact entry : this) {
            if ( entry.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the same artifact is available, neglecting the version
     * @param id The artifact id.
     * @return {@code true} if the artifact exists
     */
    public boolean containsSame(final ArtifactId id) {
        for (final Artifact entry : this) {
            if ( entry.getId().isSame(id)) {
                return true;
            }
        }
        return false;
    }
}
