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

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.tm.BatchModeTransactionManager;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.TransactionSynchronizationRegistryProvider;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheConfigurationService implements Service<Configuration> {

    private static final Logger log = Logger.getLogger(CacheConfigurationService.class.getPackage().getName()) ;
    private static final String SERVICE_NAME_ELEMENT = "config" ;

    private final String name;
    private final String template;
    private final Configuration overrides ;
    private final CacheConfigurationHelper configurationHelper;
    private Configuration configuration ;

    public static ServiceName getServiceName(String container, String cache) {
        return EmbeddedCacheManagerService.getServiceName(container)
               .append(CacheConfigurationService.SERVICE_NAME_ELEMENT)
               .append((cache != null) ? cache : CacheContainer.DEFAULT_CACHE_NAME);
    }

    public CacheConfigurationService(String name, Configuration overrides, CacheConfigurationHelper configurationHelper) {
        this(name, null, overrides, configurationHelper);
    }

    public CacheConfigurationService(String name, String template, Configuration overrides, CacheConfigurationHelper configurationHelper) {
        this.name = name;
        this.template = template;
        this.overrides = overrides ;
        this.configurationHelper = configurationHelper;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public Configuration getValue() throws IllegalStateException, IllegalArgumentException {
        return this.configuration;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {

        CacheContainer container = this.configurationHelper.getCacheContainer();
        EmbeddedCacheManagerDefaults defaults = this.configurationHelper.getEmbeddedCacheManagerDefaults();

        // set up the cache configuration
        Configuration.CacheMode mode = this.overrides.getCacheMode() ;
        Configuration configurationDefaults = defaults.getDefaultConfiguration(mode);
        configuration = configurationDefaults.clone() ;
        configuration.applyOverrides(overrides);

        // check for missing dependencies
        if (configuration.isTransactionalCache() && !configuration.isInvocationBatchingEnabled() && configurationHelper.getTransactionManager() == null) {
            throw new StartException("Missing dependency: transaction manager required") ;
        }
        if (configuration.isUseSynchronizationForTransactions() && configurationHelper.getTransactionSynchronizationRegistry() == null) {
            throw new StartException("Missing dependency: transaction synchronization registry provider required") ;
        }

        // for transactional caches, our first opportunity to set the providers
        FluentConfiguration.TransactionConfig tx = configuration.fluent().transaction();
        if (configuration.isTransactionalCache()) {
            if (configuration.isInvocationBatchingEnabled()) {
                tx.transactionManagerLookup(new TransactionManagerProvider(new ImmediateValue<TransactionManager>(BatchModeTransactionManager.getInstance())));
            } else {
                Value<TransactionManager> tm = this.configurationHelper.getTransactionManager();
                if (tm != null) {
                    tx.transactionManagerLookup(new TransactionManagerProvider(tm));
                }
                if (configuration.isUseSynchronizationForTransactions()) {
                    Value<TransactionSynchronizationRegistry> txSyncRegistry = this.configurationHelper.getTransactionSynchronizationRegistry();
                    if (txSyncRegistry != null) {
                        tx.transactionSynchronizationRegistryLookup(new TransactionSynchronizationRegistryProvider(txSyncRegistry));
                    }
                }
            }
        }

        // if template != null, a cache named template is used as the base; otherwise default
        if (this.template != null) {
            ((EmbeddedCacheManager) container).defineConfiguration(this.name, this.template, configuration);
        } else {
            ((EmbeddedCacheManager) container).defineConfiguration(this.name, configuration);
        }

        // advertise
        if (log.isDebugEnabled()) {
            Configuration config = ((EmbeddedCacheManager) container).defineConfiguration(this.name, new Configuration());
            log.debugf("Cache configuration defined for cache %s with contents: %s", this.name, dumpCacheConfiguration(this.name, config));
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        // TODO should we try to nullify the configuration in the cache manager?
    }

    private String dumpCacheConfiguration(String name, Configuration c) {
        StringBuilder sb = new StringBuilder() ;
        if (name != null && c != null) {
            sb.append("cache name: " + name) ;
            sb.append(", eviction strategy: " + c.getEvictionStrategy()) ;
            sb.append(", eviction max entries: " + c.getEvictionMaxEntries()) ;
            sb.append(", expiration max idle: " + c.getExpirationMaxIdle());
            sb.append(", expiration lifespan: " + c.getExpirationLifespan());
            sb.append(", expiration interval: " + c.getExpirationWakeUpInterval());
            return sb.toString() ;
        }
        return null ;
    }

    static class CacheConfigurationHelperImpl implements CacheConfigurationHelper {
        private final InjectedValue<CacheContainer> container = new InjectedValue<CacheContainer>();
        private final InjectedValue<EmbeddedCacheManagerDefaults> defaults = new InjectedValue<EmbeddedCacheManagerDefaults>();
        private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();
        private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistry = new InjectedValue<TransactionSynchronizationRegistry>();
        private final String name;

        CacheConfigurationHelperImpl(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }

        Injector<CacheContainer> getCacheContainerInjector() {
            return this.container;
        }

        Injector<EmbeddedCacheManagerDefaults> getDefaultsInjector() {
            return this.defaults;
        }
        Injector<TransactionManager> getTransactionManagerInjector() {
            return this.transactionManager;
        }

        Injector<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistryInjector() {
            return this.transactionSynchronizationRegistry;
        }

        @Override
        public CacheContainer getCacheContainer() {
            return this.container.getValue();
        }

        @Override
        public EmbeddedCacheManagerDefaults getEmbeddedCacheManagerDefaults() {
            return this.defaults.getValue();
        }

        @Override
        public Value<TransactionManager> getTransactionManager() {
            return this.transactionManager;
        }

        @Override
        public Value<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistry() {
            return this.transactionSynchronizationRegistry;
        }
    }
}
