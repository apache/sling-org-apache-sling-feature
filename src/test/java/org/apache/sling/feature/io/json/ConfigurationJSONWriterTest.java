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

import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Configurations;
import org.junit.Test;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.fail;

public class ConfigurationJSONWriterTest {
    @Test
    public void testPreventDuplicatePIDs() {
        Configurations configs = new Configurations();
        Configuration c1 = new Configuration("my.pid");
        c1.getProperties().put("a", "123");
        Configuration c2 = new Configuration("MY.PID");
        c2.getProperties().put("b", "456");
        configs.addAll(Arrays.asList(c1, c2));

        try {
            ConfigurationJSONWriter.write(new CharArrayWriter(), configs);

            fail("The write operation should have thrown an exception since the "
                    + "set of configurations contains two identical PIDs (in "
                    + "different capitalizations)");
        } catch (IOException ioe) {
            // good!
        }
    }
}
