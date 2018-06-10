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

import static org.jboss.as.clustering.infinispan.subsystem.TransportResourceDefinition.CLUSTERING_CAPABILITIES;

import java.util.EnumSet;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.spi.CapabilityServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.IdentityGroupServiceConfiguratorProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class JGroupsTransportServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.getParent();
        String name = containerAddress.getLastElement().getValue();
        ServiceTarget target = context.getServiceTarget();

        JGroupsTransportServiceConfigurator transportBuilder = new JGroupsTransportServiceConfigurator(address).configure(context, model);
        transportBuilder.build(target).install();

        new SiteServiceConfigurator(address).configure(context, model).build(target).install();

        String channel = transportBuilder.getChannel();

        ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

        for (IdentityGroupServiceConfiguratorProvider provider : ServiceLoader.load(IdentityGroupServiceConfiguratorProvider.class, IdentityGroupServiceConfiguratorProvider.class.getClassLoader())) {
            for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(registry, name, channel)) {
                configurator.configure(context).build(target).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.getParent();
        String name = containerAddress.getLastElement().getValue();

        ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

        for (IdentityGroupServiceConfiguratorProvider provider : ServiceLoader.load(IdentityGroupServiceConfiguratorProvider.class, IdentityGroupServiceConfiguratorProvider.class.getClassLoader())) {
            for (ServiceNameProvider configurator : provider.getServiceConfigurators(registry, name, null)) {
                context.removeService(configurator.getServiceName());
            }
        }

        for (CacheContainerComponent component: EnumSet.allOf(CacheContainerComponent.class)) {
            context.removeService(component.getServiceName(address));
        }
    }
}
