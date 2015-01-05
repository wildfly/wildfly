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

import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Channel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.service.ChannelBuilder;
import org.wildfly.clustering.jgroups.spi.service.ChannelConnectorBuilder;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceName;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceNameFactory;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceName;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceNameFactory;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.ClusteredGroupBuilderProvider;
import org.wildfly.clustering.spi.GroupBuilderProvider;

/**
 * Handler for /subsystem=jgroups/channel=*:add() operations
 * @author Paul Ferraro
 */
public class ChannelAddHandler extends AbstractAddStepHandler {

    private final boolean allowRuntimeOnlyRegistration;

    ChannelAddHandler(boolean allowRuntimeOnlyRegistration) {
        super(ChannelResourceDefinition.ATTRIBUTES);
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.populateModel(context, operation, resource);

        // Register runtime resource children for channel protocols
        if (this.allowRuntimeOnlyRegistration && (context.getRunningMode() == RunningMode.NORMAL)) {
            PathAddress address = Operations.getPathAddress(operation);
            String name = address.getLastElement().getValue();
            String stack = ModelNodes.asString(ChannelResourceDefinition.STACK.resolveModelAttribute(context, resource.getModel()));

            PathAddress subsystemAddress = address.subAddress(0, address.size() - 1);
            // Lookup the name of the default stack if necessary
            PathAddress stackAddress = subsystemAddress.append(StackResourceDefinition.pathElement((stack != null) ? stack : JGroupsSubsystemResourceDefinition.DEFAULT_STACK.resolveModelAttribute(context, context.readResourceFromRoot(subsystemAddress, false).getModel()).asString()));

            context.addStep(new ProtocolResourceRegistrationHandler(name, stackAddress), OperationContext.Stage.MODEL);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        installRuntimeServices(context, operation, model);
    }

    static void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        String name = Operations.getPathAddress(operation).getLastElement().getValue();
        String stack = ModelNodes.asString(ChannelResourceDefinition.STACK.resolveModelAttribute(context, model), ProtocolStackServiceNameFactory.DEFAULT_STACK);

        ModuleIdentifier module = ModelNodes.asModuleIdentifier(ChannelResourceDefinition.MODULE.resolveModelAttribute(context, model));

        ServiceTarget target = context.getServiceTarget();

        // Install channel factory alias
        new AliasServiceBuilder<>(ChannelServiceName.FACTORY.getServiceName(name), ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(stack), ChannelFactory.class).build(target).install();

        // Install channel
        new ChannelBuilder(name).build(target).install();

        // Install channel connector
        new ChannelConnectorBuilder(name).build(target).install();

        // Install channel jndi binding
        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(name), ChannelServiceName.CHANNEL.getServiceName(name), Channel.class).build(target).install();

        // Install fork channel factory
        new ForkChannelFactoryBuilder(name).build(target).install();

        // Install fork channel factory jndi binding
        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelFactoryBinding(name), ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(name), ChannelFactory.class).build(target).install();

        // Install group services for channel
        for (GroupBuilderProvider provider : ServiceLoader.load(ClusteredGroupBuilderProvider.class, ClusteredGroupBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(name, module)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Installing %s for channel %s", builder.getServiceName(), name);
                builder.build(target).install();
            }
        }
    }

    static void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) {
        String name = Operations.getPathAddress(operation).getLastElement().getValue();

        context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());

        for (ChannelServiceNameFactory factory : ChannelServiceName.values()) {
            context.removeService(factory.getServiceName(name));
        }

        context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(name).getBinderServiceName());
        context.removeService(ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(name));

        for (GroupBuilderProvider provider : ServiceLoader.load(ClusteredGroupBuilderProvider.class, ClusteredGroupBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(name, null)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Removing %s for channel %s", builder.getServiceName(), name);
                context.removeService(builder.getServiceName());
            }
        }
    }
}
