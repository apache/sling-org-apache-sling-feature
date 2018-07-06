/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.io.json;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.Configuration;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ApplicationJSONReaderTest {
    @Test
    public void testOverrideVariables() throws Exception {
        File appFile = new File(getClass().getResource("/applications/app_with_vars.json").toURI());
        FileReader fr = new FileReader(appFile);

        Map<String, String> overridden = new HashMap<>();
        overridden.put("web.port", "12345");
        overridden.put("something", "else");

        Application app = ApplicationJSONReader.read(fr, overridden);
        Configuration cfg = app.getConfigurations().getConfiguration("org.apache.felix.http");
        assertEquals("12345", cfg.getProperties().get("org.osgi.service.http.port"));
    }

    @Test
    public void testDefaultVariables() throws Exception {
        File appFile = new File(getClass().getResource("/applications/app_with_vars.json").toURI());
        FileReader fr = new FileReader(appFile);

        Application app = ApplicationJSONReader.read(fr);
        Configuration cfg = app.getConfigurations().getConfiguration("org.apache.felix.http");
        assertEquals("8888", cfg.getProperties().get("org.osgi.service.http.port"));
    }
}
