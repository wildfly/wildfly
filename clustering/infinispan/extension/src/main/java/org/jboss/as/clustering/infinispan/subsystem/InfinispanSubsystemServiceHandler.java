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

import static org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemResourceDefinition.CLUSTERING_CAPABILITIES;
import static org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemResourceDefinition.LOCAL_CLUSTERING_CAPABILITIES;

import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.spi.CapabilityServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.IdentityGroupServiceConfiguratorProvider;
import org.wildfly.clustering.spi.GroupServiceConfiguratorProvider;
import org.wildfly.clustering.spi.LocalGroupServiceConfiguratorProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class InfinispanSubsystemServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        InfinispanLogger.ROOT_LOGGER.activatingSubsystem();

        PathAddress address = context.getCurrentAddress();
        ServiceTarget target = context.getServiceTarget();

        // Install local group services
        ServiceNameRegistry<ClusteringRequirement> localRegistry = new CapabilityServiceNameRegistry<>(LOCAL_CLUSTERING_CAPABILITIES, address);

        for (GroupServiceConfiguratorProvider provider : ServiceLoader.load(LocalGroupServiceConfiguratorProvider.class, LocalGroupServiceConfiguratorProvider.class.getClassLoader())) {
            InfinispanLogger.ROOT_LOGGER.debugf("Installing %s for %s group", provider.getClass().getSimpleName(), LocalGroupServiceConfiguratorProvider.LOCAL);
            for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(localRegistry, LocalGroupServiceConfiguratorProvider.LOCAL)) {
                configurator.configure(context).build(target).install();
            }
        }

        // If JGroups subsystem is not available, install default group aliases to local group.
        if (!context.hasOptionalCapability(JGroupsRequirement.CHANNEL.getDefaultRequirement().getName(), null, null)) {
            ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

            for (IdentityGroupServiceConfiguratorProvider provider : ServiceLoader.load(IdentityGroupServiceConfiguratorProvider.class, IdentityGroupServiceConfiguratorProvider.class.getClassLoader())) {
                for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(registry, null, LocalGroupServiceConfiguratorProvider.LOCAL)) {
                    configurator.configure(context).build(target).install();
                }
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();

        ServiceNameRegistry<ClusteringRequirement> localRegistry = new CapabilityServiceNameRegistry<>(LOCAL_CLUSTERING_CAPABILITIES, address);

        for (GroupServiceConfiguratorProvider provider : ServiceLoader.load(LocalGroupServiceConfiguratorProvider.class, LocalGroupServiceConfiguratorProvider.class.getClassLoader())) {
            for (ServiceNameProvider configurator : provider.getServiceConfigurators(localRegistry, LocalGroupServiceConfiguratorProvider.LOCAL)) {
                context.removeService(configurator.getServiceName());
            }
        }

        if (!context.hasOptionalCapability(JGroupsRequirement.CHANNEL.getDefaultRequirement().getName(), null, null)) {
            ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

            for (IdentityGroupServiceConfiguratorProvider provider : ServiceLoader.load(IdentityGroupServiceConfiguratorProvider.class, IdentityGroupServiceConfiguratorProvider.class.getClassLoader())) {
                for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(registry, null, LocalGroupServiceConfiguratorProvider.LOCAL)) {
                    context.removeService(configurator.getServiceName());
                }
            }
        }
    }
}
