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
package org.apache.sling.feature.builder;

import static org.junit.Assert.assertEquals;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Include;
import org.junit.Test;

public class ApplicationBuilderTest {


    @Test public void testIncludedFeatureProvided() throws Exception {
        final ArtifactId idA = ArtifactId.fromMvnId("g:a:1.0.0");
        final ArtifactId idB = ArtifactId.fromMvnId("g:b:1.0.0");

        final Feature a = new Feature(idA);
        final Feature b = new Feature(idB);
        // feature b includes feature a
        final Include inc = new Include(idA);
        b.getIncludes().add(inc);

        // assemble application, it should only contain feature b as a is included by b
        final Application app = ApplicationBuilder.assemble(null, new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(ArtifactId id) {
                return null;
            }
        }), a, b);
        assertEquals(1, app.getFeatureIds().size());
        assertEquals(idB, app.getFeatureIds().get(0));
    }
}
