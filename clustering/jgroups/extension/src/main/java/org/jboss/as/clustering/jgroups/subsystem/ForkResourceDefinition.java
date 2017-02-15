/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

import org.jboss.as.clustering.controller.CapabilityProvider;
import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * Definition of a fork resource.
 * @author Paul Ferraro
 */
public class ForkResourceDefinition extends ChildResourceDefinition {

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

    static final Map<ClusteringRequirement, org.jboss.as.clustering.controller.Capability> CLUSTERING_CAPABILITIES = new EnumMap<>(ClusteringRequirement.class);
    static {
        EnumSet.allOf(ClusteringRequirement.class).forEach(requirement -> CLUSTERING_CAPABILITIES.put(requirement, new UnaryRequirementCapability(requirement)));
    }

    ForkResourceDefinition() {
        super(WILDCARD_PATH, new JGroupsResourceDescriptionResolver(WILDCARD_PATH));
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(Capability.class)
                .addCapabilities(CLUSTERING_CAPABILITIES.values())
                ;
        ResourceServiceBuilderFactory<ChannelFactory> builderFactory = address -> new ForkChannelFactoryBuilder(Capability.FORK_CHANNEL_FACTORY.getServiceName(address), address.getParent().getLastElement().getValue());
        ResourceServiceHandler handler = new ForkServiceHandler(builderFactory);
        new SimpleResourceRegistration(descriptor, handler).register(registration);

        new ProtocolRegistration(builderFactory, new ForkProtocolResourceRegistrationHandler()).register(registration);
    }
}
