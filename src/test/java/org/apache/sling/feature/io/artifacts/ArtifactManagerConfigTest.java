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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.apache.sling.feature.io.artifacts.ArtifactManagerConfig.toFileUrl;
import static org.junit.Assert.assertArrayEquals;

public class ArtifactManagerConfigTest {

    private String userHomePreviousValue;
    private Path tempUserHome;

    @Before
    public void setUp() throws IOException {
        userHomePreviousValue = System.getProperty("user.home");

        // set user.home to temp directory
        tempUserHome = Files.createTempDirectory("ArtifactManagerConfigTest").toAbsolutePath();
        System.setProperty("user.home", tempUserHome.toString());
    }

    @After
    public void tearDown() {
        // restore previous user home value
        System.setProperty("user.home", userHomePreviousValue);
    }

    @Test
    public void testDefaultRepositoryUrlsWithoutMavenSettings() {
        ArtifactManagerConfig underTest = new ArtifactManagerConfig();
        assertArrayEquals(
                new String[] {
                    toFileUrl(tempUserHome + "/.m2/repository"),
                    "https://repo.maven.apache.org/maven2",
                    "https://repository.apache.org/content/groups/snapshots",
                },
                underTest.getRepositoryUrls());
    }

    @Test
    public void testDefaultRepositoryUrlsWithMavenSettings() throws IOException {
        FileUtils.write(
                new File(tempUserHome.toString() + "/.m2/settings.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd\">\n"
                        + "  <localRepository>" + tempUserHome + "/other-repo-path</localRepository>\n"
                        + "</settings>",
                StandardCharsets.UTF_8);

        ArtifactManagerConfig underTest = new ArtifactManagerConfig();
        assertArrayEquals(
                new String[] {
                    toFileUrl(tempUserHome + "/other-repo-path"),
                    "https://repo.maven.apache.org/maven2",
                    "https://repository.apache.org/content/groups/snapshots",
                },
                underTest.getRepositoryUrls());
    }
}
