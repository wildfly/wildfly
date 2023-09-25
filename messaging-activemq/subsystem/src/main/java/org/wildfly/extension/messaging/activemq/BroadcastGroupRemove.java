/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Removes a broadcast group.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BroadcastGroupRemove extends AbstractRemoveStepHandler {

    public static final BroadcastGroupRemove INSTANCE = new BroadcastGroupRemove(false);
    public static final BroadcastGroupRemove LEGACY_INSTANCE = new BroadcastGroupRemove(true);

    private final boolean isLegacyCall;

    private BroadcastGroupRemove(boolean isLegacyCall) {
        super();
        this.isLegacyCall= isLegacyCall;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (!isLegacyCall) {
            ModelNode op = operation.clone();
            PathAddress target = context.getCurrentAddress().getParent();
            OperationStepHandler removeHandler;
            try {
                context.readResourceFromRoot(target.append(CommonAttributes.JGROUPS_BROADCAST_GROUP, context.getCurrentAddressValue()), false);
                target = target.append(CommonAttributes.JGROUPS_BROADCAST_GROUP, context.getCurrentAddressValue());
                removeHandler = JGroupsBroadcastGroupRemove.LEGACY_INSTANCE;
            } catch(Resource.NoSuchResourceException ex) {
                target = target.append(CommonAttributes.SOCKET_BROADCAST_GROUP, context.getCurrentAddressValue());
                removeHandler = SocketBroadcastGroupRemove.LEGACY_INSTANCE;
            }
            op.get(OP_ADDR).set(target.toModelNode());
            context.addStep(op, removeHandler, OperationContext.Stage.MODEL, true);
        }
        super.execute(context, operation);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.reloadRequired();
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.revertReloadRequired();
    }
}