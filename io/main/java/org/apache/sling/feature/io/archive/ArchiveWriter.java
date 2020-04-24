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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.Deflater;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.apache.sling.feature.io.json.FeatureJSONWriter;

/**
 * The feature archive writer can be used to create an archive based on a
 * feature model. The archive contains the feature model file and all artifacts
 * using a maven repository layout.
 */
public class ArchiveWriter {

    /** The manifest header marking an archive as a feature archive. */
    public static final String VERSION_HEADER = "Feature-Archive-Version";

    /** The manifest header listing the features in this archive. */
    public static final String CONTENTS_HEADER = "Feature-Archive-Contents";

    /** Current support version of the feature model archive. */
    public static final int ARCHIVE_VERSION = 1;

    /**
     * Create a feature model archive. The output stream will not be closed by this
     * method. The caller must call {@link JarOutputStream#close()}
     * on the return output stream. The caller can
     * add additional files through the return stream. However, the files
     * should not be compressed (which is the default for the output stream).
     *
     * A feature model can be in different states: it might be a partial feature
     * model, a complete feature model or an assembled feature model. This method
     * takes the feature model as provided and only writes the listed bundles and
     * artifacts of this feature model into the archive. In general, the best
     * approach for sharing features is to archive {@link Feature#isComplete()
     * complete} features.
     *
     * @param out          The output stream to write to
     * @param baseManifest Optional base manifest used for creating the manifest.
     * @param provider     The artifact provider
     * @param features     The features model to archive
     * @return The jar output stream.
     * @throws IOException If anything goes wrong
     */
    public static JarOutputStream write(final OutputStream out,
            final Manifest baseManifest,
            final ArtifactProvider provider, final Feature... features)
    throws IOException {
        // create manifest
        final Manifest manifest = (baseManifest == null ? new Manifest() : new Manifest(baseManifest));
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue(VERSION_HEADER, String.valueOf(ARCHIVE_VERSION));
        manifest.getMainAttributes().putValue(CONTENTS_HEADER, String.join(",", Arrays.asList(features).stream()
                .map(feature -> feature.getId().toMvnId()).collect(Collectors.toList())));

        final Set<ArtifactId> artifacts = new HashSet<>();
        final byte[] buffer = new byte[1024*1024*256];

        // create archive
        final JarOutputStream jos = new JarOutputStream(out, manifest);

        // write everything without compression
        jos.setLevel(Deflater.NO_COMPRESSION);
        for (final Feature feature : features) {
            writeFeature(artifacts, feature, provider, jos, buffer);
        }



        for (final Feature feature : features) {
            for (final Artifact a : feature.getBundles()) {
                writeArtifact(artifacts, provider, a, jos, buffer);
            }

            for (final Extension e : feature.getExtensions()) {
                if (e.getType() == ExtensionType.ARTIFACTS) {
                    final boolean isFeature = Extension.EXTENSION_NAME_ASSEMBLED_FEATURES.equals(e.getName());
                    for (final Artifact a : e.getArtifacts()) {
                        if ( isFeature ) {
                            writeFeature(artifacts, provider, a.getId(), jos, buffer);
                        } else {
                            writeArtifact(artifacts, provider, a, jos, buffer);
                        }
                    }
                }
            }
        }
        return jos;
    }

    private static void writeFeature(final Set<ArtifactId> artifacts,
            final Feature feature,
            final ArtifactProvider provider,
            final JarOutputStream jos, final byte[] buffer) throws IOException {
        if ( artifacts.add(feature.getId())) {
            final JarEntry entry = new JarEntry(feature.getId().toMvnPath());
            jos.putNextEntry(entry);
            final Writer writer = new OutputStreamWriter(jos, StandardCharsets.UTF_8);
            FeatureJSONWriter.write(writer, feature);
            writer.flush();
            jos.closeEntry();

            if ( feature.getPrototype() != null ) {
                writeFeature(artifacts, provider, feature.getPrototype().getId(), jos, buffer);
            }
        }
    }

    private static void writeFeature(final Set<ArtifactId> artifacts,
            final ArtifactProvider provider,
            final ArtifactId featureId,
            final JarOutputStream jos, final byte[] buffer) throws IOException {
        if ( !artifacts.contains(featureId)) {
            final URL url = provider.provide(featureId);
            try ( final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                  final InputStream is = url.openStream()) {
                int l = 0;
                while ( (l = is.read(buffer)) > 0 ) {
                    baos.write(buffer, 0, l);
                }
                final String contents = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                try ( final Reader reader = new StringReader(contents)) {
                    final Feature feature = FeatureJSONReader.read(reader, featureId.toMvnId());
                    writeFeature(artifacts, feature, provider, jos, buffer);
                }
            }
        }
    }

    private static void writeArtifact(final Set<ArtifactId> artifacts,
            final ArtifactProvider provider,
            final Artifact artifact,
            final JarOutputStream jos,
            final byte[] buffer) throws IOException {
        if ( artifacts.add(artifact.getId())) {
            final JarEntry artifactEntry = new JarEntry(artifact.getId().toMvnPath());
            jos.putNextEntry(artifactEntry);

            final URL url = provider.provide(artifact.getId());
            if (url == null) {
                throw new IOException("Unable to find artifact " + artifact.getId().toMvnId());
            }
            try (final InputStream is = url.openStream()) {
                int l = 0;
                while ( (l = is.read(buffer)) > 0 ) {
                    jos.write(buffer, 0, l);
                }
            }
            jos.closeEntry();
        }
    }
}
