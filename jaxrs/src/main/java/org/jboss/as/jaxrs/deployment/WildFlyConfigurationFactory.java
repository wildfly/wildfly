/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
