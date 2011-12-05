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

import java.util.EnumMap;
import java.util.Map;

import org.infinispan.config.Configuration;
import org.infinispan.config.Configuration.CacheMode;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.config.GlobalConfiguration.ShutdownHookBehavior;
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that provides infinispan cache configuration defaults per cache mode.
 * @author Paul Ferraro
 */
public class EmbeddedCacheManagerDefaultsService implements Service<EmbeddedCacheManagerDefaults> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(InfinispanExtension.SUBSYSTEM_NAME, "config", "defaults");
/*
    private static final String DEFAULTS = "infinispan-defaults.xml";
    private static final Logger log = Logger.getLogger(CacheContainerAdd.class.getPackage().getName());
*/
    private volatile EmbeddedCacheManagerDefaults defaults;
/*
    private final String resource;

    public EmbeddedCacheManagerDefaultsService() {
        this(DEFAULTS);
    }

    public EmbeddedCacheManagerDefaultsService(String resource) {
        this.resource = resource;
    }
*/
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
        // Don't pull defaults from external file until Infinispan's configuration
        // file parsing performs better (ISPN-1065).
/*
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
            configuration.fluent().mode(mode);
            defaults.add(mode, configuration);
        }
*/
        GlobalConfiguration global = new GlobalConfiguration(this.getClass().getClassLoader());
        global.fluent()
            .transport().distributedSyncTimeout(60000L)
            .shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER)
            ;
        Defaults defaults = new Defaults(global);
        Configuration defaultConfig = new Configuration();
        defaultConfig.fluent()
            .locking().lockAcquisitionTimeout(15000L).useLockStriping(false).concurrencyLevel(1000)
            .eviction().strategy(EvictionStrategy.NONE).maxEntries(10000)
            .transaction().transactionMode(org.infinispan.transaction.TransactionMode.TRANSACTIONAL).useSynchronization(true)
            ;
        for (Configuration.CacheMode mode: Configuration.CacheMode.values()) {
            Configuration configuration = defaultConfig.clone();
            FluentConfiguration.ClusteringConfig fluent = configuration.fluent().mode(mode);
            if (mode.isClustered()) {
                FluentConfiguration.StoreAsBinaryConfig binary = fluent.storeAsBinary();
                if (mode.isInvalidation()) {
                    // Don't serialize cache entry values
                    binary.storeValuesAsBinary(false);
                }
            }
            if (mode.isReplicated()) {
                fluent.stateRetrieval().fetchInMemoryState(true).timeout(60000L);
            }
            if (mode.isSynchronous()) {
                fluent.sync().replTimeout(17500L);
            } else {
                // ISPN-835 workaround
                if (configuration.isFetchInMemoryState()) {
                    fluent.async().useReplQueue(true).replQueueMaxElements(1);
                }
            }
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
/*
    private static InfinispanConfiguration load(String resource) throws StartException {
        URL url = find(resource, InfinispanExtension.class.getClassLoader());
        log.debugf("Loading Infinispan defaults from %s", url.toString());
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
            if (loader != null) {
                URL url = loader.getResource(resource);
                if (url != null) {
                    return url;
                }
            }
        }
        throw new StartException(String.format("Failed to locate %s", resource));
    }
*/
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
