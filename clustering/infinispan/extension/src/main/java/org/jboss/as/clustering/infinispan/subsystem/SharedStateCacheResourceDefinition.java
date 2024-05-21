/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PartitionHandlingConfiguration;
import org.infinispan.configuration.cache.SitesConfiguration;
import org.infinispan.configuration.cache.SitesConfigurationBuilder;
import org.infinispan.configuration.cache.StateTransferConfiguration;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.server.util.MapEntry;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Base class for cache resources which require common cache attributes, clustered cache attributes
 * and shared cache attributes.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class SharedStateCacheResourceDefinition extends ClusteredCacheResourceDefinition {

    static final Set<PathElement> REQUIRED_CHILDREN = Set.of(PartitionHandlingResourceDefinition.PATH, StateTransferResourceDefinition.PATH, BackupsResourceDefinition.PATH);

    private static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        private final UnaryOperator<ResourceDescriptor> configurator;

        ResourceDescriptorConfigurator(UnaryOperator<ResourceDescriptor> configurator) {
            this.configurator = configurator;
        }

        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return this.configurator.apply(descriptor).addRequiredChildren(REQUIRED_CHILDREN);
        }
    }

    private final FunctionExecutorRegistry<Cache<?, ?>> executors;

    SharedStateCacheResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, CacheMode mode, FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(path, new ResourceDescriptorConfigurator(configurator), mode, executors);
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new PartitionHandlingResourceDefinition().register(registration);
        new StateTransferResourceDefinition().register(registration);
        new BackupsResourceDefinition(this.executors).register(registration);

        return registration;
    }

    @Override
    public MapEntry<Consumer<ConfigurationBuilder>, Stream<Consumer<RequirementServiceBuilder<?>>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String containerName = address.getParent().getLastElement().getValue();
        String cacheName = address.getLastElement().getValue();

        ServiceDependency<PartitionHandlingConfiguration> partitionHandling = ServiceDependency.on(PartitionHandlingResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);
        ServiceDependency<StateTransferConfiguration> stateTransfer = ServiceDependency.on(StateTransferResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);
        ServiceDependency<SitesConfiguration> backups = ServiceDependency.on(BackupsResourceDefinition.SERVICE_DESCRIPTOR, containerName, cacheName);

        return super.resolve(context, model).map(consumer -> consumer.andThen(new Consumer<>() {
            @Override
            public void accept(ConfigurationBuilder builder) {
                builder.clustering().partitionHandling().read(partitionHandling.get());
                builder.clustering().stateTransfer().read(stateTransfer.get());

                SitesConfigurationBuilder sitesBuilder = builder.sites();
                sitesBuilder.read(backups.get());
            }
        }), stream -> Stream.concat(stream, Stream.of(partitionHandling, stateTransfer, backups)));
    }
}
