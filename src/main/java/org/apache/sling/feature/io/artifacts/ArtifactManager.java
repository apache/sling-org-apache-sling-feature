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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.builder.FeatureProvider;
import org.apache.sling.feature.io.artifacts.spi.ArtifactProvider;
import org.apache.sling.feature.io.artifacts.spi.ArtifactProviderContext;
import org.apache.sling.feature.io.json.FeatureJSONReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The artifact manager is the central service to get artifacts.
 * It uses {@link ArtifactProvider}s to get artifacts. The
 * providers are loaded using the service loader.
 */
public class ArtifactManager
        implements AutoCloseable, org.apache.sling.feature.builder.ArtifactProvider {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The map of providers. */
    private final Map<String, ArtifactProvider> providers;

    /** The configuration */
    private final ArtifactManagerConfig config;

    /**
     * Get an artifact manager based on the configuration
     * @param config The configuration
     * @return The artifact manager
     * @throws IOException If the manager can't be initialized
     */
    public static ArtifactManager getArtifactManager(final ArtifactManagerConfig config) throws IOException {
        final ServiceLoader<ArtifactProvider> loader = ServiceLoader.load(ArtifactProvider.class);
        final Map<String, ArtifactProvider> providers = new HashMap<>();
        for(final ArtifactProvider provider : loader) {
            providers.put(provider.getProtocol(), provider);
        }

        final String[] repositoryURLs = new String[config.getRepositoryUrls().length];
        int index = 0;
        for(final String urlString : config.getRepositoryUrls()) {
            repositoryURLs[index] = urlString;
            index++;
        }
        // default
        if ( !providers.containsKey("*") ) {
            providers.put("*", new DefaultArtifactHandler());
        }

        return new ArtifactManager(config, providers);
    }

    /**
     * Internal constructor for the manager
     * @param config The configuration
     * @param providers The provider map
     * @throws IOException If the manager can't be initialized
     */
    ArtifactManager(final ArtifactManagerConfig config, final Map<String, ArtifactProvider> providers)
    throws IOException {
        this.config = config;
        this.providers = providers;
        try {
            for(final ArtifactProvider provider : this.providers.values()) {
                provider.init(config);
            }
        } catch ( final IOException io) {
            shutdown();
            throw io;
        }
    }

    /**
     * Shutdown the artifact manager.
     */
    public void shutdown() {
        for(final ArtifactProvider provider : this.providers.values()) {
            provider.shutdown();
        }
        this.providers.clear();
    }

    @Override
    public void close() {
        shutdown();
    }

    @Override
    public URL provide(final ArtifactId id) {
        try {
            final ArtifactHandler handler = this.getArtifactHandler(id.toMvnUrl());
            return handler.getLocalURL();
        } catch (final IOException e) {
            // ignore
            return null;
        }
    }

    /**
     * Return a feature provider based on this artifact manager
     *
     * @return A feature provider
     * @since 1.1.0
     */
    public FeatureProvider toFeatureProvider() {
        return (id -> {
            try {
                final ArtifactHandler handler = this.getArtifactHandler(id.toMvnUrl());
                try (final Reader r = new InputStreamReader(handler.getLocalURL().openStream(), "UTF-8")) {
                    final Feature f = FeatureJSONReader.read(r, handler.getUrl());
                    return f;
                }
            } catch (final IOException e) {
                // ignore
                return null;
            }
        });
    }

    private final URL getArtifactFromProviders(final String url, final String relativeCachePath) throws IOException {
        final int pos = url.indexOf(":");
        final String scheme = url.substring(0, pos);

        ArtifactProvider provider = this.providers.get(scheme);
        if ( provider == null ) {
            provider = this.providers.get("*");
        }
        if ( provider == null ) {
            throw new IOException("No URL provider found for " + url);
        }
        return provider.getArtifact(url, relativeCachePath);
    }

    /**
     * Get the full artifact url and file for an artifact.
     *
     * @param url Artifact url or relative path.
     * @return Absolute url and file in the form of a handler.
     * @throws IOException If something goes wrong or the artifact can't be found.
     */
    public ArtifactHandler getArtifactHandler(final String url) throws IOException {
        logger.debug("Trying to get artifact for {}", url);

        final String path;

        ArtifactId artifactId = null;

        if ( url.startsWith("mvn:") ) {
            // mvn url
            try {
                artifactId = ArtifactId.fromMvnUrl(url);
                path = artifactId.toMvnPath();
            } catch (final IllegalArgumentException iae) {
                throw new IOException(iae.getMessage(), iae);
            }
        } else if ( url.startsWith(":") ) {
            // repository path
            path = url.substring(1);

        } else if ( url.indexOf(":/") > 0 ) {

            // absolute URL
            int pos = url.indexOf(":/") + 2;
            while ( url.charAt(pos) == '/') {
                pos++;
            }
            final URL file = this.getArtifactFromProviders(url, url.substring(pos));
            if ( file == null ) {
                throw new IOException("Artifact " + url + " not found.");
            }
            return new ArtifactHandler(url, file);

        } else {
            // file (either relative or absolute)
            final File f = new File(url);
            if ( !f.exists()) {
                throw new IOException("Artifact " + url + " not found.");
            }
            return new ArtifactHandler(f);
        }
        logger.debug("Querying repositories for {}", path);

        for(final String repoUrl : this.config.getRepositoryUrls()) {
            final StringBuilder builder = new StringBuilder();
            builder.append(repoUrl);
            builder.append('/');
            builder.append(path);

            final String artifactUrl = builder.toString();
            final int pos = artifactUrl.indexOf(":");
            final String scheme = artifactUrl.substring(0, pos);

            ArtifactProvider handler = this.providers.get(scheme);
            if ( handler == null ) {
                handler = this.providers.get("*");
            }
            if ( handler == null ) {
                throw new IOException("No URL handler found for " + artifactUrl);
            }

            logger.debug("Checking {} to get artifact from {}", handler, artifactUrl);

            final URL file = handler.getArtifact(artifactUrl, path);
            if ( file != null ) {
                logger.debug("Found artifact {}", artifactUrl);
                return new ArtifactHandler(artifactUrl, file);
            }

            // check for SNAPSHOT
            final int lastSlash = artifactUrl.lastIndexOf('/');
            final int startSnapshot = artifactUrl.indexOf("-SNAPSHOT", lastSlash + 1);

            if ( startSnapshot > -1 ) {
                // special snapshot handling
                final String metadataUrl = artifactUrl.substring(0, lastSlash) + "/maven-metadata.xml";
                try {
                    final ArtifactHandler metadataHandler = this.getArtifactHandler(metadataUrl);

                    final String contents = getFileContents(metadataHandler);

                    final String latestVersion = getLatestSnapshot(contents);
                    if ( latestVersion != null ) {
                        final String name = artifactUrl.substring(lastSlash); // includes slash
                        final String fullURL = artifactUrl.substring(0, lastSlash) + name.replace("SNAPSHOT", latestVersion);
                        int pos2 = fullURL.indexOf(":/") + 2;
                        while ( fullURL.charAt(pos2) == '/') {
                            pos2++;
                        }
                        final URL file2 = this.getArtifactFromProviders(fullURL, path);
                        if ( file2 == null ) {
                            throw new IOException("Artifact " + fullURL + " not found.");
                        }
                        return new ArtifactHandler(artifactUrl, file2);
                    }
                } catch ( final IOException ignore ) {
                    // we ignore this but report the original 404
                }
            }
        }

        // if we have an artifact id and using mvn is enabled, we try this as a last
        // resort
        if (artifactId != null && this.config.isUseMvn()) {
            final File file = getArtifactFromMvn(artifactId);
            if (file != null) {
                return new ArtifactHandler(file);
            }
        }
        throw new IOException("Artifact " + url + " not found in any repository.");
    }

    protected String getFileContents(final ArtifactHandler handler) throws IOException {
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(handler.getLocalURL().openStream(), "UTF-8"))) {
            for(String line = reader.readLine(); line != null; line = reader.readLine()) {
                sb.append(line).append('\n');
            }
        }

        return sb.toString();
    }

    public static String getValue(final String xml, final String[] xpath) {
        String value = null;
        int pos = 0;
        for(final String name : xpath) {
            final String element = '<' + name + '>';

            pos = xml.indexOf(element, pos);
            if ( pos == -1 ) {
                final String elementWithAttributes = '<' + name + ' ';
                pos = xml.indexOf(elementWithAttributes, pos);
                if ( pos == -1 ) {
                    break;
                }
            }
            pos = xml.indexOf('>', pos) + 1;
        }
        if ( pos != -1 ) {
            final int endPos = xml.indexOf("</", pos);
            if ( endPos != -1 ) {
                value = xml.substring(pos, endPos).trim();
            }
        }
        return value;
    }

    public static String getLatestSnapshot(final String mavenMetadata) {
        final String timestamp = getValue(mavenMetadata, new String[] {"metadata", "versioning", "snapshot", "timestamp"});
        final String buildNumber = getValue(mavenMetadata, new String[] {"metadata", "versioning", "snapshot", "buildNumber"});

        if ( timestamp != null && buildNumber != null ) {
            return timestamp + '-' + buildNumber;
        }

        return null;
    }

    private static final class DefaultArtifactHandler implements ArtifactProvider {

        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private volatile File cacheDir;

        private volatile ArtifactProviderContext config;

        @Override
        public String getProtocol() {
            return "*";
        }

        @Override
        public void init(final ArtifactProviderContext config) throws IOException {
            this.cacheDir = config.getCacheDirectory();
            this.config = config;
        }

        @Override
        public void shutdown() {
            this.config = null;
            this.cacheDir = null;
        }

        @Override
        public URL getArtifact(final String url, final String relativeCachePath) {
            logger.debug("Checking url to be local file {}", url);
            // check if this is already a local file
            try {
                final File f = new File(new URL(url).toURI());
                if (f.exists()) {
                    this.config.incLocalArtifacts();
                    return f.toURI().toURL();
                }
                return null;
            } catch ( final URISyntaxException ise) {
                // ignore
            } catch ( final IllegalArgumentException iae) {
                // ignore
            } catch ( final MalformedURLException mue) {
                // ignore
            }
            logger.debug("Checking remote url {}", url);
            try {
                // check for url
                if ( url.indexOf(":") == -1 ) {
                    return null;
                }

                String adjustedRelativePath = relativeCachePath;
                // For Windows we need to remove the drive name from the path
                int pos = adjustedRelativePath.indexOf(":/");
                if(pos >= 0) {
                    adjustedRelativePath = adjustedRelativePath.substring(pos + 2);
                }
                final String filePath = (this.cacheDir.getAbsolutePath() + File.separatorChar + adjustedRelativePath).replace('/', File.separatorChar);
                final File cacheFile = new File(filePath);

                if ( !cacheFile.exists() ) {
                    cacheFile.getParentFile().mkdirs();
                    final URL u = new URL(url);
                    final URLConnection con = u.openConnection();
                    final String userInfo = u.getUserInfo();
                    if (userInfo != null) {
                        con.addRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(u.toURI().getUserInfo().getBytes("UTF-8")));
                    }
                    con.connect();

                    final InputStream readIS = con.getInputStream();
                    try {
                        copyFileContent(readIS, cacheFile, 32768);
                    } catch(IOException e) {
                        //TODO: Remove this logging statement when it settled down
                        logger.debug("Failed to copy file", e);
                        throw e;
                    }
                    this.config.incDownloadedArtifacts();
                } else {
                    this.config.incCachedArtifacts();
                }
                return cacheFile.toURI().toURL();
            } catch ( final FileNotFoundException e) {
                logger.trace("File not found here (keep on looking): '{}'", url);
                // Do not report if the file does not exist as we cycle through the various sources
                return null;
            } catch ( final Exception e) {
                logger.info("Artifact not found in one repository", e);
                // ignore for now
                return null;
            }
        }

        @Override
        public String toString() {
            return "DefaultArtifactHandler";
        }

        private void copyFileContent(InputStream readIS, File cacheFile, int bufferSize) throws IOException {
            final byte[] buffer = new byte[bufferSize];
            int l;
            OutputStream os = null;
            try {
                os = new FileOutputStream(cacheFile);
                while ( (l = readIS.read(buffer)) >= 0 ) {
                    os.write(buffer, 0, l);
                }
            } finally {
                try {
                    readIS.close();
                } catch ( final IOException ignore) {
                    // ignore
                }
                if ( os != null ) {
                    try {
                        os.close();
                    } catch ( final IOException ignore ) {
                        // ignore
                    }
                }
            }
        }
    }

    private File getArtifactFromMvn(final ArtifactId artifactId) {
        final String filePath = this.config.getMvnHome()
                .concat(artifactId.toMvnPath().replace('/', File.separatorChar));
        logger.debug("Trying to fetch artifact {} from local mvn repository {}", artifactId.toMvnId(), filePath);
        final File f = new File(filePath);
        if (!f.exists() || !f.isFile() || !f.canRead()) {
            logger.debug("Trying to download {}", artifactId.toMvnId());
            try {
                this.downloadArtifact(artifactId);
            } catch (final IOException ioe) {
                logger.debug("Error downloading file.", ioe);
            }
            if (!f.exists() || !f.isFile() || !f.canRead()) {
                logger.info("Artifact not found {}", artifactId.toMvnId());

                return null;
            }
        }
        return f;
    }

    /**
     * Download artifact from maven
     *
     * @throws IOException
     */
    private void downloadArtifact(final ArtifactId artifactId) throws IOException {
        // create fake pom
        final Path dir = Files.createTempDirectory(null);
        try {
            final List<String> lines = new ArrayList<String>();
            lines.add(
                    "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">");
            lines.add("    <modelVersion>4.0.0</modelVersion>");
            lines.add("    <groupId>org.apache.sling</groupId>");
            lines.add("    <artifactId>temp-artifact</artifactId>");
            lines.add("    <version>1-SNAPSHOT</version>");
            lines.add("    <dependencies>");
            lines.add("        <dependency>");
            lines.add("            <groupId>".concat(artifactId.getGroupId()).concat("</groupId>"));
            lines.add("            <artifactId>".concat(artifactId.getArtifactId()).concat("</artifactId>"));
            lines.add("            <version>".concat(artifactId.getVersion()).concat("</version>"));
            if (artifactId.getClassifier() != null) {
                lines.add("            <classifier>".concat(artifactId.getClassifier()).concat("</classifier>"));
            }
            if (!"bundle".equals(artifactId.getType()) && !"jar".equals(artifactId.getType())) {
                lines.add("            <type>".concat(artifactId.getType()).concat("</type>"));
            }
            lines.add("            <scope>provided</scope>");
            lines.add("        </dependency>");
            lines.add("    </dependencies>");
            lines.add("</project>");
            logger.debug("Writing pom to {}", dir);
            Files.write(dir.resolve("pom.xml"), lines, Charset.forName("UTF-8"));

            final File output = dir.resolve("output.txt").toFile();
            final File error = dir.resolve("error.txt").toFile();

            // invoke maven
            logger.debug("Invoking mvn...");
            final ProcessBuilder pb = new ProcessBuilder("mvn", "verify");
            pb.directory(dir.toFile());
            pb.redirectOutput(Redirect.to(output));
            pb.redirectError(Redirect.to(error));

            final Process p = pb.start();
            try {
                p.waitFor();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } finally {
            Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
