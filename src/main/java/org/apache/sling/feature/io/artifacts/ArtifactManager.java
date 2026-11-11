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
package org.apache.sling.feature.io.artifacts;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ProcessBuilder.Redirect;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class ArtifactManager implements AutoCloseable, org.apache.sling.feature.builder.ArtifactProvider {

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
        for (final ArtifactProvider provider : loader) {
            providers.put(provider.getProtocol(), provider);
        }

        final String[] repositoryURLs = new String[config.getRepositoryUrls().length];
        int index = 0;
        for (final String urlString : config.getRepositoryUrls()) {
            repositoryURLs[index] = urlString;
            index++;
        }
        // default
        if (!providers.containsKey("*")) {
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
            for (final ArtifactProvider provider : this.providers.values()) {
                provider.init(config);
            }
        } catch (final IOException io) {
            shutdown();
            throw io;
        }
    }

    /**
     * Shutdown the artifact manager.
     */
    public void shutdown() {
        for (final ArtifactProvider provider : this.providers.values()) {
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
                try (final Reader r =
                        new InputStreamReader(handler.getLocalURL().openStream(), "UTF-8")) {
                    final Feature f = FeatureJSONReader.read(r, handler.getUrl());
                    return f;
                }
            } catch (final IOException e) {
                // ignore
                return null;
            }
        });
    }

    ArtifactProvider getArtifactProvider(final String scheme) {
        ArtifactProvider provider = this.providers.get(scheme);
        if (provider == null) {
            provider = this.providers.get("*");
        }
        return provider;
    }

    private final URL getArtifactFromProviders(final String url, final String relativeCachePath) throws IOException {
        final int pos = url.indexOf(":");
        final String scheme = url.substring(0, pos);

        final ArtifactProvider provider = this.getArtifactProvider(scheme);
        if (provider == null) {
            throw new IOException("No URL provider found for " + url);
        }
        URL file = provider.getArtifactFile(url, relativeCachePath);
        if (file == null) {
            file = provider.getArtifact(url, relativeCachePath);
        }
        if (file == null) {
            throw new IOException("Artifact not found: " + url);
        }
        return file;
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

        if (url.startsWith("mvn:")) {
            // mvn url
            try {
                artifactId = ArtifactId.fromMvnUrl(url);
                path = artifactId.toMvnPath();
            } catch (final IllegalArgumentException iae) {
                throw new IOException(iae.getMessage(), iae);
            }
        } else if (url.startsWith(":")) {
            // repository path
            path = url.substring(1);

        } else if (url.indexOf(":/") > 0) {

            // absolute URL
            int pos = url.indexOf(":/") + 2;
            while (url.charAt(pos) == '/') {
                pos++;
            }
            final URL file = this.getArtifactFromProviders(url, url.substring(pos));
            if (file == null) {
                throw new IOException("Artifact " + url + " not found.");
            }
            return new ArtifactHandler(url, file);

        } else {
            // file (either relative or absolute)
            final Path f = Paths.get(url);
            if (!Files.exists(f)) {
                throw new IOException("Artifact " + url + " not found.");
            }
            return new ArtifactHandler(f);
        }
        logger.debug("Querying repositories for {}", path);

        final Map<String, Exception> failures = new LinkedHashMap<>();
        for (final String repoUrl : this.config.getRepositoryUrls()) {
            final StringBuilder builder = new StringBuilder();
            builder.append(repoUrl);
            builder.append('/');
            builder.append(path);

            final String artifactUrl = builder.toString();
            final int pos = artifactUrl.indexOf(":");
            final String scheme = artifactUrl.substring(0, pos);

            ArtifactProvider handler = this.getArtifactProvider(scheme);
            if (handler == null) {
                failures.put(repoUrl, new IOException("No URL handler found for " + artifactUrl));
                continue;
            }

            logger.debug("Checking {} to get artifact from {}", handler, artifactUrl);

            URL file = null;
            try {
                file = handler.getArtifactFile(artifactUrl, path);
            } catch (final IOException e) {
                logger.debug("Failed to get artifact from {}", artifactUrl, e);
                failures.put(repoUrl, e);
            }
            if (file == null && !failures.containsKey(repoUrl)) {
                file = handler.getArtifact(artifactUrl, path);
                if (file == null) {
                    failures.put(repoUrl, new IOException("Artifact not found: " + artifactUrl));
                }
            }
            if (file != null) {
                logger.debug("Found artifact {}", artifactUrl);
                return new ArtifactHandler(artifactUrl, file);
            }

            // check for SNAPSHOT
            final int lastSlash = artifactUrl.lastIndexOf('/');
            final int startSnapshot = artifactUrl.indexOf("-SNAPSHOT", lastSlash + 1);

            if (startSnapshot > -1) {
                // special snapshot handling
                final String metadataUrl = artifactUrl.substring(0, lastSlash) + "/maven-metadata.xml";
                try {
                    final ArtifactHandler metadataHandler = this.getArtifactHandler(metadataUrl);

                    final String contents = getFileContents(metadataHandler);

                    final String latestVersion = getLatestSnapshot(contents);
                    if (latestVersion != null) {
                        final String name = artifactUrl.substring(lastSlash); // includes slash
                        final String fullURL =
                                artifactUrl.substring(0, lastSlash) + name.replace("SNAPSHOT", latestVersion);
                        int pos2 = fullURL.indexOf(":/") + 2;
                        while (fullURL.charAt(pos2) == '/') {
                            pos2++;
                        }
                        final URL file2 = this.getArtifactFromProviders(fullURL, path);
                        return new ArtifactHandler(artifactUrl, file2);
                    }
                } catch (final IOException ignore) {
                    // we ignore this but report the original 404
                }
            }
        }

        // if we have an artifact id and using mvn is enabled, we try this as a last
        // resort
        if (artifactId != null && this.config.isUseMvn()) {
            final Path file = getArtifactFromMvn(artifactId);
            if (file != null) {
                return new ArtifactHandler(file);
            }
        }

        if (failures.isEmpty()) {
            throw new IOException("Artifact " + url + " not found in any repository.");
        }
        final StringBuilder message = new StringBuilder("Artifact ");
        message.append(url).append(" not found in configured repositories:");
        for (final Map.Entry<String, Exception> entry : failures.entrySet()) {
            message.append("\n  - ")
                    .append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue().getMessage());
        }
        final IOException exception = new IOException(message.toString());
        for (final Exception failure : failures.values()) {
            exception.addSuppressed(failure);
        }
        throw exception;
    }

    protected String getFileContents(final ArtifactHandler handler) throws IOException {
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(handler.getLocalURL().openStream(), "UTF-8"))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                sb.append(line).append('\n');
            }
        }

        return sb.toString();
    }

    public static String getValue(final String xml, final String[] xpath) {
        String value = null;
        int pos = 0;
        for (final String name : xpath) {
            final String element = '<' + name + '>';

            pos = xml.indexOf(element, pos);
            if (pos == -1) {
                final String elementWithAttributes = '<' + name + ' ';
                pos = xml.indexOf(elementWithAttributes, pos);
                if (pos == -1) {
                    break;
                }
            }
            pos = xml.indexOf('>', pos) + 1;
        }
        if (pos != -1) {
            final int endPos = xml.indexOf("</", pos);
            if (endPos != -1) {
                value = xml.substring(pos, endPos).trim();
            }
        }
        return value;
    }

    public static String getLatestSnapshot(final String mavenMetadata) {
        final String timestamp =
                getValue(mavenMetadata, new String[] {"metadata", "versioning", "snapshot", "timestamp"});
        final String buildNumber =
                getValue(mavenMetadata, new String[] {"metadata", "versioning", "snapshot", "buildNumber"});

        if (timestamp != null && buildNumber != null) {
            return timestamp + '-' + buildNumber;
        }

        return null;
    }

    private static final class DefaultArtifactHandler implements ArtifactProvider {

        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private Path cacheDir;

        private boolean isNewlyCreatedCacheDir;
        private ArtifactProviderContext config;

        @Override
        public String getProtocol() {
            return "*";
        }

        @Override
        public void init(final ArtifactProviderContext config) throws IOException {
            File cd = config.getCacheDirectory();
            if (cd != null) {
                this.cacheDir = cd.toPath();
            } else {
                this.cacheDir = Files.createTempDirectory("slingfeature");
                isNewlyCreatedCacheDir = true;
            }
            this.config = config;
        }

        @Override
        public void shutdown() {
            this.config = null;
            if (isNewlyCreatedCacheDir) {
                try {
                    deleteDirectoryRecursively(cacheDir);
                } catch (IOException e) {
                    logger.warn("Could not remove temp directory at " + cacheDir, e);
                }
            }
            this.cacheDir = null;
        }

        @Override
        public URL getArtifact(final String url, final String relativeCachePath) {
            try {
                return getArtifactFile(url, relativeCachePath);
            } catch (final IOException e) {
                logger.info("Artifact not found in one repository", e);
                return null;
            }
        }

        private URL checkLocalFile(final String url) throws IOException {
            logger.debug("Checking url to be local file {}", url);
            try {
                final URI uri = new URI(url);
                if (FileSystems.getDefault().provider().getScheme().equals(uri.getScheme())) {
                    final Path f = Paths.get(uri);
                    if (Files.exists(f)) {
                        this.config.incLocalArtifacts();
                        return f.toUri().toURL();
                    }
                    throw new IOException("Local file does not exist: " + url);
                }
            } catch (final URISyntaxException | MalformedURLException ignored) {
                // ignore
            }
            return null;
        }

        private Path getCacheFile(final String relativeCachePath) {
            String adjustedRelativePath = relativeCachePath;
            // For Windows we need to remove the drive name from the path
            int pos = adjustedRelativePath.indexOf(":/");
            if (pos >= 0) {
                adjustedRelativePath = adjustedRelativePath.substring(pos + 2);
            }
            return cacheDir.resolve(adjustedRelativePath.replace(
                    "/", java.nio.file.FileSystems.getDefault().getSeparator()));
        }

        private void setupAuthorization(final URL u, final URLConnection con) throws IOException {
            final String userInfo = u.getUserInfo();
            if (userInfo != null) {
                try {
                    con.addRequestProperty(
                            "Authorization",
                            "Basic "
                                    + Base64.getEncoder()
                                            .encodeToString(
                                                    u.toURI().getUserInfo().getBytes("UTF-8")));
                } catch (final URISyntaxException use) {
                    throw new IOException("Malformed user info URI in " + u, use);
                }
            }
        }

        private void downloadRemoteArtifact(final String url, final Path cacheFile) throws IOException {
            Files.createDirectories(cacheFile.getParent());
            final URL u = new URL(url);
            final URLConnection con = u.openConnection();
            setupAuthorization(u, con);
            try {
                con.connect();
            } catch (final IOException ce) {
                throw new IOException("Connection failed to " + url + ": " + ce.getMessage(), ce);
            }

            try (InputStream input = con.getInputStream()) {
                Files.copy(input, cacheFile);
            } catch (IOException e) {
                if (con instanceof HttpURLConnection) {
                    final HttpURLConnection httpCon = (HttpURLConnection) con;
                    int code = -1;
                    String msg = null;
                    try {
                        code = httpCon.getResponseCode();
                        msg = httpCon.getResponseMessage();
                    } catch (final IOException ignored) {
                        // fall back to e
                    }
                    if (code != -1) {
                        throw new IOException(
                                "HTTP " + code + (msg != null && !msg.isEmpty() ? " " + msg : "") + " from " + url, e);
                    }
                }
                throw new IOException("Failed to download artifact from " + url + ": " + e.getMessage(), e);
            }
            this.config.incDownloadedArtifacts();
        }

        @Override
        public URL getArtifactFile(final String url, final String relativeCachePath) throws IOException {
            final URL localFile = checkLocalFile(url);
            if (localFile != null) {
                return localFile;
            }

            logger.debug("Checking remote url {}", url);
            if (url.indexOf(":") == -1) {
                throw new IOException("Malformed URL without scheme: " + url);
            }

            final Path cacheFile = getCacheFile(relativeCachePath);
            if (!Files.exists(cacheFile)) {
                downloadRemoteArtifact(url, cacheFile);
            } else {
                this.config.incCachedArtifacts();
            }
            return cacheFile.toUri().toURL();
        }

        @Override
        public String toString() {
            return "DefaultArtifactHandler";
        }
    }

    private Path getArtifactFromMvn(final ArtifactId artifactId) {
        final Path filePath = Paths.get(
                config.getMvnHome(),
                artifactId
                        .toMvnPath()
                        .replace("/", java.nio.file.FileSystems.getDefault().getSeparator()));
        logger.debug("Trying to fetch artifact {} from local mvn repository {}", artifactId.toMvnId(), filePath);
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath) || Files.isReadable(filePath)) {
            logger.debug("Trying to download {}", artifactId.toMvnId());
            try {
                this.downloadArtifact(artifactId);
            } catch (final IOException ioe) {
                logger.debug("Error downloading file.", ioe);
            }
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath) || !Files.isReadable(filePath)) {
                logger.info("Artifact not found {}", artifactId.toMvnId());

                return null;
            }
        }
        return filePath;
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
            lines.add("            <artifactId>"
                    .concat(artifactId.getArtifactId())
                    .concat("</artifactId>"));
            lines.add("            <version>".concat(artifactId.getVersion()).concat("</version>"));
            if (artifactId.getClassifier() != null) {
                lines.add("            <classifier>"
                        .concat(artifactId.getClassifier())
                        .concat("</classifier>"));
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

            final Path output = dir.resolve("output.txt");
            final Path error = dir.resolve("error.txt");

            // invoke maven
            logger.debug("Invoking mvn...");
            final ProcessBuilder pb = new ProcessBuilder("mvn", "verify");
            pb.directory(dir.toFile());
            pb.redirectOutput(Redirect.to(output.toFile()));
            pb.redirectError(Redirect.to(error.toFile()));

            final Process p = pb.start();
            try {
                p.waitFor();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } finally {
            deleteDirectoryRecursively(dir);
        }
    }

    private static void deleteDirectoryRecursively(Path directory) throws IOException {
        try (Stream<Path> filesStream = Files.walk(directory)) {
            final List<Path> pathsToDelete =
                    filesStream.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path path : pathsToDelete) {
                Files.deleteIfExists(path);
            }
        }
    }
}
