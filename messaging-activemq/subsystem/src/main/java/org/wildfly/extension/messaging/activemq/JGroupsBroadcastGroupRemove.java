/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.dmr.ModelNode;

/**
 * Removes a broadcast group using JGroups.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JGroupsBroadcastGroupRemove extends ReloadRequiredRemoveStepHandler {

    public static final JGroupsBroadcastGroupRemove INSTANCE = new JGroupsBroadcastGroupRemove(true);
    public static final JGroupsBroadcastGroupRemove LEGACY_INSTANCE = new JGroupsBroadcastGroupRemove(false);

    private final boolean needLegacyCall;

    private JGroupsBroadcastGroupRemove(boolean needLegacyCall) {
        super();
        this.needLegacyCall = needLegacyCall;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if(needLegacyCall) {
            PathAddress target = context.getCurrentAddress().getParent().append(CommonAttributes.BROADCAST_GROUP, context.getCurrentAddressValue());
            ModelNode op = operation.clone();
            op.get(OP_ADDR).set(target.toModelNode());
            context.addStep(op, BroadcastGroupRemove.LEGACY_INSTANCE, OperationContext.Stage.MODEL, true);
        }
        super.execute(context, operation);
    }
}