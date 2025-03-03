/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.ejb.infinispan.remote.InfinispanModuleAvailabilityRegistrarProvider;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

import java.util.function.UnaryOperator;

/**
 * Definition of the /subsystem=distributable-ejb/module-availability-registrar=infinispan resource.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanModuleAvailabilityRegistrarProviderResourceDefinition extends ModuleAvailabilityRegistrarProviderResourceDefinition {

    static final PathElement PATH = pathElement("infinispan");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CACHE_CONTAINER("cache-container", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setRequired(true)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION).build())
                        ;
            }
        },
        CACHE("cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setRequired(false)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(CAPABILITY, InfinispanServiceDescriptor.CACHE_CONFIGURATION).withParentAttribute(CACHE_CONTAINER.getDefinition()).build());
            }
        }
        ;

        private final AttributeDefinition definition ;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final ResourceModelResolver<BinaryServiceConfiguration> resolver = BinaryServiceConfiguration.resolver(Attribute.CACHE_CONTAINER.getDefinition(), Attribute.CACHE.getDefinition());

    InfinispanModuleAvailabilityRegistrarProviderResourceDefinition() {
        super(PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, new InfinispanModuleAvailabilityRegistrarProvider(this.resolver.resolve(context, model))).build();
    }
}
