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
package org.apache.sling.feature;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.cm.json.Configurations;


/**
 * A configuration has either
 * <ul>
 * <li>a pid
 * <li>or a factory pid and a name
 * </ul>
 * and properties.
 *
 * This class is not thread-safe.
 */
public class Configuration
    implements Comparable<Configuration> {

    /**
     * Prefix for instructions for the configurator.
     */
    public static final String CONFIGURATOR_PREFIX = ":configurator:";

    /**
     * Prefix for special properties which are not configuration properties.
     */
    public static final String PROP_PREFIX = CONFIGURATOR_PREFIX + "feature:";

    /**
     * This optional configuration property stores the artifact id (mvn id) of the
     * bundle this configuration belongs to.
     */
    public static final String PROP_ARTIFACT_ID = PROP_PREFIX + "service.bundleLocation";

    /** The pid or name for factory pids. */
    private final String pid;

    /** The ordered properties. */
    private final Dictionary<String, Object> properties = Configurations.newConfiguration();

    /**
     * Create a new configuration
     * @param pid The pid
     * @throws IllegalArgumentException If pid is {@code null}
     */
    public Configuration(final String pid) {
        if ( pid == null ) {
            throw new IllegalArgumentException("pid must not be null");
        }
        this.pid = pid;
    }

    @Override
    public int compareTo(final Configuration o) {
        return this.pid.compareTo(o.pid);
    }

    /**
     * Get the pid.
     *
     * @return The pid
     */
    public String getPid() {
        return this.pid;
    }

    /**
     * Check whether the pid is a factory pid
     *
     * @return {@code true} if it's a factory pid
     * @since 1.3
     */
    public boolean isFactoryConfiguration() {
        return isFactoryConfiguration(this.pid);
    }

    /**
     * Return the factory pid of a pid if it's a factory configuration
     *
     * @return The factory pid or {@code null}.
     * @see #isFactoryConfiguration()
     * @since 1.3
     */
    public String getFactoryPid() {
        return getFactoryPid(this.pid);
    }

    /**
     * Return the name for a factory configuration if it is a factory configuration.
     *
     * @return The name or {@code null}.
     * @see #isFactoryConfiguration()
     * @since 1.3
     */
    public String getName() {
        return getName(this.pid);
    }

    /**
     * Check whether the pid is a factory pid
     *
     * @param pid The pid
     * @return {@code true} if it's a factory pid
     */
    public static boolean isFactoryConfiguration(final String pid) {
        return pid.contains("~");
    }

    /**
     * Return the factory pid of a pid if it's a factory configuration
     *
     * @param pid The pid
     * @return The factory pid or {@code null}.
     * @see #isFactoryConfiguration(String)
     */
    public static String getFactoryPid(final String pid) {
        final int pos = pid.indexOf('~');
        if (pos != -1) {
            return pid.substring(0, pos);
        }
        return null;
    }

    /**
     * Return the name for a factory configuration if it is a factory configuration.
     *
     * @param pid The pid
     * @return The name or {@code null}.
     * @see #isFactoryConfiguration(String)
     */
    public static String getName(final String pid) {
        final int pos = pid.indexOf('~');
        if (pos != -1) {
            return pid.substring(pos + 1);
        }
        return null;
    }

    /**
     * Get all properties of the configuration. This method returns a mutable
     * dictionary which can be mutated to alter the properties for this
     * configuration.
     *
     * @return The properties
     */
    public Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    /**
     * Get the configuration properties of the configuration. This configuration
     * properties are all properties minus properties used to manage the
     * configuration. Managing properties have to start with
     * {@code #CONFIGURATOR_PREFIX}. The returned copy is a mutable dictionary which
     * represents a snapshot of the properties at the time this method is called.
     *
     * @return The configuration properties
     */
    public Dictionary<String, Object> getConfigurationProperties() {
        final Dictionary<String, Object> p = new Hashtable<>();
        final Enumeration<String> keys = this.properties.keys();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            if (!key.startsWith(CONFIGURATOR_PREFIX)) {
                p.put(key, this.properties.get(key));
            }
        }
        return p;
    }

    /**
     * Create a copy of the configuration with a provided PID.
     *
     * @param aPid The pid of the configuration
     * @return A copy of this configuration with the given PID
     */
    public Configuration copy(final String aPid) {
        final Configuration result = new Configuration(aPid);
        final Enumeration<String> keyEnum = this.getProperties().keys();
        while (keyEnum.hasMoreElements()) {
            final String key = keyEnum.nextElement();
            result.getProperties().put(key, this.getProperties().get(key));
        }
        return result;
    }

    @Override
    public String toString() {
        return "Configuration [pid=" + pid
                + ", properties=" + properties
                + "]";
    }
}
