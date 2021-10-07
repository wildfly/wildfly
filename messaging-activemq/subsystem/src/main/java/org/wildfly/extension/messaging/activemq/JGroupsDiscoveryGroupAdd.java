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
import static org.wildfly.extension.messaging.activemq.JGroupsDiscoveryGroupDefinition.JGROUPS_CHANNEL;
import static org.wildfly.extension.messaging.activemq.JGroupsDiscoveryGroupDefinition.JGROUPS_CHANNEL_FACTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;

import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.messaging.activemq.broadcast.BroadcastCommandDispatcherFactory;
import org.wildfly.extension.messaging.activemq.broadcast.CommandDispatcherBroadcastEndpointFactory;

/**
 * Handler for adding a discovery group using JGroups.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JGroupsDiscoveryGroupAdd extends AbstractAddStepHandler {

    public static final JGroupsDiscoveryGroupAdd INSTANCE = new JGroupsDiscoveryGroupAdd(true);
    public static final JGroupsDiscoveryGroupAdd LEGACY_INSTANCE = new JGroupsDiscoveryGroupAdd(false);

    private final boolean needLegacyCall;

    private JGroupsDiscoveryGroupAdd(boolean needLegacyCall) {
        super(JGroupsDiscoveryGroupDefinition.ATTRIBUTES);
        this.needLegacyCall= needLegacyCall;
    }

    private static boolean isSubsystemResource(final OperationContext context) {
        return ModelDescriptionConstants.SUBSYSTEM.equals(context.getCurrentAddress().getParent().getLastElement().getKey());
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        CommonAttributes.renameChannelToCluster(operation);
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
        super.execute(context, operation);
        if(needLegacyCall) {
            PathAddress target = context.getCurrentAddress().getParent().append(CommonAttributes.DISCOVERY_GROUP, context.getCurrentAddressValue());
            ModelNode op = operation.clone();
            op.get(OP_ADDR).set(target.toModelNode());
            context.addStep(op, DiscoveryGroupAdd.LEGACY_INSTANCE, OperationContext.Stage.MODEL, true);
        }
    }


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        ServiceRegistry registry = context.getServiceRegistry(false);
        ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = serviceName == null ? null : registry.getService(serviceName);
        if (service != null) {
            context.reloadRequired();
        }
    }

    static Map<String, DiscoveryGroupConfiguration> addDiscoveryGroupConfigs(final OperationContext context, final ModelNode model)  throws OperationFailedException {
         Map<String, DiscoveryGroupConfiguration> configs = new HashMap<>();
        if (model.hasDefined(CommonAttributes.JGROUPS_DISCOVERY_GROUP)) {
            for (Property prop : model.get(CommonAttributes.JGROUPS_DISCOVERY_GROUP).asPropertyList()) {
                configs.put(prop.getName(), createDiscoveryGroupConfiguration(context, prop.getName(), prop.getValue()));
            }
        }
        return configs;
    }

    static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final OperationContext context, final String name, final ModelNode model) throws OperationFailedException {

        final long refreshTimeout = DiscoveryGroupDefinition.REFRESH_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final long initialWaitTimeout = DiscoveryGroupDefinition.INITIAL_WAIT_TIMEOUT.resolveModelAttribute(context, model).asLong();

        return new DiscoveryGroupConfiguration()
                .setName(name)
                .setRefreshTimeout(refreshTimeout)
                .setDiscoveryInitialWaitTimeout(initialWaitTimeout);
    }

   public static DiscoveryGroupConfiguration createDiscoveryGroupConfiguration(final String name, final DiscoveryGroupConfiguration config, final BroadcastCommandDispatcherFactory commandDispatcherFactory, final String channelName) throws Exception {
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
