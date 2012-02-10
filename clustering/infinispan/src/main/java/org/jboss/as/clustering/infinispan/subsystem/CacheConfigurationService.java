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

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.InfinispanMessages;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.TransactionSynchronizationRegistryProvider;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheConfigurationService implements Service<Configuration> {

    private final String name;
    private final ConfigurationBuilder builder;
    private final Dependencies dependencies;
    private volatile Configuration config;

    private static final Logger log = Logger.getLogger(CacheConfigurationService.class.getPackage().getName());

    public static ServiceName getServiceName(String container, String cache) {
        return CacheService.getServiceName(container, cache).append("config");
    }

    interface Dependencies {
        EmbeddedCacheManager getCacheContainer();
        TransactionManager getTransactionManager();
        TransactionSynchronizationRegistry getTransactionSynchronizationRegistry();
    }

    public CacheConfigurationService(String name, ConfigurationBuilder builder, Dependencies dependencies) {
        this.name = name;
        this.builder = builder;
        this.dependencies = dependencies;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public Configuration getValue() {
        return this.config;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {
        EmbeddedCacheManager container = this.dependencies.getCacheContainer();
        this.builder.jmxStatistics().enabled(container.getCacheManagerConfiguration().globalJmxStatistics().enabled());
        TransactionManager tm = this.dependencies.getTransactionManager();
        if (tm != null) {
            this.builder.transaction().transactionManagerLookup(new TransactionManagerProvider(tm));
        }
        TransactionSynchronizationRegistry tsr = this.dependencies.getTransactionSynchronizationRegistry();
        if (tsr != null) {
            this.builder.transaction().transactionSynchronizationRegistryLookup(new TransactionSynchronizationRegistryProvider(tsr));
        }
        this.config = this.builder.build();

        CacheMode mode = this.config.clustering().cacheMode();
        if (mode.isClustered() && (container.getTransport() == null)) {
            throw InfinispanMessages.MESSAGES.transportRequired(mode, this.name, container.getCacheManagerConfiguration().globalJmxStatistics().cacheManagerName());
        }

        container.defineConfiguration(this.name, this.config);

        log.debugf("%s cache configuration started", this.name);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.config = null;
        log.debugf("%s cache configuration stopped", this.name);
    }
}
