/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.stream.Collectors;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.server.service.ClusteringRequirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Definition of a fork resource.
 * @author Paul Ferraro
 */
public class ForkResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    public static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("fork", name);
    }

    enum Capability implements CapabilityProvider {
        FORK_CHANNEL(JGroupsRequirement.CHANNEL),
        FORK_CHANNEL_CLUSTER(JGroupsRequirement.CHANNEL_CLUSTER),
        FORK_CHANNEL_FACTORY(JGroupsRequirement.CHANNEL_FACTORY),
        FORK_CHANNEL_MODULE(JGroupsRequirement.CHANNEL_MODULE),
        FORK_CHANNEL_SOURCE(JGroupsRequirement.CHANNEL_SOURCE),
        ;
        private final org.jboss.as.clustering.controller.Capability capability;

        Capability(UnaryRequirement requirement) {
            this.capability = new UnaryRequirementCapability(requirement);
        }

        @Override
        public org.jboss.as.clustering.controller.Capability getCapability() {
            return this.capability;
        }
    }

    static class ForkChannelFactoryServiceConfiguratorFactory implements ResourceServiceConfiguratorFactory {
        @Override
        public ResourceServiceConfigurator createServiceConfigurator(PathAddress address) {
            return new ForkChannelFactoryServiceConfigurator(Capability.FORK_CHANNEL_FACTORY, address);
        }
    }

    private final ServiceValueExecutorRegistry<JChannel> registry;

    ForkResourceDefinition(ServiceValueExecutorRegistry<JChannel> registry) {
        super(WILDCARD_PATH, JGroupsExtension.SUBSYSTEM_RESOLVER.createChildResolver(WILDCARD_PATH));
        this.registry = registry;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = parent.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(Capability.class)
                .addCapabilities(EnumSet.allOf(ClusteringRequirement.class).stream().map(UnaryRequirementCapability::new).collect(Collectors.toList()))
                ;
        ResourceServiceConfiguratorFactory serviceConfiguratorFactory = new ForkChannelFactoryServiceConfiguratorFactory();
        ResourceServiceHandler handler = new ForkServiceHandler(serviceConfiguratorFactory, this.registry);
        new SimpleResourceRegistrar(descriptor, handler).register(registration);

        new ProtocolResourceRegistrar(serviceConfiguratorFactory, new ForkProtocolRuntimeResourceRegistration(this.registry)).register(registration);

        return registration;
    }
}
