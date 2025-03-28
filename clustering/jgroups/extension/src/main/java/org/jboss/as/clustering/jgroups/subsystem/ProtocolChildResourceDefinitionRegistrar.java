/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.List;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ModuleAttributeDefinition;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
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
 * Registers a resource definition for a typed JGroups protocol component.
 * @author Paul Ferraro
 */
public class ProtocolChildResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, UnaryOperator<ResourceDescriptor.Builder> {
    static final ModuleAttributeDefinition MODULE = new ModuleAttributeDefinition.Builder().setDefaultValue(Module.forClass(JChannel.class)).build();
    static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder().build();
    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().setDefaultValue(null).build();

    interface Configurator {
        ResourceRegistration getResourceRegistration();

        ResourceDescriptionResolver getResourceDescriptionResolver();

        default UnaryOperator<ResourceDefinition.Builder> getResourceDefinitionConfigurator() {
            return UnaryOperator.identity();
        }

        default JGroupsSubsystemModel getDeprecation() {
            return null;
        }
    }

    private final Configurator configurator;

    ProtocolChildResourceDefinitionRegistrar(Configurator configurator) {
        this.configurator = configurator;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = this.configurator.getResourceDescriptionResolver();
        ResourceDescriptor descriptor = this.apply(ResourceDescriptor.builder(resolver)).build();
        ResourceDefinition definition = this.configurator.getResourceDefinitionConfigurator().apply(ResourceDefinition.builder(this.configurator.getResourceRegistration(), resolver, this.configurator.getDeprecation())).build();

        ManagementResourceRegistration registration = parent.registerSubModel(definition);

        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return builder.addAttributes(List.of(MODULE, PROPERTIES, STATISTICS_ENABLED));
    }
}
