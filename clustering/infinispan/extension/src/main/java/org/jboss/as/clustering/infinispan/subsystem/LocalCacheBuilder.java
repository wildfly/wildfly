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
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.TransactionConfiguration;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class LocalCacheBuilder extends CacheConfigurationBuilder {

    private final InjectedValue<TransactionConfiguration> transaction = new InjectedValue<>();
    private final InjectedValue<LockingConfiguration> locking = new InjectedValue<>();

    private final String containerName;
    private final String cacheName;

    LocalCacheBuilder(String containerName, String cacheName) {
        super(containerName, cacheName);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        return super.build(target)
                .addDependency(CacheComponent.TRANSACTION.getServiceName(this.containerName, this.cacheName), TransactionConfiguration.class, this.transaction)
                .addDependency(CacheComponent.LOCKING.getServiceName(this.containerName, this.cacheName), LockingConfiguration.class, this.locking)
        ;
    }

    @Override
    public ConfigurationBuilder createConfigurationBuilder() {
        ConfigurationBuilder builder = super.createConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.LOCAL);

        if ((this.transaction.getValue().lockingMode() == LockingMode.OPTIMISTIC) && (this.locking.getValue().isolationLevel() == IsolationLevel.REPEATABLE_READ)) {
            builder.locking().writeSkewCheck(true);
            builder.versioning().enable().scheme(VersioningScheme.SIMPLE);
        }

        return builder;
    }

}
