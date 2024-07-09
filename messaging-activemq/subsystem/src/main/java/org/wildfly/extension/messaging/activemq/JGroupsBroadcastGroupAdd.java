/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.BroadcastGroupDefinition.CONNECTOR_REFS;
import static org.wildfly.extension.messaging.activemq.BroadcastGroupDefinition.JGROUPS_CHANNEL;
import static org.wildfly.extension.messaging.activemq.BroadcastGroupDefinition.JGROUPS_CHANNEL_FACTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.activemq.artemis.api.core.BroadcastEndpointFactory;
import org.apache.activemq.artemis.api.core.BroadcastGroupConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredAddStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.extension.messaging.activemq.broadcast.BroadcastCommandDispatcherFactory;
import org.wildfly.extension.messaging.activemq.broadcast.CommandDispatcherBroadcastEndpointFactory;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Handler for adding a broadcast group using JGroups.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JGroupsBroadcastGroupAdd extends ReloadRequiredAddStepHandler {

    public static final JGroupsBroadcastGroupAdd INSTANCE = new JGroupsBroadcastGroupAdd(true);
    public static final JGroupsBroadcastGroupAdd LEGACY_INSTANCE = new JGroupsBroadcastGroupAdd(false);

    private final boolean needLegacyCall;
    private JGroupsBroadcastGroupAdd(boolean needLegacyCall) {
        super(JGroupsBroadcastGroupDefinition.ATTRIBUTES);
        this.needLegacyCall= needLegacyCall;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        CommonAttributes.renameChannelToCluster(operation);
        if (operation.hasDefined(JGROUPS_CLUSTER.getName())
                && operation.hasDefined(JGROUPS_CHANNEL_FACTORY.getName())
                && !operation.hasDefined(JGROUPS_CHANNEL.getName())) {
            // Handle legacy behavior
            String channel = operation.get(JGROUPS_CLUSTER.getName()).asString();
            operation.get(JGROUPS_CHANNEL.getName()).set(channel);

            PathAddress channelAddress = context.getCurrentAddress().getParent().getParent().getParent()
                    .append(ModelDescriptionConstants.SUBSYSTEM, "jgroups").append("channel", channel);
            ModelNode addChannelOperation = Util.createAddOperation(channelAddress);
            addChannelOperation.get("stack").set(operation.get(JGROUPS_CHANNEL_FACTORY.getName()));
            // Fabricate a channel resource if it is missing
            context.addStep(addChannelOperation, AddIfAbsentStepHandler.INSTANCE, OperationContext.Stage.MODEL);
        }
        if(needLegacyCall) {
            PathAddress target = context.getCurrentAddress().getParent().append(CommonAttributes.BROADCAST_GROUP, context.getCurrentAddressValue());
            ModelNode op = operation.clone();
            op.get(OP_ADDR).set(target.toModelNode());
            context.addStep(op, BroadcastGroupAdd.LEGACY_INSTANCE, OperationContext.Stage.MODEL, true);
        }
        super.execute(context, operation);
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        super.populateModel(context, operation, resource);

        final ModelNode connectorRefs = resource.getModel().require(CONNECTOR_REFS.getName());
        if (connectorRefs.isDefined()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    BroadcastGroupWriteAttributeHandler.JGROUP_INSTANCE.validateConnectors(context, operation, connectorRefs);
                }
            }, OperationContext.Stage.MODEL);
        }
    }

    static void addBroadcastGroupConfigs(final OperationContext context, final List<BroadcastGroupConfiguration> configs, final Set<String> connectors, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.JGROUPS_BROADCAST_GROUP)) {
            for (Property prop : model.get(CommonAttributes.JGROUPS_BROADCAST_GROUP).asPropertyList()) {
                configs.add(createBroadcastGroupConfiguration(context, connectors, prop.getName(), prop.getValue()));
            }
        }
    }

    static BroadcastGroupConfiguration createBroadcastGroupConfiguration(final OperationContext context, final Set<String> connectors, final String name, final ModelNode model) throws OperationFailedException {
        final long broadcastPeriod = BroadcastGroupDefinition.BROADCAST_PERIOD.resolveModelAttribute(context, model).asLong();
        final List<String> connectorRefs = new ArrayList<>();
        if (model.hasDefined(CommonAttributes.CONNECTORS)) {
            for (ModelNode ref : model.get(CommonAttributes.CONNECTORS).asList()) {
                final String refName = ref.asString();
                if(!connectors.contains(refName)){
                    throw MessagingLogger.ROOT_LOGGER.wrongConnectorRefInBroadCastGroup(name, refName, connectors);
                }
                connectorRefs.add(refName);
            }
        }

        return new BroadcastGroupConfiguration()
                .setName(name)
                .setBroadcastPeriod(broadcastPeriod)
                .setConnectorInfos(connectorRefs);
    }

    static BroadcastGroupConfiguration createBroadcastGroupConfiguration(final String name, final BroadcastGroupConfiguration config, final BroadcastCommandDispatcherFactory commandDispatcherFactory, final String channelName) throws Exception {

        final long broadcastPeriod = config.getBroadcastPeriod();
        final List<String> connectorRefs = config.getConnectorInfos();

        final BroadcastEndpointFactory endpointFactory = new CommandDispatcherBroadcastEndpointFactory(commandDispatcherFactory, channelName);

        return new BroadcastGroupConfiguration()
                .setName(name)
                .setBroadcastPeriod(broadcastPeriod)
                .setConnectorInfos(connectorRefs)
                .setEndpointFactory(endpointFactory);
    }
}