/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Register a resource definition for a JGroups protocol.
 * @author Paul Ferraro
 */
public class ProtocolChildResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar {

    interface ProtocolChildResourceDescriptorConfigurator extends UnaryOperator<ResourceDescriptor.Builder> {
        ProtocolChildResourceDescription getResourceDescription();

        @Override
        default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
            return builder.addAttributes(this.getResourceDescription().getAttributes().collect(Collectors.toList()));
        }

        ResourceDescriptionResolver getResourceDescriptionResolver();

        default UnaryOperator<ResourceDefinition.Builder> getResourceDefinitionConfigurator() {
            return UnaryOperator.identity();
        }

        default JGroupsSubsystemModel getDeprecation() {
            return null;
        }
    }

    private final ProtocolChildResourceDescriptorConfigurator configurator;

    ProtocolChildResourceDefinitionRegistrar(ProtocolChildResourceDescriptorConfigurator configurator) {
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = this.configurator.getResourceDescriptionResolver();
        ResourceDescriptor descriptor = this.configurator.apply(ResourceDescriptor.builder(resolver)).build();

        ResourceDefinition definition = this.configurator.getResourceDefinitionConfigurator().apply(ResourceDefinition.builder(this.configurator.getResourceDescription(), resolver, this.configurator.getDeprecation())).build();

        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }
}
