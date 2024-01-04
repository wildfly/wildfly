/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;
import org.wildfly.extension.messaging.activemq.shallow.ShallowResourceAdd;

/**
 * Handler for adding a discovery group.
 * This is now a ShallowResourceAdd.
 *
 * @deprecated please use Jgroups DiscoveryGroupAdd or Socket DiscoveryGroupAdd
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DiscoveryGroupAdd extends ShallowResourceAdd {

    public static final DiscoveryGroupAdd INSTANCE = new DiscoveryGroupAdd(false);
    public static final DiscoveryGroupAdd LEGACY_INSTANCE = new DiscoveryGroupAdd(true);

    private final boolean isLegacyCall;

    private DiscoveryGroupAdd(boolean isLegacyCall) {
        super(DiscoveryGroupDefinition.ATTRIBUTES);
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
                target = target.append(CommonAttributes.JGROUPS_DISCOVERY_GROUP, context.getCurrentAddressValue());
                addHandler = JGroupsDiscoveryGroupAdd.LEGACY_INSTANCE;
            } else if (operation.hasDefined(CommonAttributes.SOCKET_BINDING.getName())) {
                target = target.append(CommonAttributes.SOCKET_DISCOVERY_GROUP, context.getCurrentAddressValue());
                addHandler = SocketDiscoveryGroupAdd.LEGACY_INSTANCE;
            } else {
                throw MessagingLogger.ROOT_LOGGER.socketBindingOrJGroupsClusterRequired();
            }
            op.get(OP_ADDR).set(target.toModelNode());
            context.addStep(op, addHandler, OperationContext.Stage.MODEL, true);
        }
        super.execute(context, operation);
    }
}
