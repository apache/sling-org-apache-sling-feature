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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

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

    @FunctionalInterface
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
     * @param in       The input stream to read from.
     * @param consumer The plugin consuming the binaries, including the features.
     *                 If no consumer is provided, only the features will be returned.
     * @return The feature models mentioned in the manifest of the archive
     * @throws IOException If anything goes wrong
     */
    public static Set<Feature> read(final InputStream in,
                             final ArtifactConsumer consumer)
    throws IOException {
        final JarInputStream jis = new JarInputStream(in);

        // validate manifest and get feature ids
        final String[] featureIds = checkHeaderAndExtractContents(jis.getManifest());
        final List<String> featurePaths = Arrays.asList(featureIds).stream()
                .map(id -> ArtifactId.parse(id).toMvnPath()).collect(Collectors.toList());


        // read contents
        final Set<Feature> features = new HashSet<>();
        final Set<ArtifactId> artifacts = new HashSet<>();

        JarEntry entry = null;
        while ( ( entry = jis.getNextJarEntry() ) != null ) {
            if (!entry.isDirectory() && !entry.getName().startsWith("META-INF/")) {
                final ArtifactId id = ArtifactId.fromMvnPath(entry.getName());

                if (featurePaths.contains(entry.getName())) {
                    // feature - read to string first
                    final String contents;
                    try ( final StringWriter writer = new StringWriter()) {
                        // don't close the input stream
                        final Reader reader = new InputStreamReader(jis, "UTF-8");
                        final char[] buffer = new char[2048];
                        int l;
                        while ( (l = reader.read(buffer)) > 0) {
                            writer.write(buffer, 0, l);
                        }
                        writer.flush();
                        contents = writer.toString();
                    }
                    // add to features
                    try ( final Reader reader = new StringReader(contents) ) {
                        final Feature feature = FeatureJSONReader.read(reader, entry.getName());
                        features.add(feature);
                    }
                    // pass to consumer
                    if ( consumer != null ) {
                        try ( final InputStream is = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8))) {
                            consumer.consume(id, is);
                        }
                    }
                } else {
                    // artifact
                    if (consumer != null) {
                        consumer.consume(id, jis);
                    }
                    artifacts.add(id);
                }
            }
            jis.closeEntry();
        }
        if (features.isEmpty()) {
            throw new IOException("Not a feature model archive - feature file is missing.");
        }

        // check whether all artifacts from the models are in the archive
        for (final Feature feature : features) {
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
        }
        return features;
    }

    private static String[] checkHeaderAndExtractContents(final Manifest manifest) throws IOException {
        if (manifest == null) {
            throw new IOException("Not a feature model archive - manifest is missing.");
        }
        // check version header
        final String version = manifest.getMainAttributes().getValue(ArchiveWriter.VERSION_HEADER);
        if (version == null) {
            throw new IOException("Not a feature model archive - version manifest header is missing.");
        }
        // validate version header
        try {
            final int number = Integer.valueOf(version);
            if (number < 1 || number > ArchiveWriter.ARCHIVE_VERSION) {
                throw new IOException("Not a feature model archive - invalid manifest header value: " + version);
            }
        } catch (final NumberFormatException nfe) {
            throw new IOException("Not a feature model archive - invalid manifest header value: " + version);
        }

        // check contents header
        final String contents = manifest.getMainAttributes().getValue(ArchiveWriter.CONTENTS_HEADER);
        if (contents == null) {
            throw new IOException("Not a feature model archive - contents manifest header is missing.");
        }

        return contents.split(",");
    }
}
