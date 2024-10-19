/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PartitionHandlingConfiguration;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.StateTransferConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a resource definition for a shared state cache configuration.
 * @author Paul Ferraro
 */
public class SharedStateCacheResourceDefinitionRegistrar extends ClusteredCacheResourceDefinitionRegistrar {

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    SharedStateCacheResourceDefinitionRegistrar(CacheResourceRegistration registration, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(registration);
        this.executors = executors;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .requireChildResources(Set.of(ComponentResourceRegistration.PARTITION_HANDLING, ComponentResourceRegistration.STATE_TRANSFER, ComponentResourceRegistration.BACKUP_SITES))
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new PartitionHandlingResourceDefinitionRegistrar().register(registration, context);
        new StateTransferResourceDefinitionRegistrar().register(registration, context);
        new BackupSitesResourceDefinitionRegistrar(this.executors).register(registration, context);

        return registration;
    }

    @Override
    public ServiceDependency<ConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        BinaryServiceConfiguration config = BinaryServiceConfiguration.of(address.getParent().getLastElement().getValue(), address.getLastElement().getValue());

        ServiceDependency<ConfigurationBuilder> configurationBuilder = super.resolve(context, model);
        ServiceDependency<PartitionHandlingConfiguration> partitioning = config.getServiceDependency(PartitionHandlingResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);
        ServiceDependency<StateTransferConfiguration> stateTransfer = config.getServiceDependency(StateTransferResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);
        ServiceDependency<SitesConfiguration> sites = config.getServiceDependency(BackupSitesResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);
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
