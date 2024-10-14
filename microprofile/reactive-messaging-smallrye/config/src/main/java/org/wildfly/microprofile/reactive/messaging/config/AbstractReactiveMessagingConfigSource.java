/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.wildfly.microprofile.reactive.messaging.config._private.MicroProfileReactiveMessagingConfigLogger;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class AbstractReactiveMessagingConfigSource implements ConfigSource {


    private final String name;
    private final Map<String, String> properties;
    private final int ordinal;

    public AbstractReactiveMessagingConfigSource(int ordinal, Map<String, String> properties) {
        this.name = this.getClass().getName();
        this.ordinal = ordinal;
        this.properties = new ConcurrentHashMap<>();
        this.properties.putAll(properties);

        MicroProfileReactiveMessagingConfigLogger logger = MicroProfileReactiveMessagingConfigLogger.LOGGER;
        if (logger.isDebugEnabled()) {
            logger.debugf("Initialising ConfigSource '%s', ordinal: %d, properties: %s", this.name, this.ordinal, this.properties);
        }
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
        return name;
    }

    @Override
    public int getOrdinal() {
        return ordinal;
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    /**
     * Adds properties to the {@code AbstractReactiveMessagingConfigSource} instance in the deployment's overall
     * {@code Config}
     *
     * @param config the deployment's {@code Config}
     * @param properties the properties we want to add
     * @param cfgClass sub-class we want to add properties to
     */
    protected static void addDeploymentProperties(Config config, Map<String, String> properties, Class<? extends AbstractReactiveMessagingConfigSource> cfgClass) {
        // Override this method from the sub-classes, and pass in cfgClass if we need it again
        for (ConfigSource source : config.getConfigSources()) {
            if (source.getClass().isAssignableFrom(cfgClass)) {
                // We have found the instance of this class in the Config, and add our properties here
                ((AbstractReactiveMessagingConfigSource)source).properties.putAll(properties);
            }
        }
    }
}
