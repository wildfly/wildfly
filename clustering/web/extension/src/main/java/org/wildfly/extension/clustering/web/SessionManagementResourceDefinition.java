/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.web.service.WebProviderRequirement;
import org.wildfly.clustering.web.service.WebRequirement;

/**
 * Base definition for session management resources.
 * @author Paul Ferraro
 */
public class SessionManagementResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    enum Capability implements CapabilityProvider, UnaryOperator<RuntimeCapability.Builder<Void>> {
        SESSION_MANAGEMENT_PROVIDER(WebProviderRequirement.SESSION_MANAGEMENT_PROVIDER),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(UnaryRequirement requirement) {
            this.capability = new UnaryRequirementCapability(requirement, this);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }

        @Override
        public RuntimeCapability.Builder<Void> apply(RuntimeCapability.Builder<Void> builder) {
            return builder.setDynamicNameMapper(UnaryCapabilityNameResolver.DEFAULT)
                    .addRequirements(WebRequirement.ROUTING_PROVIDER.getName());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        GRANULARITY("granularity", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(EnumValidator.create(SessionGranularity.class));
            }
        },
        MARSHALLER("marshaller", ModelType.STRING, new ModelNode(SessionMarshallerFactory.JBOSS.name())) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setValidator(EnumValidator.create(SessionMarshallerFactory.class));
            }
        }
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setFlags(Flag.RESTART_RESOURCE_SERVICES)
                ).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    private final UnaryOperator<ResourceDescriptor> configurator;
    private final ResourceServiceConfiguratorFactory factory;

    public SessionManagementResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory factory) {
        super(path, DistributableWebExtension.SUBSYSTEM_RESOLVER.createChildResolver(path, PathElement.pathElement("session-management")));
        this.configurator = configurator;
        this.factory = factory;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addAttributes(Attribute.class)
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(this.factory);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        new NoAffinityResourceDefinition().register(registration);
        new LocalAffinityResourceDefinition().register(registration);

        return registration;
    }
}
