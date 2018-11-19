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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


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

    /**
     * This optional configuration property stores the artifact id (mvn id) of the
     * feature this configuration actually originated from.
     */
    public static final String PROP_ORIGINAL__FEATURE = PROP_PREFIX + "org-feature";

    /** The pid or name for factory pids. */
    private final String pid;

    /** The properties. */
    private final Dictionary<String, Object> properties = new OrderedDictionary();

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
     */
    public static String getFactoryPid(final String pid) {
        final int pos = pid.indexOf('~');
        if (pos != -1) {
            return pid.substring(0, pos);
        }
        return null;
    }

    /**
     * Return the name for a factory configuration.
     *
     * @param pid The pid
     * @return The name or {@code null}.
     */
    public static String getName(final String pid) {
        final int pos = pid.indexOf('~');
        if (pos != -1) {
            return pid.substring(pos + 1);
        }
        return null;
    }

    /**
     * Get all properties of the configuration.
     *
     * @return The properties
     */
    public Dictionary<String, Object> getProperties() {
        return this.properties;
    }

    /**
     * Get the configuration properties of the configuration.
     *
     * @return The properties
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

    @Override
    public String toString() {
        return "Configuration [pid=" + pid
                + ", properties=" + properties
                + "]";
    }

    public static class OrderedDictionary extends Dictionary<String, Object> implements Map<String, Object> {

        private static class EnumarationImpl<E> implements Enumeration<E> {
            private final Iterator<E> iterator;

            public EnumarationImpl(Iterator<E> iterator) {
                this.iterator = iterator;
            }

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public E nextElement() {
                return iterator.next();
            }
        }

        private final Map<String, Object> map = new LinkedHashMap<String, Object>();

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public Enumeration<String> keys() {
            return new EnumarationImpl<>(map.keySet().iterator());
        }

        @Override
        public Enumeration<Object> elements() {
            return new EnumarationImpl<>(map.values().iterator());
        }

        @Override
        public Object get(Object key) {
            return map.get(key);
        }

        @Override
        public Object put(String key, Object value) {
            // Make sure the value is not null
            if (value == null) {
                throw new NullPointerException();
            }

            return map.put(key, value);
        }

        @Override
        public Object remove(Object key) {
            return map.remove(key);
        }

        @Override
		public boolean containsKey(Object key) {
			return this.map.containsKey(key);
		}

		@Override
		public boolean containsValue(Object value) {
			return this.map.containsValue(value);
		}

		@Override
		public void putAll(Map<? extends String, ? extends Object> m) {
			this.map.putAll(m);
		}

		@Override
		public void clear() {
			this.map.clear();
		}

		@Override
		public Set<String> keySet() {
			return this.map.keySet();
		}

		@Override
		public Collection<Object> values() {
			return this.map.values();
		}

		@Override
		public Set<Entry<String, Object>> entrySet() {
			return this.map.entrySet();
		}

		@Override
        public boolean equals(Object o) {
        	if ( !(o instanceof OrderedDictionary) ) {
        		if ( o instanceof Dictionary ) {
        			@SuppressWarnings("rawtypes")
					final Dictionary other = (Dictionary)o;
        			if (other.size() == this.size() ) {
        				final Enumeration<String> iter = this.keys();
        				while ( iter.hasMoreElements() ) {
        					final String key = iter.nextElement();
        					final Object ov = other.get(key);
        					if ( ov == null ) {
        						return false;
        					}
        					final Object tv = this.get(key);
        					if ( !tv.equals(ov) ) {
        						return false;
        					}
        				}
        				return true;
        			}
        		}
        		if ( o instanceof Map ) {
        			return map.equals(o);
        		}
        		return false;
        	}
            return map.equals(((OrderedDictionary)o).map);
        }

        @Override
        public int hashCode() {
            return map.hashCode();
        }

		@Override
		public String toString() {
			return map.toString();
		}
    }
}
