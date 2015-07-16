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

import org.infinispan.configuration.cache.BackupForConfiguration;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LockingConfiguration;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.SitesConfigurationBuilder;
import org.infinispan.configuration.cache.StateTransferConfiguration;
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
public class SharedStateCacheBuilder extends ClusteredCacheBuilder {

    private final InjectedValue<StateTransferConfiguration> stateTransfer = new InjectedValue<>();
    private final InjectedValue<BackupForConfiguration> backupFor = new InjectedValue<>();
    private final InjectedValue<SitesConfiguration> backups = new InjectedValue<>();
    private final InjectedValue<TransactionConfiguration> transaction = new InjectedValue<>();
    private final InjectedValue<LockingConfiguration> locking = new InjectedValue<>();

    private final String containerName;
    private final String cacheName;

    SharedStateCacheBuilder(String containerName, String cacheName, CacheMode mode) {
        super(containerName, cacheName, mode);
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceBuilder<Configuration> build(ServiceTarget target) {
        return super.build(target)
                .addDependency(CacheComponent.STATE_TRANSFER.getServiceName(this.containerName, this.cacheName), StateTransferConfiguration.class, this.stateTransfer)
                .addDependency(CacheComponent.BACKUPS.getServiceName(this.containerName, this.cacheName), SitesConfiguration.class, this.backups)
                .addDependency(CacheComponent.BACKUP_FOR.getServiceName(this.containerName, this.cacheName), BackupForConfiguration.class, this.backupFor)
                .addDependency(CacheComponent.TRANSACTION.getServiceName(this.containerName, this.cacheName), TransactionConfiguration.class, this.transaction)
                .addDependency(CacheComponent.LOCKING.getServiceName(this.containerName, this.cacheName), LockingConfiguration.class, this.locking)
        ;
    }

    @Override
    public ConfigurationBuilder createConfigurationBuilder() {
        ConfigurationBuilder builder = super.createConfigurationBuilder();
        builder.clustering().stateTransfer().read(this.stateTransfer.getValue());

        CacheMode mode = builder.clustering().cacheMode();

        if (mode.isSynchronous() && (this.transaction.getValue().lockingMode() == LockingMode.OPTIMISTIC) && (this.locking.getValue().isolationLevel() == IsolationLevel.REPEATABLE_READ)) {
            builder.locking().writeSkewCheck(true);
            builder.versioning().enable().scheme(VersioningScheme.SIMPLE);
        }

        SitesConfigurationBuilder sitesBuilder = builder.sites();
        sitesBuilder.read(this.backups.getValue());
        sitesBuilder.backupFor().read(this.backupFor.getValue());

        return builder;
    }
}
