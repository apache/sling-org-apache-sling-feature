/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.feature.builder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.osgi.framework.Version;

/**
 * Resolves OSGi Bundle-SymbolicName collisions in an assembled feature. See
 * {@link BuilderContext#setOsgiBsnCollisionDetection(boolean)}.
 */
final class OsgiBsnDeduplicator {

    private OsgiBsnDeduplicator() {}

    static void apply(final Feature target, final BuilderContext context) {
        if (!context.isOsgiBsnCollisionDetectionEnabled()) {
            return;
        }
        final ArtifactProvider provider = context.getArtifactProvider();
        if (provider == null) {
            return;
        }

        // Iteration order matches the order features were merged in; LATEST/FIRST rely on it.
        final Map<String, List<Artifact>> groups = new LinkedHashMap<>();
        final Map<Artifact, String> bundleVersions = new HashMap<>();
        for (final Artifact a : target.getBundles()) {
            final String[] bsnAndVersion = readBsnAndVersion(provider, a.getId());
            if (bsnAndVersion.length == 0) {
                continue;
            }
            groups.computeIfAbsent(bsnAndVersion[0], k -> new ArrayList<>()).add(a);
            bundleVersions.put(a, bsnAndVersion[1]);
        }

        for (final Map.Entry<String, List<Artifact>> entry : groups.entrySet()) {
            final List<Artifact> conflicting = entry.getValue();
            if (conflicting.size() < 2) {
                continue;
            }
            final List<Artifact> winners =
                    resolve(entry.getKey(), conflicting, bundleVersions, context.getArtifactOverrides());
            for (final Artifact a : conflicting) {
                if (!winners.contains(a)) {
                    target.getBundles().remove(a);
                }
            }
        }
    }

    private static List<Artifact> resolve(
            final String bsn,
            final List<Artifact> conflicting,
            final Map<Artifact, String> bundleVersions,
            final List<ArtifactId> artifactOverrides) {
        final String overrideRule = findWildcardOverrideRule(artifactOverrides);
        if (overrideRule == null) {
            throw new IllegalStateException("Bundle-SymbolicName collision detected and no override rule available. "
                    + "Configure a wildcard override (e.g. *:*:HIGHEST) on the BuilderContext to resolve it. "
                    + buildConflictMessage(bsn, conflicting));
        }
        return applyOverrideRule(bsn, conflicting, bundleVersions, overrideRule);
    }

    /** @return the rule from a {@code *:*:<rule>} override, or null if none configured. */
    private static String findWildcardOverrideRule(final List<ArtifactId> artifactOverrides) {
        for (final ArtifactId override : artifactOverrides) {
            if (BuilderContext.COORDINATE_MATCH_ALL.equals(override.getGroupId())
                    && BuilderContext.COORDINATE_MATCH_ALL.equals(override.getArtifactId())) {
                return override.getVersion();
            }
        }
        return null;
    }

    private static List<Artifact> applyOverrideRule(
            final String bsn,
            final List<Artifact> conflicting,
            final Map<Artifact, String> bundleVersions,
            final String rule) {
        if (BuilderContext.VERSION_OVERRIDE_ALL.equalsIgnoreCase(rule)) {
            return new ArrayList<>(conflicting);
        }
        if (BuilderContext.VERSION_OVERRIDE_FIRST.equalsIgnoreCase(rule)) {
            return Collections.singletonList(conflicting.get(0));
        }
        if (BuilderContext.VERSION_OVERRIDE_LATEST.equalsIgnoreCase(rule)) {
            return Collections.singletonList(conflicting.get(conflicting.size() - 1));
        }
        if (BuilderContext.VERSION_OVERRIDE_HIGHEST.equalsIgnoreCase(rule)) {
            Artifact best = conflicting.get(0);
            Version bestVersion = parseQuietly(bundleVersions.get(best));
            for (int i = 1; i < conflicting.size(); i++) {
                final Artifact candidate = conflicting.get(i);
                final Version candidateVersion = parseQuietly(bundleVersions.get(candidate));
                if (candidateVersion.compareTo(bestVersion) > 0) {
                    best = candidate;
                    bestVersion = candidateVersion;
                }
            }
            return Collections.singletonList(best);
        }
        // Literal version: match against each candidate's Bundle-Version.
        for (final Artifact a : conflicting) {
            if (rule.equals(bundleVersions.get(a))) {
                return Collections.singletonList(a);
            }
        }
        throw new IllegalStateException("Wildcard override rule '" + rule
                + "' is a literal version that does not match any bundle in the colliding group. "
                + buildConflictMessage(bsn, conflicting));
    }

    private static Version parseQuietly(final String version) {
        try {
            return Version.parseVersion(version);
        } catch (final IllegalArgumentException e) {
            return Version.emptyVersion;
        }
    }

    private static String buildConflictMessage(final String bsn, final List<Artifact> conflicting) {
        final StringBuilder sb =
                new StringBuilder("Bundle-SymbolicName collision: ").append(bsn).append(" provided by ");
        for (int i = 0; i < conflicting.size(); i++) {
            if (i > 0) {
                sb.append(i == conflicting.size() - 1 ? " and " : ", ");
            }
            sb.append(conflicting.get(i).getId().toMvnId());
        }
        return sb.toString();
    }

    private static final String[] EMPTY = new String[0];

    /** @return [bsn, version] or empty if unavailable. BSN directives stripped. */
    private static String[] readBsnAndVersion(final ArtifactProvider provider, final ArtifactId id) {
        final URL url = provider.provide(id);
        if (url == null) {
            return EMPTY;
        }
        try (final InputStream is = url.openStream();
                final JarInputStream jar = new JarInputStream(is)) {
            final Manifest mf = jar.getManifest();
            if (mf == null) {
                return EMPTY;
            }
            final Attributes attrs = mf.getMainAttributes();
            final String bsnRaw = attrs.getValue("Bundle-SymbolicName");
            final String version = attrs.getValue("Bundle-Version");
            if (bsnRaw == null || version == null) {
                return EMPTY;
            }
            final int sep = bsnRaw.indexOf(';');
            final String bsn = (sep < 0 ? bsnRaw : bsnRaw.substring(0, sep)).trim();
            return new String[] {bsn, version};
        } catch (final IOException e) {
            return EMPTY;
        }
    }
}
