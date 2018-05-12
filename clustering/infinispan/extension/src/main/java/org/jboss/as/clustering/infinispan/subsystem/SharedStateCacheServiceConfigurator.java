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
