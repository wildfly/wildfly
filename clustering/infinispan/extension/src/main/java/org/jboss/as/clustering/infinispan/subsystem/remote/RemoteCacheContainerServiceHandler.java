/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import static org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.ListAttribute.MODULES;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ModulesServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
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
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 * @author Radoslav Husar
 */
public class RemoteCacheContainerServiceHandler extends SimpleResourceServiceHandler {

    private final ServiceValueRegistry<RemoteCacheContainer> registry;
    private final Map<PathAddress, Consumer<OperationContext>> removers = new ConcurrentHashMap<>();

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

        this.removers.put(address, this.registry.capture(ServiceDependency.on(RemoteCacheContainerResourceDefinition.Capability.CONTAINER.getDefinition().getCapabilityServiceName(address))).install(context));
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

        context.removeService(RemoteCacheContainerComponent.MODULES.getServiceName(address));

        Consumer<OperationContext> remover = this.removers.remove(address);
        if (remover != null) {
            remover.accept(context);
        }

        super.removeServices(context, model);
    }
}
