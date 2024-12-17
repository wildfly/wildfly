/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.clustering.web.sso.infinispan.InfinispanUserManagementProvider;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-web/infinispan-single-sign-on-management=* resource.
 * @author Paul Ferraro
 */
public class InfinispanUserManagementResourceDefinition extends UserManagementResourceDefinition {

    static final PathElement WILDCARD_PATH = PathElement.pathElement("infinispan-single-sign-on-management");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CACHE_CONTAINER("cache-container", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(UserManagementResourceDefinition.CAPABILITY, InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION).build())
                        ;
            }
        },
        CACHE("cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(UserManagementResourceDefinition.CAPABILITY, InfinispanServiceDescriptor.CACHE_CONFIGURATION).withParentAttribute(CACHE_CONTAINER.getDefinition()).build())
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

    private final ResourceModelResolver<BinaryServiceConfiguration> resolver = BinaryServiceConfiguration.resolver(Attribute.CACHE_CONTAINER.getDefinition(), Attribute.CACHE.getDefinition());

    InfinispanUserManagementResourceDefinition() {
        super(WILDCARD_PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, Functions.constantSupplier(new InfinispanUserManagementProvider(this.resolver.resolve(context, model)))).build();
    }
}
