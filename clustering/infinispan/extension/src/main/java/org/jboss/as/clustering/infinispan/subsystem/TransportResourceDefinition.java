/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ChildResourceDefinition;
import org.jboss.as.clustering.controller.Registration;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleResourceRegistration;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.spi.ClusteringRequirement;

/**
 * @author Paul Ferraro
 */
public abstract class TransportResourceDefinition extends ChildResourceDefinition<ManagementResourceRegistration> {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String value) {
        return PathElement.pathElement("transport", value);
    }

    static final Map<ClusteringRequirement, org.jboss.as.clustering.controller.Capability> CLUSTERING_CAPABILITIES = new EnumMap<>(ClusteringRequirement.class);
    static {
        EnumSet.allOf(ClusteringRequirement.class).forEach(requirement -> CLUSTERING_CAPABILITIES.put(requirement, new UnaryRequirementCapability(requirement) {
            @Override
            public RuntimeCapability<?> resolve(PathAddress address) {
                return super.resolve(address.getParent());
            }
        }));
    }

    private final Registration<ManagementResourceRegistration> registrar;

    TransportResourceDefinition(PathElement path, Consumer<ResourceDescriptor> configurator, ResourceServiceHandler handler) {
        super(path, new InfinispanResourceDescriptionResolver(path));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addCapabilities(CLUSTERING_CAPABILITIES.values())
                ;
        configurator.accept(descriptor);
        this.registrar = new SimpleResourceRegistration(descriptor, handler);
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);

        this.registrar.register(registration);
    }
}
