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
package org.apache.sling.feature.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.Test;

public class IOUtilsTest {

    @Test public void testFileSort() {
        final String[] files = new String[] {
            "/different/path/app.json",
            "/path/to/base.json",
            "/path/to/feature.json",
            "/path/to/amode/feature.json",
            "/path/to/later/feature.json",
            "http://sling.apache.org/features/one.json",
            "http://sling.apache.org/features/two.json",
            "http://sling.apache.org/features/amode/feature.json"
        };

        final List<String> l = new ArrayList<>(Arrays.asList(files));
        Collections.sort(l, IOUtils.FEATURE_PATH_COMP);
        for(int i=0; i<files.length; i++) {
            assertEquals(files[i], l.get(i));
        }
    }

    @Test public void testGetFileFromURL() throws IOException {
        File file = File.createTempFile("IOUtilsTest", ".test");

        try {
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
                writer.println("Hello");
            }

            assertEquals(file, IOUtils.getFileFromURL(file.toURI().toURL(), false, null));

            URL url = new URL(null,"bla:" + file.toURI().toURL(), new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u){
                    return new URLConnection(u) {
                        @Override
                        public void connect()
                        {

                        }

                        @Override
                        public InputStream getInputStream() throws IOException
                        {
                            return new FileInputStream(file);
                        }
                    };
                }
            });

            assertNull(IOUtils.getFileFromURL(url, false, null));
            File tmp = IOUtils.getFileFromURL(url, true, null);

            assertNotEquals(file, tmp);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tmp), "UTF-8"))) {
                assertEquals("Hello", reader.readLine());
            }
        } finally {
            file.delete();
        }
        File jarFile = File.createTempFile("IOUtilsTes", ".jar");
        try {

            try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jarFile))) {
                output.putNextEntry(new JarEntry("test"));
                output.write("Hello".getBytes());
                output.closeEntry();
            }

            assertEquals(jarFile, IOUtils.getFileFromURL(new URL("jar:" + jarFile.toURI().toURL() + "!/"), false, null));
            assertNull(IOUtils.getFileFromURL(new URL("jar:file:" + jarFile.getPath() + "!/test"), false, null));
            File tmpJar = IOUtils.getFileFromURL(new URL("jar:" + jarFile.toURI().toURL() + "!/test"), true, null);
            assertNotNull(tmpJar);
            assertNotEquals(jarFile, tmpJar);
        } finally {
            jarFile.delete();
        }
    }

    @Test public void testGetJarFileFromURL() throws IOException {
        File jarFile = File.createTempFile("IOUtilsTest", ".jar");

        try {
            try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jarFile))) {
                output.putNextEntry(new JarEntry("test"));
                output.write("Hello".getBytes());
                output.closeEntry();
                output.putNextEntry(new JarEntry("test.jar"));
                try (JarOutputStream inner = new JarOutputStream(output)) {
                    inner.putNextEntry(new JarEntry("inner"));
                    inner.write("Hello".getBytes());
                    inner.closeEntry();
                }
            }

            JarFile jar = IOUtils.getJarFileFromURL(new URL("jar:" + jarFile.toURI().toURL() + "!/"), true, null);
            assertNotNull(jar);
            jar = IOUtils.getJarFileFromURL(jarFile.toURI().toURL(), true, null);
            assertNotNull(jar);

            assertNull(IOUtils.getFileFromURL(new URL("jar:" + jarFile.toURI().toURL() + "!/test"), false, null));

            JarFile tmpJar = IOUtils.getJarFileFromURL(new URL("jar:" + jarFile.toURI().toURL() + "!/test.jar"), true, null);
            assertNotNull(tmpJar);
            assertNotNull(tmpJar.getEntry("inner"));

            try {
                IOUtils.getJarFileFromURL(new URL("jar:" + jarFile.toURI().toURL() + "!/test"), true, null);
                fail();
            } catch (IOException ex) {
                // Expected
            }

            try {
                IOUtils.getJarFileFromURL(new URL("jar:" + jarFile.toURI().toURL() + "!/test.jar"), false, null);
                fail();
            } catch (IOException ex) {
                // Expected
            }
        } finally {
            jarFile.delete();
        }
    }
}
