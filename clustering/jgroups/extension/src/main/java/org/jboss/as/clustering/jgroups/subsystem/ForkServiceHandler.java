/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL;
import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL_CLUSTER;
import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL_FACTORY;
import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL_MODULE;
import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL_SOURCE;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.server.service.ProvidedIdentityGroupServiceConfigurator;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 * @author Paul Ferraro
 */
public class ForkServiceHandler extends SimpleResourceServiceHandler {

    private final ServiceValueRegistry<JChannel> registry;
    private final Map<PathAddress, Consumer<OperationContext>> removers = new ConcurrentHashMap<>();

    ForkServiceHandler(ResourceServiceConfiguratorFactory factory, ServiceValueRegistry<JChannel> registry) {
        super(factory);
        this.registry = registry;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {

        super.installServices(context, model);

        PathAddress address = context.getCurrentAddress();
        String name = address.getLastElement().getValue();
        String channel = address.getParent().getLastElement().getValue();

        ServiceTarget target = context.getServiceTarget();

        new IdentityServiceConfigurator<>(FORK_CHANNEL_SOURCE.getServiceName(address), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, channel)).build(target).install();
        new IdentityServiceConfigurator<>(FORK_CHANNEL_MODULE.getServiceName(address), JGroupsRequirement.CHANNEL_MODULE.getServiceName(context, channel)).build(target).install();
        new IdentityServiceConfigurator<>(FORK_CHANNEL_CLUSTER.getServiceName(address), JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, channel)).build(target).install();

        this.removers.put(address, this.registry.capture(ServiceDependency.on(ForkResourceDefinition.Capability.FORK_CHANNEL.getDefinition().getCapabilityServiceName(address))).install(context));
        new ChannelServiceConfigurator(FORK_CHANNEL, address).configure(context, model).build(target).install();

        new BinderServiceConfigurator(JGroupsBindingFactory.createChannelBinding(name), JGroupsRequirement.CHANNEL.getServiceName(context, name)).build(target).install();
        new BinderServiceConfigurator(JGroupsBindingFactory.createChannelFactoryBinding(name), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, name)).build(target).install();

        new ProvidedIdentityGroupServiceConfigurator(name, channel).configure(context).build(target).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {

        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();
        String channel = address.getParent().getLastElement().getValue();

        ServiceName channelServiceName = ForkResourceDefinition.Capability.FORK_CHANNEL.getDefinition().getCapabilityServiceName(address);
        this.registry.remove(ServiceDependency.on(channelServiceName));

        new ProvidedIdentityGroupServiceConfigurator(name, channel).remove(context);

        context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());
        context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(name).getBinderServiceName());

        Consumer<OperationContext> remover = this.removers.remove(address);
        if (remover != null) {
            remover.accept(context);
        }

        // FORK_CHANNEL_FACTORY is removed by super impl
        for (Capability capability : EnumSet.complementOf(EnumSet.of(FORK_CHANNEL_FACTORY))) {
            context.removeService(capability.getServiceName(address));
        }

        super.removeServices(context, model);
    }
}
