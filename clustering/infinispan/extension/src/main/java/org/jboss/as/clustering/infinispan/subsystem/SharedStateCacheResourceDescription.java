/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Set;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PartitionHandlingConfiguration;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.StateTransferConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a shared state cache resource.
 * @author Paul Ferraro
 */
public interface SharedStateCacheResourceDescription extends ClusteredCacheResourceDescription {

    @Override
    default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return ClusteredCacheResourceDescription.super.apply(builder)
                .requireChildResources(Set.of(PartitionHandlingResourceDescription.INSTANCE, StateTransferResourceDescription.INSTANCE, BackupSitesResourceDescription.INSTANCE))
                ;
    }

    @Override
    default ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        BinaryServiceConfiguration config = BinaryServiceConfiguration.of(address.getParent().getLastElement().getValue(), address.getLastElement().getValue());

        ServiceDependency<ConfigurationBuilder> configurationBuilder = ClusteredCacheResourceDescription.super.resolve(context, model);
        ServiceDependency<PartitionHandlingConfiguration> partitioning = config.getServiceDependency(PartitionHandlingResourceDescription.INSTANCE.getServiceDescriptor());
        ServiceDependency<StateTransferConfiguration> stateTransfer = config.getServiceDependency(StateTransferResourceDescription.INSTANCE.getServiceDescriptor());
        ServiceDependency<SitesConfiguration> sites = config.getServiceDependency(BackupSitesResourceDescription.INSTANCE.getServiceDescriptor());
        return new ServiceDependency<>() {
            @Override
            public void accept(RequirementServiceBuilder<?> builder) {
                configurationBuilder.accept(builder);
                partitioning.accept(builder);
                stateTransfer.accept(builder);
                sites.accept(builder);
            }

            @Override
            public ConfigurationBuilder get() {
                ConfigurationBuilder builder = configurationBuilder.get();
                builder.clustering().partitionHandling().read(partitioning.get());
                builder.clustering().stateTransfer().read(stateTransfer.get());
                builder.clustering().sites().read(sites.get());
                return builder;
            }
        };
    }
}
