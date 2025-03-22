/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.extension.clustering.web.routing.infinispan.InfinispanRoutingProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for an Infinispan routing provider.
 * @author Paul Ferraro
 */
public class InfinispanRoutingProviderResourceDefinitionRegistrar extends RoutingProviderResourceDefinitionRegistrar {

    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(RoutingProvider.INFINISPAN_SERVICE_DESCRIPTOR).build();
    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(CAPABILITY);

    InfinispanRoutingProviderResourceDefinitionRegistrar() {
        super(RoutingProviderResourceRegistration.INFINISPAN);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addCapability(CAPABILITY)
                .addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes())
                ;
    }

    @Override
    public RoutingProvider resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return new InfinispanRoutingProvider(CACHE_ATTRIBUTE_GROUP.resolve(context, model), UnaryOperator.identity());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        // Additionally provide an implementation-specific capability
        // This allows an affinity resource to require a specific routing provider
        return CapabilityServiceInstaller.builder(RoutingProviderResourceDefinitionRegistrar.CAPABILITY, this.resolve(context, model))
                .provides(CAPABILITY.getCapabilityServiceName())
                .build();
    }
}
