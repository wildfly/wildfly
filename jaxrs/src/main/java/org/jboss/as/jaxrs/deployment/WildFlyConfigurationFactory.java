/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs.deployment;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.jaxrs.logging.JaxrsLogger;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.config.Configuration;
import org.jboss.resteasy.spi.config.ConfigurationFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class WildFlyConfigurationFactory implements ConfigurationFactory {
    private static final ConfigurationFactory DEFAULT = () -> Integer.MAX_VALUE;
    private final Map<ClassLoader, ConfigurationFactory> delegates;

    public WildFlyConfigurationFactory() {
        delegates = new ConcurrentHashMap<>();
    }

    static WildFlyConfigurationFactory getInstance() {
        final ConfigurationFactory result = ConfigurationFactory.getInstance();
        if (!(result instanceof WildFlyConfigurationFactory)) {
            throw JaxrsLogger.JAXRS_LOGGER.invalidConfigurationFactory(result == null ? null : result.getClass());
        }
        return (WildFlyConfigurationFactory) result;
    }

    void register(final ClassLoader classLoader, final boolean useMpConfig) {
        delegates.put(classLoader, createDelegate(classLoader, useMpConfig));
    }

    void unregister(final ClassLoader classLoader) {
        delegates.remove(classLoader);
    }


    @Override
    public Configuration getConfiguration() {
        return getDelegate().getConfiguration();
    }

    @Override
    public Configuration getConfiguration(final ResteasyConfiguration config) {
        return getDelegate().getConfiguration(config);
    }

    @Override
    public int priority() {
        return 0;
    }

    private ConfigurationFactory getDelegate() {
        return delegates.getOrDefault(WildFlySecurityManager.getCurrentContextClassLoaderPrivileged(), DEFAULT);
    }

    private static ConfigurationFactory createDelegate(final ClassLoader classLoader, final boolean useMpConfig) {
        if (useMpConfig) {
            // Safely load this and have a default. This way we won't fail using RESTEasy.
            try {
                final Constructor<? extends ConfigurationFactory> constructor =
                        getMpConfigFactory(classLoader).getConstructor();
                return constructor.newInstance();
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException | ClassNotFoundException e) {
                JaxrsLogger.JAXRS_LOGGER.failedToLoadConfigurationFactory(e.getMessage());
            }
        }
        return DEFAULT;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends ConfigurationFactory> getMpConfigFactory(final ClassLoader classLoader) throws ClassNotFoundException {
        return (Class<? extends ConfigurationFactory>) Class.forName("org.jboss.resteasy.microprofile.config.ConfigConfigurationFactory", false, classLoader);
    }
}
