/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SOCKET_BINDING;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.activemq.artemis.api.core.BroadcastGroupConfiguration;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;
import org.wildfly.extension.messaging.activemq.shallow.ShallowResourceAdd;

/**
 * Handler for adding a broadcast group.
 * This is now a ShallowResourceAdd.
 *
 * @deprecated please use JgroupsBroadcastGroupAdd or SocketBroadcastGroupAdd
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BroadcastGroupAdd extends ShallowResourceAdd {

    public static final BroadcastGroupAdd INSTANCE = new BroadcastGroupAdd(false);
    public static final BroadcastGroupAdd LEGACY_INSTANCE = new BroadcastGroupAdd(true);

    private final boolean isLegacyCall;

    private BroadcastGroupAdd(boolean isLegacyCall) {
        super(BroadcastGroupDefinition.ATTRIBUTES);
        this.isLegacyCall = isLegacyCall;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
       CommonAttributes.renameChannelToCluster(operation);
        if (!isLegacyCall) {
            ModelNode op = operation.clone();
            PathAddress target = context.getCurrentAddress().getParent();
            OperationStepHandler addHandler;
            if (operation.hasDefined(JGROUPS_CLUSTER.getName())) {
                target = target.append(CommonAttributes.JGROUPS_BROADCAST_GROUP, context.getCurrentAddressValue());
                addHandler = JGroupsBroadcastGroupAdd.LEGACY_INSTANCE;
            } else if (operation.hasDefined(SOCKET_BINDING.getName())) {
                target = target.append(CommonAttributes.SOCKET_BROADCAST_GROUP, context.getCurrentAddressValue());
                addHandler = SocketBroadcastGroupAdd.LEGACY_INSTANCE;
            } else {
                throw MessagingLogger.ROOT_LOGGER.socketBindingOrJGroupsClusterRequired();
            }
            op.get(OP_ADDR).set(target.toModelNode());
            context.addStep(op, addHandler, OperationContext.Stage.MODEL, true);
        }
       super.execute(context, operation);
    }

    static void addBroadcastGroupConfigs(final OperationContext context, final List<BroadcastGroupConfiguration> configs, final Set<String> connectors, final ModelNode model)  throws OperationFailedException {
        if (model.hasDefined(CommonAttributes.JGROUPS_BROADCAST_GROUP)) {
            for (Property prop : model.get(CommonAttributes.JGROUPS_BROADCAST_GROUP).asPropertyList()) {
                configs.add(createBroadcastGroupConfiguration(context, connectors, prop.getName(), prop.getValue()));
            }
        }
        if (model.hasDefined(CommonAttributes.SOCKET_BROADCAST_GROUP)) {
            for (Property prop : model.get(CommonAttributes.SOCKET_BROADCAST_GROUP).asPropertyList()) {
                configs.add(createBroadcastGroupConfiguration(context, connectors, prop.getName(), prop.getValue()));
            }
        }
    }

    static BroadcastGroupConfiguration createBroadcastGroupConfiguration(final OperationContext context, final Set<String> connectors, final String name, final ModelNode model) throws OperationFailedException {
        final long broadcastPeriod = BroadcastGroupDefinition.BROADCAST_PERIOD.resolveModelAttribute(context, model).asLong();
        final List<String> connectorRefs = new ArrayList<String>();
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
}