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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.sling.feature.io.artifacts.spi.ArtifactProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArtifactManagerTest {

    private static final String METADATA =
            "<metadata modelVersion=\"1.1.0\">\n" + "<groupId>org.apache.sling.samples</groupId>\n"
                    + "<artifactId>slingshot</artifactId>\n"
                    + "<version>0-DEFAULT-SNAPSHOT</version>\n"
                    + "<versioning>\n"
                    + "<snapshot>\n"
                    + "<timestamp>20160321.103951</timestamp>\n"
                    + "<buildNumber>1</buildNumber>\n"
                    + "</snapshot>\n"
                    + "<lastUpdated>20160321103951</lastUpdated>\n"
                    + "<snapshotVersions>\n"
                    + "<snapshotVersion>\n"
                    + "<extension>txt</extension>\n"
                    + "<value>0-DEFAULT-20160321.103951-1</value>\n"
                    + "<updated>20160321103951</updated>\n"
                    + "</snapshotVersion>\n"
                    + "<snapshotVersion>\n"
                    + "<extension>pom</extension>\n"
                    + "<value>0-DEFAULT-20160321.103951-1</value>\n"
                    + "<updated>20160321103951</updated>\n"
                    + "</snapshotVersion>\n"
                    + "</snapshotVersions>\n"
                    + "</versioning></metadata>";

    @Test
    public void testMetadataParsing() {
        final String version = ArtifactManager.getLatestSnapshot(METADATA);
        assertEquals("20160321.103951-1", version);
    }

    @Test
    public void testSnapshotHandling() throws IOException {
        final String REPO = "http://org.apache.sling";
        final ArtifactManagerConfig config = mock(ArtifactManagerConfig.class);
        when(config.getRepositoryUrls()).thenReturn(new String[] {REPO});

        final URL metadataFile = new URL("file:/maven-metadata.xml");

        final URL artifactFile = new URL("file:/artifact");

        final ArtifactProvider provider = mock(ArtifactProvider.class);
        when(provider.getArtifact(
                        REPO + "/group/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-SNAPSHOT.txt",
                        "group/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-SNAPSHOT.txt"))
                .thenReturn(null);
        when(provider.getArtifact(
                        REPO + "/group/artifact/1.0.0-SNAPSHOT/maven-metadata.xml",
                        "org.apache.sling/group/artifact/1.0.0-SNAPSHOT/maven-metadata.xml"))
                .thenReturn(metadataFile);
        when(provider.getArtifact(
                        REPO + "/group/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-20160321.103951-1.txt",
                        "group/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-SNAPSHOT.txt"))
                .thenReturn(artifactFile);

        final Map<String, ArtifactProvider> providers = new HashMap<>();
        providers.put("*", provider);

        final ArtifactManager mgr = new ArtifactManager(config, providers) {

            @Override
            protected String getFileContents(final ArtifactHandler handler) throws IOException {
                final String path = handler.getLocalURL().getPath();
                if ("/maven-metadata.xml".equals(path)) {
                    return METADATA;
                }
                return super.getFileContents(handler);
            }
        };

        final ArtifactHandler handler = mgr.getArtifactHandler("mvn:group/artifact/1.0.0-SNAPSHOT/txt");
        assertNotNull(handler);
        assertEquals(artifactFile, handler.getLocalURL());
    }

    @Test
    public void testGetArtifactManager() throws Exception {
        ArtifactManagerConfig cfg = new ArtifactManagerConfig();
        ArtifactManager am = ArtifactManager.getArtifactManager(cfg);

        am.shutdown();
    }

    @Test
    public void testGetArtifactManagerWithCachedir() throws Exception {
        ArtifactManagerConfig cfg = new ArtifactManagerConfig();
        Path tempDir = Files.createTempDirectory("testGetArtifactManagerWithCachedir");
        cfg.setCacheDirectory(tempDir.toFile());

        ArtifactManager am = ArtifactManager.getArtifactManager(cfg);

        am.shutdown();
        assertTrue(tempDir.toFile().isDirectory());
        assertTrue(tempDir.toFile().delete());
    }

    @Test
    public void testGetArtifactHandler() throws Exception {
        ArtifactManagerConfig cfg = new ArtifactManagerConfig();
        cfg.setRepositoryUrls(new String[] {"https://repo.maven.apache.org/maven2"});

        ArtifactManager am = ArtifactManager.getArtifactManager(cfg);

        assertNotNull(am.getArtifactHandler(
                ":org/apache/felix/org.apache.felix.framework/7.0.1/org.apache.felix.framework-7.0.1.jar"));
        assertNotNull(am.getArtifactHandler("mvn:org.apache.felix/org.apache.felix.framework/7.0.1"));
        assertNotNull(
                am.getArtifactHandler(
                        "src/test/resources/m2/org/apache/felix/org.apache.felix.framework/7.0.1/org.apache.felix.framework-7.0.1.jar"));

        String fooProtocol = "foo" + new Random().nextLong();
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                if (fooProtocol.equals(protocol)) {
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) throws IOException {
                            return new File(
                                            "src/test/resources/m2/org/apache/felix/org.apache.felix.framework/7.0.1/org.apache.felix.framework-7.0.1.jar")
                                    .toURL()
                                    .openConnection();
                        }
                    };
                } else {
                    return null;
                }
            }
        });
        assertNotNull(am.getArtifactHandler(fooProtocol + ":/felix.jar"));

        am.shutdown();
    }

    @Test
    public void testArtifactNotFoundDetailedException() throws IOException {
        final String repo1 = "http://repo1.example.com";
        final String repo2 = "http://repo2.example.com";
        final ArtifactManagerConfig config = mock(ArtifactManagerConfig.class);
        when(config.getRepositoryUrls()).thenReturn(new String[] {repo1, repo2});

        final ArtifactProvider provider = mock(ArtifactProvider.class);
        when(provider.getArtifactFile(
                        repo1 + "/group/artifact/1.0.0/artifact-1.0.0.jar", "group/artifact/1.0.0/artifact-1.0.0.jar"))
                .thenThrow(new IOException("HTTP 404 Not Found from " + repo1));
        when(provider.getArtifactFile(
                        repo2 + "/group/artifact/1.0.0/artifact-1.0.0.jar", "group/artifact/1.0.0/artifact-1.0.0.jar"))
                .thenThrow(new IOException("HTTP 401 Unauthorized from " + repo2));

        final Map<String, ArtifactProvider> providers = new HashMap<>();
        providers.put("*", provider);

        final ArtifactManager mgr = new ArtifactManager(config, providers);
        try {
            mgr.getArtifactHandler("mvn:group/artifact/1.0.0");
            fail("Expected IOException");
        } catch (final IOException e) {
            assertTrue(e.getMessage().contains("mvn:group/artifact/1.0.0"));
            assertTrue(e.getMessage().contains("HTTP 404 Not Found from " + repo1));
            assertTrue(e.getMessage().contains("HTTP 401 Unauthorized from " + repo2));
            assertEquals(2, e.getSuppressed().length);
        }
    }

    @Test
    public void testArtifactProviderDefaultMethod() throws IOException {
        final URL expectedUrl = new URL("file:/some/path/artifact.jar");
        final ArtifactProvider provider = new ArtifactProvider() {
            @Override
            public String getProtocol() {
                return "test";
            }

            @Override
            public void init(final org.apache.sling.feature.io.artifacts.spi.ArtifactProviderContext context) {
                // Not needed for testing default methods
            }

            @Override
            public void shutdown() {
                // Not needed for testing default methods
            }

            @Override
            public URL getArtifact(final String url, final String relativeCachePath) {
                if ("http://found.example.com".equals(url)) {
                    return expectedUrl;
                }
                return null;
            }
        };

        assertEquals(expectedUrl, provider.getArtifactFile("http://found.example.com", "artifact.jar"));
        try {
            provider.getArtifactFile("http://notfound.example.com", "artifact.jar");
            fail("Expected IOException");
        } catch (final IOException e) {
            assertTrue(e.getMessage().contains("Artifact not found: http://notfound.example.com"));
        }
    }

    @Test
    public void testArtifactManagerFailuresEmpty() throws IOException {
        final ArtifactManagerConfig config = mock(ArtifactManagerConfig.class);
        when(config.getRepositoryUrls()).thenReturn(new String[0]);

        final ArtifactManager mgr = new ArtifactManager(config, new HashMap<>());
        try {
            mgr.getArtifactHandler("mvn:group/artifact/1.0.0");
            fail("Expected IOException");
        } catch (final IOException e) {
            assertTrue(e.getMessage().contains("Artifact mvn:group/artifact/1.0.0 not found in any repository."));
        }
    }

    @Test
    public void testDefaultArtifactHandlerHttpOperations() throws Exception {
        final com.sun.net.httpserver.HttpServer server =
                com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/repo", new com.sun.net.httpserver.HttpHandler() {
            @Override
            public void handle(final com.sun.net.httpserver.HttpExchange exchange) throws IOException {
                final String path = exchange.getRequestURI().getPath();
                if (path.endsWith("found.jar")) {
                    final byte[] response = "found-artifact".getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, response.length);
                    try (java.io.OutputStream os = exchange.getResponseBody()) {
                        os.write(response);
                    }
                } else if (path.endsWith("auth.jar")) {
                    final String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                    if ("Basic dXNlcjpwYXNz".equals(authHeader)) {
                        final byte[] response = "auth-artifact".getBytes("UTF-8");
                        exchange.sendResponseHeaders(200, response.length);
                        try (java.io.OutputStream os = exchange.getResponseBody()) {
                            os.write(response);
                        }
                    } else {
                        exchange.sendResponseHeaders(401, -1);
                    }
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            }
        });
        server.start();

        try {
            final int port = server.getAddress().getPort();
            final Path tempCache = Files.createTempDirectory("test-cache");
            final ArtifactManagerConfig cfg = new ArtifactManagerConfig();
            cfg.setCacheDirectory(tempCache.toFile());
            cfg.setRepositoryUrls(new String[] {"http://127.0.0.1:" + port + "/repo"});

            final ArtifactManager am = ArtifactManager.getArtifactManager(cfg);

            // 1. Download artifact and verify cache hit on second call
            final ArtifactHandler h1 = am.getArtifactHandler(":group/artifact/1.0.0/found.jar");
            assertNotNull(h1);
            assertEquals(1, cfg.getDownloadedArtifacts());
            assertEquals(0, cfg.getCachedArtifacts());

            final ArtifactHandler h2 = am.getArtifactHandler(":group/artifact/1.0.0/found.jar");
            assertNotNull(h2);
            assertEquals(1, cfg.getDownloadedArtifacts());
            assertEquals(1, cfg.getCachedArtifacts());

            // 2. Authenticated download with user:pass in repo URL
            final ArtifactManagerConfig authCfg = new ArtifactManagerConfig();
            final Path authTempCache = Files.createTempDirectory("test-auth-cache");
            authCfg.setCacheDirectory(authTempCache.toFile());
            authCfg.setRepositoryUrls(new String[] {"http://user:pass@127.0.0.1:" + port + "/repo"});
            final ArtifactManager authAm = ArtifactManager.getArtifactManager(authCfg);

            final ArtifactHandler hAuth = authAm.getArtifactHandler(":group/artifact/1.0.0/auth.jar");
            assertNotNull(hAuth);

            // 3. HTTP 404 error message diagnostics
            try {
                am.getArtifactHandler(":group/artifact/1.0.0/missing.jar");
                fail("Expected IOException on 404");
            } catch (final IOException e) {
                assertTrue(e.getMessage().contains("HTTP 404"));
            }

            // 4. Test provider getArtifact() returns null on failure
            final ArtifactProvider provider = am.getArtifactProvider("*");
            assertNotNull(provider);
            org.junit.Assert.assertNull(
                    provider.getArtifact("http://127.0.0.1:" + port + "/repo/missing.jar", "missing.jar"));

            // 5. Test relative cache path with Windows drive prefix
            final URL windowsCacheUrl = provider.getArtifactFile(
                    "http://127.0.0.1:" + port + "/repo/group/artifact/1.0.0/found.jar",
                    "C:/group/artifact/1.0.0/found.jar");
            assertNotNull(windowsCacheUrl);

            // 6. Test local file that does not exist
            try {
                provider.getArtifactFile("file:/nonexistent/path/artifact.jar", "artifact.jar");
                fail("Expected IOException on nonexistent local file");
            } catch (final IOException e) {
                assertTrue(e.getMessage().contains("Local file does not exist"));
            }

            // 7. Test malformed URL without scheme
            try {
                provider.getArtifactFile("noscheme-url", "artifact.jar");
                fail("Expected IOException on malformed URL");
            } catch (final IOException e) {
                assertTrue(e.getMessage().contains("Malformed URL without scheme"));
            }

            am.shutdown();
            authAm.shutdown();
        } finally {
            server.stop(0);
        }

        // 8. Connection failed check (server stopped)
        int unusedPort;
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            unusedPort = socket.getLocalPort();
        }
        final ArtifactManagerConfig downCfg = new ArtifactManagerConfig();
        downCfg.setRepositoryUrls(new String[] {"http://127.0.0.1:" + unusedPort + "/repo"});
        final ArtifactManager downAm = ArtifactManager.getArtifactManager(downCfg);
        try {
            downAm.getArtifactHandler(":group/artifact/1.0.0/found.jar");
            fail("Expected IOException on connection refused");
        } catch (final IOException e) {
            assertTrue(e.getMessage().contains("Connection failed to http://127.0.0.1:" + unusedPort));
        } finally {
            downAm.shutdown();
        }
    }
}
