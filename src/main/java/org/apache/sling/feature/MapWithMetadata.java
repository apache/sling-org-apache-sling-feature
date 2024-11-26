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
package org.apache.sling.feature;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to maintain metadata for a map.
 * This class is not thread-safe.
 * @since 1.7.0
 */
class MapWithMetadata implements Map<String, String>, Serializable {

    private static final long serialVersionUID = 2L;

    private final Map<String, String> values = new LinkedHashMap<>();

    private final Map<String, Map<String, Object>> metadata = new HashMap<>();

    public Map<String, Object> getMetadata(final String key) {
        if (values.containsKey(key)) {
            return metadata.computeIfAbsent(key, id -> new LinkedHashMap<>());
        }
        metadata.remove(key);
        return null;
    }

    @Override
    public void clear() {
        this.values.clear();
        this.metadata.clear();
    }

    @Override
    public boolean containsKey(final Object key) {
        return this.values.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return this.values.containsValue(value);
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return this.values.entrySet();
    }

    @Override
    public String get(final Object key) {
        return this.values.get(key);
    }

    @Override
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return this.values.keySet();
    }

    @Override
    public String put(final String key, final String value) {
        return this.values.put(key, value);
    }

    @Override
    public void putAll(final Map<? extends String, ? extends String> m) {
        this.values.putAll(m);
    }

    @Override
    public String remove(final Object key) {
        this.metadata.remove(key);
        return this.values.remove(key);
    }

    @Override
    public int size() {
        return this.values.size();
    }

    @Override
    public Collection<String> values() {
        return this.values.values();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return values.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MapWithMetadata) {
            return this.values.equals((MapWithMetadata) obj);
        }
        if (!(obj instanceof Map)) {
            return false;
        }
        return this.values.equals(obj);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return values.toString();
    }
}
