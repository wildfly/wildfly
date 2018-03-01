/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.MemoryConfiguration;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class LocalCacheBuilder extends CacheConfigurationBuilder {

    private final ValueDependency<MemoryConfiguration> memory;
    private final ValueDependency<PersistenceConfiguration> persistence;
    private final ValueDependency<TransactionConfiguration> transaction;

    LocalCacheBuilder(PathAddress address) {
        super(address);
        this.memory = new InjectedValueDependency<>(CacheComponent.MEMORY.getServiceName(address), MemoryConfiguration.class);
        this.persistence = new InjectedValueDependency<>(CacheComponent.PERSISTENCE.getServiceName(address), PersistenceConfiguration.class);
        this.transaction = new InjectedValueDependency<>(CacheComponent.TRANSACTION.getServiceName(address), TransactionConfiguration.class);
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        ServiceBuilder<Configuration> builder = super.build(target);
        return new CompositeDependency(this.memory, this.persistence, this.transaction).register(builder);
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        super.accept(builder);

        builder.clustering().cacheMode(CacheMode.LOCAL);

        MemoryConfiguration memory = this.memory.getValue();
        PersistenceConfiguration persistence = this.persistence.getValue();
        TransactionConfiguration transaction = this.transaction.getValue();

        // Auto-enable simple cache optimization if cache is non-transactional and non-persistent
        builder.simpleCache((memory.storageType() == StorageType.OBJECT) && !transaction.transactionMode().isTransactional() && !persistence.usingStores());
    }
}
