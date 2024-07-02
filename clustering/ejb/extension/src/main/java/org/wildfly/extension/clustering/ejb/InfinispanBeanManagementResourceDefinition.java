/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

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
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanManagementProvider;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-ejb/infinispan-bean-management=* resource.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanBeanManagementResourceDefinition extends BeanManagementResourceDefinition {

    static PathElement pathElement(String name) {
        return PathElement.pathElement("infinispan-bean-management", name);
    }
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CACHE_CONTAINER("cache-container", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setRequired(true).setCapabilityReference(CapabilityReferenceRecorder.builder(BEAN_MANAGEMENT_PROVIDER, InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION).build());
            }
        },
        CACHE("cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(CapabilityReferenceRecorder.builder(BEAN_MANAGEMENT_PROVIDER, InfinispanServiceDescriptor.CACHE_CONFIGURATION).withParentAttribute(CACHE_CONTAINER.getDefinition()).build());
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
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

    InfinispanBeanManagementResourceDefinition() {
        super(WILDCARD_PATH, new SimpleResourceDescriptorConfigurator<>(Attribute.class));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        BeanManagementProvider provider = new InfinispanBeanManagementProvider<>(name, this.resolve(context, model), this.resolver.resolve(context, model));
        return CapabilityServiceInstaller.builder(BEAN_MANAGEMENT_PROVIDER, provider).build();
    }
}
