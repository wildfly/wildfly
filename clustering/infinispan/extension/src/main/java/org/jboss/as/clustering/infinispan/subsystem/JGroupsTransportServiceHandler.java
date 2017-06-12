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

import static org.jboss.as.clustering.infinispan.subsystem.JGroupsTransportResourceDefinition.*;

import java.util.EnumSet;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.spi.GroupAliasBuilderProvider;

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

        JGroupsTransportBuilder transportBuilder = new JGroupsTransportBuilder(containerAddress).configure(context, model);
        transportBuilder.build(target).install();

        new SiteBuilder(containerAddress).configure(context, model).build(target).install();

        String channel = transportBuilder.getChannel();

        for (GroupAliasBuilderProvider provider : ServiceLoader.load(GroupAliasBuilderProvider.class, GroupAliasBuilderProvider.class.getClassLoader())) {
            for (CapabilityServiceBuilder<?> builder : provider.getBuilders(requirement -> CLUSTERING_CAPABILITIES.get(requirement).getServiceName(address), name, channel)) {
                builder.configure(context).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        PathAddress containerAddress = address.getParent();
        String name = containerAddress.getLastElement().getValue();

        for (GroupAliasBuilderProvider provider : ServiceLoader.load(GroupAliasBuilderProvider.class, GroupAliasBuilderProvider.class.getClassLoader())) {
            for (ServiceNameProvider builder : provider.getBuilders(requirement -> CLUSTERING_CAPABILITIES.get(requirement).getServiceName(address), name, null)) {
                context.removeService(builder.getServiceName());
            }
        }

        EnumSet.allOf(CacheContainerComponent.class).stream().map(component -> component.getServiceName(containerAddress)).forEach(serviceName -> context.removeService(serviceName));
    }
}
