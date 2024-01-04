/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import static org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.ListAttribute.MODULES;

import java.util.Collections;
import java.util.EnumSet;

import org.jboss.as.clustering.controller.ModulesServiceConfigurator;
import org.jboss.as.clustering.controller.ServiceValueCaptorServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ServiceValueRegistry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanBindingFactory;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * @author Radoslav Husar
 */
public class RemoteCacheContainerServiceHandler extends SimpleResourceServiceHandler {

    private final ServiceValueRegistry<RemoteCacheContainer> registry;

    RemoteCacheContainerServiceHandler(ResourceServiceConfiguratorFactory configuratorFactory, ServiceValueRegistry<RemoteCacheContainer> registry) {
        super(configuratorFactory);
        this.registry = registry;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        super.installServices(context, model);

        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        ServiceTarget target = context.getServiceTarget();

        Module defaultModule = Module.forClass(RemoteCacheContainer.class);
        new ModulesServiceConfigurator(RemoteCacheContainerComponent.MODULES.getServiceName(address), MODULES, Collections.singletonList(defaultModule)).configure(context, model).build(target).setInitialMode(ServiceController.Mode.PASSIVE).install();

        ServiceConfigurator containerBuilder = new RemoteCacheContainerServiceConfigurator(address).configure(context, model);
        containerBuilder.build(target).install();

        new ServiceValueCaptorServiceConfigurator<>(this.registry.add(containerBuilder.getServiceName())).build(target).install();

        new BinderServiceConfigurator(InfinispanBindingFactory.createRemoteCacheContainerBinding(name), containerBuilder.getServiceName()).build(target).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        context.removeService(InfinispanBindingFactory.createRemoteCacheContainerBinding(name).getBinderServiceName());
        context.removeService(new ServiceValueCaptorServiceConfigurator<>(this.registry.remove(new RemoteCacheContainerServiceConfigurator(address).getServiceName())).getServiceName());

        for (RemoteCacheContainerResourceDefinition.Capability component : EnumSet.allOf(RemoteCacheContainerResourceDefinition.Capability.class)) {
            ServiceName serviceName = component.getServiceName(address);
            context.removeService(serviceName);
        }

        context.removeService(RemoteCacheContainerComponent.MODULES.getServiceName(address));

        super.removeServices(context, model);
    }
}
