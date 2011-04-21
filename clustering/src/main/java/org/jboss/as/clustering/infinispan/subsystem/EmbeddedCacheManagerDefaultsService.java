/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessController;
import java.util.EnumMap;
import java.util.Map;

import org.infinispan.config.Configuration;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.config.InfinispanConfiguration;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.util.loading.ContextClassLoaderSwitcher;
import org.jboss.util.loading.ContextClassLoaderSwitcher.SwitchContext;

/**
 * Service that provides infinispan cache configuration defaults per cache mode.
 * @author Paul Ferraro
 */
public class EmbeddedCacheManagerDefaultsService implements Service<EmbeddedCacheManagerDefaults> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(InfinispanExtension.SUBSYSTEM_NAME, "config", "defaults");

    private static final String DEFAULTS = "infinispan-defaults.xml";
    private static final Logger log = Logger.getLogger(CacheContainerAdd.class.getPackage().getName());

    @SuppressWarnings("unchecked")
    private static final ContextClassLoaderSwitcher switcher = (ContextClassLoaderSwitcher) AccessController.doPrivileged(ContextClassLoaderSwitcher.INSTANTIATOR);

    private volatile EmbeddedCacheManagerDefaults defaults;
    private final String resource;

    public EmbeddedCacheManagerDefaultsService() {
        this(DEFAULTS);
    }

    public EmbeddedCacheManagerDefaultsService(String resource) {
        this.resource = resource;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public EmbeddedCacheManagerDefaults getValue() throws IllegalStateException, IllegalArgumentException {
        return this.defaults;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {
        InfinispanConfiguration config = load(this.resource);
        Defaults defaults = new Defaults(config.parseGlobalConfiguration());
        Configuration defaultConfig = config.parseDefaultConfiguration();
        Map<String, Configuration> namedConfigs = config.parseNamedConfigurations();
        for (Configuration.CacheMode mode: Configuration.CacheMode.values()) {
            Configuration configuration = defaultConfig.clone();
            Configuration overrides = namedConfigs.get(mode.name());
            if (overrides != null) {
                configuration.applyOverrides(overrides);
            }
            configuration.setCacheMode(mode);
            defaults.add(mode, configuration);
        }
        this.defaults = defaults;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.defaults = null;
    }

    private static InfinispanConfiguration load(String resource) throws StartException {
        URL url = find(resource, Thread.currentThread().getContextClassLoader(), InfinispanExtension.class.getClassLoader());
        try {
            InputStream input = url.openStream();
            SwitchContext context = switcher.getSwitchContext(InfinispanConfiguration.class.getClassLoader());
            try {
                return InfinispanConfiguration.newInfinispanConfiguration(input);
            } finally {
                context.reset();
                try {
                    input.close();
                } catch (IOException e) {
                    log.warn(e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new StartException(String.format("Failed to parse %s", url), e);
        }
    }

    private static URL find(String resource, ClassLoader... loaders) throws StartException {
        for (ClassLoader loader: loaders) {
            URL url = loader.getResource(resource);
            if (url != null) {
                return url;
            }
        }
        throw new StartException(String.format("Failed to locate %s", resource));
    }

    class Defaults implements EmbeddedCacheManagerDefaults {
        private final GlobalConfiguration global;
        private final Map<Configuration.CacheMode, Configuration> configs = new EnumMap<Configuration.CacheMode, Configuration>(Configuration.CacheMode.class);

        Defaults(GlobalConfiguration global) {
            this.global = global;
        }

        void add(Configuration.CacheMode mode, Configuration config) {
            this.configs.put(mode, config);
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerDefaults#getGlobalConfiguration()
         */
        @Override
        public GlobalConfiguration getGlobalConfiguration() {
            return this.global;
        }

        /**
         * {@inheritDoc}
         * @see org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerDefaults#getDefaultConfiguration(org.infinispan.config.Configuration.CacheMode)
         */
        @Override
        public Configuration getDefaultConfiguration(CacheMode mode) {
            return this.configs.get(mode);
        }
    }
}
