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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.sling.feature.io.artifacts.spi.ArtifactProviderContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class holds the configuration of artifact manager.
 * This class is not thread-safe.
 */
public class ArtifactManagerConfig implements ArtifactProviderContext {

    /** The repository urls. */
    private String[] repositoryUrls;

    /** The cache directory. */
    private File cacheDirectory;

    /** Metrics for artifacts used from the cache. */
    private final AtomicLong cachedArtifacts = new AtomicLong();

    /** Metrics for artifacts needed to be downloaded. */
    private final AtomicLong downloadedArtifacts = new AtomicLong();

    /** Metrics for artifacts read locally. */
    private final AtomicLong localArtifacts = new AtomicLong();

    /** Whether locally mvn command can be used to download artifacts. */
    private boolean useMvn = false;

    /**
     * The .m2 directory.
     */
    private final @NotNull String repoHome;

    private static final Logger logger = LoggerFactory.getLogger(ArtifactManagerConfig.class);

    /**
     * Create a new configuration object. Set the default values
     */
    public ArtifactManagerConfig() {
        // set defaults
        String mvnRepositoryDirectory = getMvnRepositoryDirectory();
        this.repositoryUrls = new String[] {
            toFileUrl(mvnRepositoryDirectory),
            "https://repo.maven.apache.org/maven2",
            "https://repository.apache.org/content/groups/snapshots"
        };
        this.repoHome = mvnRepositoryDirectory + "/";
    }

    /**
     * Set the repository urls
     * @param urls The repository urls
     */
    public void setRepositoryUrls(final String[] urls) {
        if (urls == null || urls.length == 0) {
            this.repositoryUrls = new String[0];
        } else {
            this.repositoryUrls = new String[urls.length];
            System.arraycopy(urls, 0, this.repositoryUrls, 0, urls.length);
            for (int i = 0; i < this.repositoryUrls.length; i++) {
                if (this.repositoryUrls[i].endsWith("/")) {
                    this.repositoryUrls[i] = this.repositoryUrls[i].substring(0, this.repositoryUrls[i].length() - 1);
                }
            }
        }
    }

    /**
     * Get the repository urls.
     * A repository url does not end with a slash.
     * @return The repository urls.
     */
    public @NotNull String[] getRepositoryUrls() {
        return repositoryUrls;
    }

    /**
     * Get the cache directory
     * @return The cache directory or {@code null} if none has been set
     */
    @Override
    public @Nullable File getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * Set the cache directory
     * @param dir The cache directory
     */
    public void setCacheDirectory(final File dir) {
        this.cacheDirectory = dir;
    }

    @Override
    public void incCachedArtifacts() {
        this.cachedArtifacts.incrementAndGet();
    }

    @Override
    public void incDownloadedArtifacts() {
        this.downloadedArtifacts.incrementAndGet();
    }

    @Override
    public void incLocalArtifacts() {
        this.localArtifacts.incrementAndGet();
    }

    /**
     * Get the number of cached artifacts
     * @return The number of cached artifacts
     */
    public long getCachedArtifacts() {
        return this.cachedArtifacts.get();
    }

    /**
     * Get the number of downloaded artifacts
     * @return The number of downloaded artifacts
     */
    public long getDownloadedArtifacts() {
        return this.downloadedArtifacts.get();
    }

    /**
     * Get the number of local artifacts
     * @return The number of local artifacts
     */
    public long getLocalArtifacts() {
        return this.localArtifacts.get();
    }

    /**
     * Should mvn be used if an artifact can't be found in the repositories
     *
     * @return Whether mvn command should be used.
     * @since 1.1.0
     */
    public boolean isUseMvn() {
        return useMvn;
    }

    /**
     * Set whether mvn should be used to get artifacts.
     *
     * @param useMvn flag for enabling mvn
     * @since 1.1.0
     */
    public void setUseMvn(final boolean useMvn) {
        this.useMvn = useMvn;
    }

    /**
     * Return mvn home
     *
     * @since 1.1.0
     */
    @NotNull
    String getMvnHome() {
        return this.repoHome;
    }

    /**
     * Get the .m2 directory for the current user.
     * If a ~/.m2/settings.xml exists, it is checked for a localRepository configuration.
     * Otherwise ~/.m2/repository is used as default value.
     * @return Maven repository directory
     */
    private static final String getMvnRepositoryDirectory() {
        String mavenDirectory = System.getProperty("user.home") + "/.m2";
        File settings = new File(mavenDirectory, "settings.xml");
        if (settings.exists()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(settings);
                NodeList nodes = doc.getElementsByTagName("localRepository");
                if (nodes.getLength() > 0) {
                    String localRepository = nodes.item(0).getTextContent();
                    return new File(localRepository).getAbsolutePath();
                }
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                logger.warn("Unable to parse " + settings.getAbsolutePath(), ex);
            }
        }
        return mavenDirectory + "/repository";
    }

    static final String toFileUrl(String path) {
        return "file://" + new File(path).toURI().getPath();
    }
}
