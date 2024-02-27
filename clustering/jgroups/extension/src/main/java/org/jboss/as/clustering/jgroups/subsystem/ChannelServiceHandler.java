/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.MODULE;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.STACK;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.STATISTICS_ENABLED;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability.FORK_CHANNEL_FACTORY;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability.JCHANNEL;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability.JCHANNEL_FACTORY;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability.JCHANNEL_MODULE;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.ModuleServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.server.service.DistributedGroupServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.ProvidedGroupServiceConfigurator;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 * @author Paul Ferraro
 */
public class ChannelServiceHandler implements ResourceServiceHandler {

    private final ServiceValueRegistry<JChannel> registry;
    private final Map<PathAddress, Consumer<OperationContext>> removers = new ConcurrentHashMap<>();

    public ChannelServiceHandler(ServiceValueRegistry<JChannel> registry) {
        this.registry = registry;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();
        String stack = STACK.resolveModelAttribute(context, model).asString();

        ServiceTarget target = context.getServiceTarget();

        new ChannelClusterServiceConfigurator(address).configure(context, model).build(target).install();
        this.removers.put(address, this.registry.capture(ServiceDependency.on(Capability.JCHANNEL.getDefinition().getCapabilityServiceName(address))).install(context));
        new ChannelServiceConfigurator(JCHANNEL, address).statisticsEnabled(STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean()).configure(context, model).build(target).install();
        new IdentityServiceConfigurator<>(JCHANNEL_FACTORY.getServiceName(address), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, stack)).build(target).install();
        new ForkChannelFactoryServiceConfigurator(FORK_CHANNEL_FACTORY, address.append(ForkResourceDefinition.pathElement(name))).configure(context, new ModelNode()).build(target).install();
        new ModuleServiceConfigurator(JCHANNEL_MODULE.getServiceName(address), MODULE).configure(context, model).build(target).setInitialMode(ServiceController.Mode.PASSIVE).install();

        new BinderServiceConfigurator(JGroupsBindingFactory.createChannelBinding(name), JGroupsRequirement.CHANNEL.getServiceName(context, name)).build(target).install();
        new BinderServiceConfigurator(JGroupsBindingFactory.createChannelFactoryBinding(name), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, name)).build(target).install();

        // Install group services for channel
        new ProvidedGroupServiceConfigurator<>(DistributedGroupServiceConfiguratorProvider.class, name).configure(context).build(target).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        for (Capability capability : EnumSet.allOf(Capability.class)) {
            context.removeService(capability.getServiceName(address));
        }

        context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());
        context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(name).getBinderServiceName());

        // Remove group services for channel
        new ProvidedGroupServiceConfigurator<>(DistributedGroupServiceConfiguratorProvider.class, name).remove(context);

        Consumer<OperationContext> remover = this.removers.remove(address);
        if (remover != null) {
            remover.accept(context);
        }
    }
}
