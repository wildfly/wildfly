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

import java.util.ServiceLoader;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.GroupsConfigurationBuilder;
import org.infinispan.distribution.group.Grouper;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.TransactionSynchronizationRegistryProvider;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CacheConfigurationService extends AbstractCacheConfigurationService {

    public static ServiceName getServiceName(String container, String cache) {
        return CacheService.getServiceName(container, cache).append("config");
    }

    interface Dependencies {
        CacheMode getCacheMode();
        ConfigurationBuilder getConfigurationBuilder();
        ModuleIdentifier getModuleIdentifier();
        ModuleLoader getModuleLoader();
        EmbeddedCacheManager getCacheContainer();
        TransactionManager getTransactionManager();
        TransactionSynchronizationRegistry getTransactionSynchronizationRegistry();
        ConsistentHashStrategy getConsistentHashStrategy();
    }

    private final Dependencies dependencies;

    public CacheConfigurationService(String name, Dependencies dependencies) {
        super(name);
        this.dependencies = dependencies;
    }

    @Override
    protected EmbeddedCacheManager getCacheContainer() {
        return this.dependencies.getCacheContainer();
    }

    @Override
    protected ConfigurationBuilder getConfigurationBuilder() {
        ConfigurationBuilder builder = this.dependencies.getConfigurationBuilder();
        ModuleIdentifier moduleId = this.dependencies.getModuleIdentifier();
        if (moduleId != null) {
            try {
                Module module = this.dependencies.getModuleLoader().loadModule(moduleId);
                // Override classloader with that of the specified module
                ClassLoader loader = module.getClassLoader();
                builder.classLoader(loader);

                GroupsConfigurationBuilder groupsBuilder = builder.clustering().hash().groups();
                for (Grouper<?> grouper: ServiceLoader.load(Grouper.class, loader)) {
                    groupsBuilder.addGrouper(grouper);
                }
            } catch (ModuleLoadException e) {
                throw new IllegalArgumentException(e);
            }
        }
        TransactionManager tm = this.dependencies.getTransactionManager();
        if (tm != null) {
            builder.transaction().transactionManagerLookup(new TransactionManagerProvider(tm));
        }
        TransactionSynchronizationRegistry tsr = this.dependencies.getTransactionSynchronizationRegistry();
        if (tsr != null) {
            builder.transaction().transactionSynchronizationRegistryLookup(new TransactionSynchronizationRegistryProvider(tsr));
        }
        boolean topologyAware = this.dependencies.getCacheContainer().getCacheManagerConfiguration().transport().hasTopologyInfo();
        this.dependencies.getConsistentHashStrategy().buildHashConfiguration(builder.clustering().hash(), this.dependencies.getCacheMode(), topologyAware);
        return builder;
    }
}
