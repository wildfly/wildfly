/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.extension.clustering.web.routing.infinispan.InfinispanRoutingProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-web/routing=infinispan resource.
 * @author Paul Ferraro
 */
public class InfinispanRoutingProviderResourceDefinition extends RoutingProviderResourceDefinition {

    static final PathElement PATH = pathElement("infinispan");

    static final RuntimeCapability<Void> INFINISPAN_ROUTING_PROVIDER = RuntimeCapability.Builder.of(RoutingProvider.INFINISPAN_SERVICE_DESCRIPTOR).build();
    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(INFINISPAN_ROUTING_PROVIDER);

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes()).addCapabilities(List.of(INFINISPAN_ROUTING_PROVIDER));
        }
    }

    InfinispanRoutingProviderResourceDefinition() {
        super(PATH, new ResourceDescriptorConfigurator());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(ROUTING_PROVIDER, new InfinispanRoutingProvider(CACHE_ATTRIBUTE_GROUP.resolve(context, model), UnaryOperator.identity()))
                .provides(INFINISPAN_ROUTING_PROVIDER.getCapabilityServiceName())
                .build();
    }
}
