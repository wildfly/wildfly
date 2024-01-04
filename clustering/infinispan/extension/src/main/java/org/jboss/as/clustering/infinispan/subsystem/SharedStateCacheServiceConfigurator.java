/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PartitionHandlingConfiguration;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.SitesConfigurationBuilder;
import org.infinispan.configuration.cache.StateTransferConfiguration;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class SharedStateCacheServiceConfigurator extends ClusteredCacheServiceConfigurator {

    private final SupplierDependency<PartitionHandlingConfiguration> partitionHandling;
    private final SupplierDependency<StateTransferConfiguration> stateTransfer;
    private final SupplierDependency<SitesConfiguration> backups;

    SharedStateCacheServiceConfigurator(PathAddress address, CacheMode mode) {
        super(address, mode);
        this.partitionHandling = new ServiceSupplierDependency<>(CacheComponent.PARTITION_HANDLING.getServiceName(address));
        this.stateTransfer = new ServiceSupplierDependency<>(CacheComponent.STATE_TRANSFER.getServiceName(address));
        this.backups = new ServiceSupplierDependency<>(CacheComponent.BACKUPS.getServiceName(address));
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(new CompositeDependency(this.partitionHandling, this.stateTransfer, this.backups).register(builder));
    }

    @Override
    public void accept(ConfigurationBuilder builder) {
        super.accept(builder);

        builder.clustering().partitionHandling().read(this.partitionHandling.get());
        builder.clustering().stateTransfer().read(this.stateTransfer.get());

        SitesConfigurationBuilder sitesBuilder = builder.sites();
        sitesBuilder.read(this.backups.get());
    }
}
