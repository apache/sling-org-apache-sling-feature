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
package org.apache.sling.feature.io.archive;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.io.json.FeatureJSONReader;

/**
 * The feature archive reader can be used to read an archive based on a feature
 * model. The archive contains the model and all artifacts.
 */
public class ArchiveReader {

    public interface ArtifactConsumer {

        /**
         * Consume the artifact from the archive The input stream must not be closed by
         * the consumer.
         *
         * @param artifactId The artifact id
         * @param is         The input stream for the artifact
         * @throws IOException If the artifact can't be consumed
         */
        void consume(ArtifactId artifactId, final InputStream is) throws IOException;
    }

    /**
     * Read a feature model archive. The input stream is not closed. It is up to the
     * caller to close the input stream.
     *
     * @param in The input stream to read from.
     * @return The feature model
     * @throws IOException If anything goes wrong
     */
    @SuppressWarnings("resource")
    public static Feature read(final InputStream in,
                             final ArtifactConsumer consumer)
    throws IOException {
        Feature feature = null;

        final JarInputStream jis = new JarInputStream(in);

        // check manifest
        final Manifest manifest = jis.getManifest();
        if ( manifest == null ) {
            throw new IOException("Not a feature model archive - manifest is missing.");
        }
        // check manifest header
        final String version = manifest.getMainAttributes().getValue(ArchiveWriter.MANIFEST_HEADER);
        if ( version == null ) {
            throw new IOException("Not a feature model archive - manifest header is missing.");
        }
        // validate manifest header
        try {
            final int number = Integer.valueOf(version);
            if ( number < 1 || number > ArchiveWriter.ARCHIVE_VERSION ) {
                throw new IOException("Not a feature model archive - invalid manifest header value: " + version);
            }
        } catch (final NumberFormatException nfe) {
            throw new IOException("Not a feature model archive - invalid manifest header value: " + version);
        }

        final Set<ArtifactId> artifacts = new HashSet<>();

        // read contents
        JarEntry entry = null;
        while ( ( entry = jis.getNextJarEntry() ) != null ) {
            if ( ArchiveWriter.MODEL_NAME.equals(entry.getName()) ) {
                feature = FeatureJSONReader.read(new InputStreamReader(jis, "UTF-8"), null);
            } else if ( !entry.isDirectory() && entry.getName().startsWith(ArchiveWriter.ARTIFACTS_PREFIX) ) { // artifact
                final ArtifactId id = ArtifactId
                        .fromMvnUrl("mvn:" + entry.getName().substring(ArchiveWriter.ARTIFACTS_PREFIX.length()));
                consumer.consume(id, jis);
                artifacts.add(id);
            }
            jis.closeEntry();
        }
        if (feature == null) {
            throw new IOException("Not a feature model archive - feature file is missing.");
        }

        // check whether all artifacts from the model are in the archive

        for (final Artifact a : feature.getBundles()) {
            if (!artifacts.contains(a.getId())) {
                throw new IOException("Artifact " + a.getId().toMvnId() + " is missing in archive");
            }
        }

        for (final Extension e : feature.getExtensions()) {
            if (e.getType() == ExtensionType.ARTIFACTS) {
                for (final Artifact a : e.getArtifacts()) {
                    if (!artifacts.contains(a.getId())) {
                        throw new IOException("Artifact " + a.getId().toMvnId() + " is missing in archive");
                    }
                }
            }
        }

        return feature;
    }
}
