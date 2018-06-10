/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem.remote;

import static org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.Attribute.MODULE;

import java.util.EnumSet;

import org.jboss.as.clustering.controller.ModuleServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanBindingFactory;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Radoslav Husar
 */
public class RemoteCacheContainerServiceHandler extends SimpleResourceServiceHandler {

    RemoteCacheContainerServiceHandler(ResourceServiceConfiguratorFactory configuratorFactory) {
        super(configuratorFactory);
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        super.installServices(context, model);

        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        ServiceTarget target = context.getServiceTarget();

        new ModuleServiceConfigurator(RemoteCacheContainerComponent.MODULE.getServiceName(address), MODULE).configure(context, model).build(target).setInitialMode(ServiceController.Mode.PASSIVE).install();

        ServiceConfigurator containerBuilder = new RemoteCacheContainerServiceConfigurator(address).configure(context, model);
        containerBuilder.build(target).install();

        new BinderServiceConfigurator(InfinispanBindingFactory.createRemoteCacheContainerBinding(name), containerBuilder.getServiceName()).build(target).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        context.removeService(InfinispanBindingFactory.createRemoteCacheContainerBinding(name).getBinderServiceName());

        for (RemoteCacheContainerResourceDefinition.Capability component : EnumSet.allOf(RemoteCacheContainerResourceDefinition.Capability.class)) {
            ServiceName serviceName = component.getServiceName(address);
            context.removeService(serviceName);
        }

        context.removeService(RemoteCacheContainerComponent.MODULE.getServiceName(address));

        super.removeServices(context, model);
    }
}
