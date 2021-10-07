/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.microprofile.reactive.messaging.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveMessagingConfigSource implements ConfigSource {

    private static final String NAME = ReactiveMessagingConfigSource.class.getName();

    private static final Map<String, String> PROPERTIES;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("mp.messaging.connector.smallrye-kafka.tracing-enabled", "false");
        map.put("smallrye-messaging-strict-binding", "true");
        PROPERTIES = Collections.unmodifiableMap(map);
    }

    private final Map<String, String> properties;

    public ReactiveMessagingConfigSource() {
        properties = new ConcurrentHashMap<>();
        properties.putAll(PROPERTIES);
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public String getValue(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getOrdinal() {
        // Make the ordinal high so it cannot be overridden
        return Integer.MAX_VALUE;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    /**
     * Adds properties to the {@code ReactiveMessagingConfigSource} instance in the deployment's overall
     * {@code Config}
     *
     * @param config the deployment's {@code Config}
     * @param properties the properties we want to add
     */
    public static void addProperties(Config config, Map<String, String> properties) {
        for (ConfigSource source : config.getConfigSources()) {
            if (source.getName().equals(NAME) && source instanceof ReactiveMessagingConfigSource) {
                // We have found the instance of this class in the Config, and add our properties here
                ((ReactiveMessagingConfigSource)source).properties.putAll(properties);
            }
        }
    }
}
