/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.clustering.web.routing.infinispan.InfinispanRoutingProvider;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-web/routing=infinispan resource.
 * @author Paul Ferraro
 */
public class InfinispanRoutingProviderResourceDefinition extends RoutingProviderResourceDefinition {

    static final PathElement PATH = pathElement("infinispan");

    static final RuntimeCapability<Void> INFINISPAN_ROUTING_PROVIDER = RuntimeCapability.Builder.of(RoutingProvider.INFINISPAN_SERVICE_DESCRIPTOR).build();

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CACHE_CONTAINER("cache-container", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(INFINISPAN_ROUTING_PROVIDER, InfinispanServiceDescriptor.CACHE_CONTAINER).build())
                        ;
            }
        },
        CACHE("cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(INFINISPAN_ROUTING_PROVIDER, InfinispanServiceDescriptor.CACHE).withParentAttribute(CACHE_CONTAINER.getDefinition()).build())
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(Attribute.class).addCapabilities(List.of(INFINISPAN_ROUTING_PROVIDER));
        }
    }

    private final ResourceModelResolver<BinaryServiceConfiguration> resolver = BinaryServiceConfiguration.resolver(Attribute.CACHE_CONTAINER.getDefinition(), Attribute.CACHE.getDefinition());

    InfinispanRoutingProviderResourceDefinition() {
        super(PATH, new ResourceDescriptorConfigurator());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(ROUTING_PROVIDER, new InfinispanRoutingProvider(this.resolver.resolve(context, model), Functions.discardingConsumer()))
                .provides(INFINISPAN_ROUTING_PROVIDER.getCapabilityServiceName())
                .build();
    }
}
