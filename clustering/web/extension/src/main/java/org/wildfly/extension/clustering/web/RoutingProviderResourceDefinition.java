/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.RequirementCapability;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.service.Requirement;
import org.wildfly.clustering.web.service.WebRequirement;

/**
 * Base definition for routing provider resources.
 * @author Paul Ferraro
 */
public class RoutingProviderResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static PathElement pathElement(String value) {
        return PathElement.pathElement("routing", value);
    }

    enum Capability implements CapabilityProvider, UnaryOperator<RuntimeCapability.Builder<Void>> {
        ROUTING_PROVIDER(WebRequirement.ROUTING_PROVIDER),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(Requirement requirement) {
            this.capability = new RequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }

        @Override
        public RuntimeCapability.Builder<Void> apply(RuntimeCapability.Builder<Void> builder) {
            return builder.setAllowMultipleRegistrations(true);
        }
    }

    private final UnaryOperator<ResourceDescriptor> configurator;
    private final ResourceServiceConfiguratorFactory serviceConfiguratorFactory;

    RoutingProviderResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceConfiguratorFactory serviceConfiguratorFactory) {
        super(path, DistributableWebExtension.SUBSYSTEM_RESOLVER.createChildResolver(pathElement(PathElement.WILDCARD_VALUE), path));
        this.configurator = configurator;
        this.serviceConfiguratorFactory = serviceConfiguratorFactory;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);
        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addCapabilities(Capability.class)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler(this.serviceConfiguratorFactory);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);
        return registration;
    }
}
