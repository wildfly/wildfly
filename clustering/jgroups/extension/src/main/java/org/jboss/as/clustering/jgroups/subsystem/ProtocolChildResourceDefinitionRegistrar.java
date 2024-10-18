/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.ModuleAttributeDefinition;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.modules.Module;
import org.jgroups.JChannel;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Register a resource definition for a JGroups protocol.
 * @author Paul Ferraro
 */
public class ProtocolChildResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar {

    static final ModuleAttributeDefinition MODULE = new ModuleAttributeDefinition.Builder().setDefaultValue(Module.forClass(JChannel.class)).build();
    static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder().build();
    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().setDefaultValue(null).build();

    private static final Collection<AttributeDefinition> ATTRIBUTES = List.of(MODULE, PROPERTIES, STATISTICS_ENABLED);
    static Stream<AttributeDefinition> attributes() {
        return ATTRIBUTES.stream();
    }

    interface ProtocolChildResourceRegistration extends ResourceRegistration, UnaryOperator<ResourceDescriptor.Builder> {
        @Override
        default ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
            return builder.addAttributes(ATTRIBUTES);
        }
        ResourceDescriptionResolver getResourceDescriptionResolver();
        default UnaryOperator<ResourceDefinition.Builder> getResourceDefinitionConfigurator() {
            return UnaryOperator.identity();
        }
        default JGroupsSubsystemModel getDeprecation() {
            return null;
        }
    }

    private final ProtocolChildResourceRegistration registration;

    ProtocolChildResourceDefinitionRegistrar(ProtocolChildResourceRegistration configuration) {
        this.registration = configuration;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = this.registration.getResourceDescriptionResolver();
        ResourceDescriptor descriptor = this.registration.apply(ResourceDescriptor.builder(resolver)).build();

        ResourceDefinition definition = this.registration.getResourceDefinitionConfigurator().apply(ResourceDefinition.builder(this.registration, resolver, this.registration.getDeprecation())).build();

        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }
}
