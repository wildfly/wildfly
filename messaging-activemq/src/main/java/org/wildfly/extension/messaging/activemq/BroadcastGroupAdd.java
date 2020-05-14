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
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;

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
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
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
            } else {
                target = target.append(CommonAttributes.SOCKET_BROADCAST_GROUP, context.getCurrentAddressValue());
                addHandler = SocketBroadcastGroupAdd.LEGACY_INSTANCE;
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