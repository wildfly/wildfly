/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.server.service.ClusteringRequirement;

/**
 * @author Paul Ferraro
 */
public abstract class TransportResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String value) {
        return PathElement.pathElement("transport", value);
    }

    private final UnaryOperator<ResourceDescriptor> configurator;
    private final ResourceServiceHandler handler;

    TransportResourceDefinition(PathElement path, UnaryOperator<ResourceDescriptor> configurator, ResourceServiceHandler handler) {
        super(path, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(path));
        this.configurator = configurator;
        this.handler = handler;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        Set<ClusteringRequirement> requirements = EnumSet.allOf(ClusteringRequirement.class);
        List<Capability> capabilities = new ArrayList<>(requirements.size());
        for (ClusteringRequirement requirement : requirements) {
            capabilities.add(new UnaryRequirementCapability(requirement, UnaryCapabilityNameResolver.PARENT));
        }

        ResourceDescriptor descriptor = this.configurator.apply(new ResourceDescriptor(this.getResourceDescriptionResolver()))
                .addCapabilities(capabilities)
                ;
        new SimpleResourceRegistrar(descriptor, this.handler).register(registration);

        return registration;
    }
}
