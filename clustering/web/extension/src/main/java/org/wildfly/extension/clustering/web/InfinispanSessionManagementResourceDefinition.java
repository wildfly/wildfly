/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.extension.clustering.web.session.infinispan.InfinispanSessionManagementProvider;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceRecorder;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-web/infinispan-session-management=* resource.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagementResourceDefinition extends SessionManagementResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("infinispan-session-management", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CACHE_CONTAINER("cache-container", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setRequired(true)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(SESSION_MANAGEMENT_PROVIDER, InfinispanServiceDescriptor.DEFAULT_CACHE_CONFIGURATION).build())
                        ;
            }
        },
        CACHE("cache", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setCapabilityReference(CapabilityReferenceRecorder.builder(SESSION_MANAGEMENT_PROVIDER, InfinispanServiceDescriptor.CACHE_CONFIGURATION).withParentAttribute(CACHE_CONTAINER.getDefinition()).build())
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

    InfinispanSessionManagementResourceDefinition() {
        super(WILDCARD_PATH, new UnaryOperator<>() {
            @Override
            public ResourceDescriptor apply(ResourceDescriptor descriptor) {
                return descriptor.addAttributes(Attribute.class)
                        .addRequiredSingletonChildren(PrimaryOwnerAffinityResourceDefinition.PATH)
                        ;
            }
        });
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new PrimaryOwnerAffinityResourceDefinition().register(registration);
        new RankedAffinityResourceDefinition().register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<RouteLocatorProvider> locatorProvider = ServiceDependency.on(RouteLocatorProvider.SERVICE_DESCRIPTOR, context.getCurrentAddressValue());
        return CapabilityServiceInstaller.builder(SessionManagementResourceDefinition.SESSION_MANAGEMENT_PROVIDER, new InfinispanSessionManagementProvider(this.resolve(context, model), this.resolver.resolve(context, model), locatorProvider))
                .requires(locatorProvider)
                .build();
    }
}
