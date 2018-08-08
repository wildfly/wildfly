/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;
import static org.wildfly.extension.messaging.activemq.DiscoveryGroupDefinition.CAPABILITY;
import static org.wildfly.extension.messaging.activemq.DiscoveryGroupDefinition.JGROUPS_CHANNEL;
import static org.wildfly.extension.messaging.activemq.DiscoveryGroupDefinition.JGROUPS_CHANNEL_FACTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory;
import org.apache.activemq.artemis.core.config.Configuration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.spi.ClusteringDefaultRequirement;
import org.wildfly.extension.messaging.activemq.broadcast.CommandDispatcherBroadcastEndpointFactory;

/**
 * Handler for adding a discovery group.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DiscoveryGroupAdd extends AbstractAddStepHandler {

    public static final DiscoveryGroupAdd INSTANCE = new DiscoveryGroupAdd();

    private DiscoveryGroupAdd() {
        super(DiscoveryGroupDefinition.ATTRIBUTES);
    }

    private static boolean isSubsystemResource(final OperationContext context) {
        return ModelDescriptionConstants.SUBSYSTEM.equals(context.getCurrentAddress().getParent().getLastElement().getKey());
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        CommonAttributes.renameChannelToCluster(operation);
        if (operation.hasDefined(JGROUPS_CLUSTER.getName())) {
            if (operation.hasDefined(JGROUPS_CHANNEL_FACTORY.getName()) && !operation.hasDefined(JGROUPS_CHANNEL.getName())) {
                // Handle legacy behavior
                String channel = operation.get(JGROUPS_CLUSTER.getName()).asString();
                operation.get(JGROUPS_CHANNEL.getName()).set(channel);


                final PathAddress channelAddress;
                if (isSubsystemResource(context)) {
                    channelAddress = context.getCurrentAddress().getParent().getParent().append(ModelDescriptionConstants.SUBSYSTEM, "jgroups").append("channel", channel);
                } else {
                    channelAddress = context.getCurrentAddress().getParent().getParent().getParent().append(ModelDescriptionConstants.SUBSYSTEM, "jgroups").append("channel", channel);
                }
                ModelNode addChannelOperation = Util.createAddOperation(channelAddress);
                addChannelOperation.get("stack").set(operation.get(JGROUPS_CHANNEL_FACTORY.getName()));
                // Fabricate a channel resource
                context.addStep(addChannelOperation, AddIfAbsentStepHandler.INSTANCE, OperationContext.Stage.MODEL);
            }
        }
        super.execute(context, operation);
    }

    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();

        context.registerCapability(CAPABILITY.fromBaseCapability(address));

        ModelNode model = resource.getModel();
        if (JGROUPS_CLUSTER.resolveModelAttribute(context, model).isDefined() && !JGROUPS_CHANNEL.resolveModelAttribute(context, model).isDefined()) {
            context.registerAdditionalCapabilityRequirement(ClusteringDefaultRequirement.COMMAND_DISPATCHER_FACTORY.getName(), CAPABILITY.getDynamicName(address), JGROUPS_CHANNEL_FACTORY.getName());
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        ServiceRegistry registry = context.getServiceRegistry(false);
        ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = serviceName == null ? null : registry.getService(serviceName);
        if (service != null) {
            context.reloadRequired();
        } else {
            final ServiceTarget target = context.getServiceTarget();
            if (model.hasDefined(JGROUPS_CLUSTER.getName())) {
                // nothing to do, in that case, the clustering.jgroups subsystem will have setup the stack
            } else if(model.hasDefined(RemoteTransportDefinition.SOCKET_BINDING.getName())) {
                if(serviceName == null) {
                    serviceName = MessagingServices.getActiveMQServiceName((String) null);
                }
                final GroupBindingService bindingService = new GroupBindingService();
                target.addService(GroupBindingService.getDiscoveryBaseServiceName(serviceName).append(name), bindingService)
                        .addDependency(SocketBinding.JBOSS_BINDING_NAME.append(model.get(SOCKET_BINDING).asString()), SocketBinding.class, bindingService.getBindingRef())
                        .install();
            }
        }
    }

    static void addDiscoveryGroupConfigs(final OperationContext context, final Configuration configuration, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.DISCOVERY_GROUP)) {
            Map<String, DiscoveryGroupConfiguration> configs = configuration.getDiscoveryGroupConfigurations();
            if (configs == null) {
                configs = new HashMap<>();
                configuration.setDiscoveryGroupConfigurations(configs);
            }
            for (Property prop : model.get(CommonAttributes.DISCOVERY_GROUP).asPropertyList()) {
                configs.put(prop.getName(), createDiscoveryGroupConfiguration(context, prop.getName(), prop.getValue()));

            }
        }
    }

    static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final long refreshTimeout = DiscoveryGroupDefinition.REFRESH_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final long initialWaitTimeout = DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.resolveModelAttribute(context, model).asLong();

        return new DiscoveryGroupConfiguration()
                .setName(name)
                .setRefreshTimeout(refreshTimeout)
                .setDiscoveryInitialWaitTimeout(initialWaitTimeout);
    }

   public static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final String name, final DiscoveryGroupConfiguration config, final SocketBinding socketBinding) throws Exception {

        final String localAddress = socketBinding.getAddress().getHostAddress();
        final String groupAddress = socketBinding.getMulticastAddress().getHostAddress();
        final int groupPort = socketBinding.getMulticastPort();
        final long refreshTimeout = config.getRefreshTimeout();
        final long initialWaitTimeout = config.getDiscoveryInitialWaitTimeout();

        final BroadcastEndpointFactory endpointFactory = new UDPBroadcastEndpointFactory()
                .setGroupAddress(groupAddress)
                .setGroupPort(groupPort)
                .setLocalBindAddress(localAddress)
                .setLocalBindPort(-1);

        return new DiscoveryGroupConfiguration()
                .setName(name)
                .setRefreshTimeout(refreshTimeout)
                .setDiscoveryInitialWaitTimeout(initialWaitTimeout)
                .setBroadcastEndpointFactory(endpointFactory);
    }


   public static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final String name, final DiscoveryGroupConfiguration config, final CommandDispatcherFactory commandDispatcherFactory, final String channelName) throws Exception {
        final long refreshTimeout = config.getRefreshTimeout();
        final long initialWaitTimeout = config.getDiscoveryInitialWaitTimeout();

        final BroadcastEndpointFactory endpointFactory = new CommandDispatcherBroadcastEndpointFactory(commandDispatcherFactory, channelName);

        return new DiscoveryGroupConfiguration()
                .setName(name)
                .setRefreshTimeout(refreshTimeout)
                .setDiscoveryInitialWaitTimeout(initialWaitTimeout)
                .setBroadcastEndpointFactory(endpointFactory);
    }

}
