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
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Removes a discovery group using JGroups.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JGroupsDiscoveryGroupRemove extends ReloadRequiredRemoveStepHandler {

    public static final JGroupsDiscoveryGroupRemove INSTANCE = new JGroupsDiscoveryGroupRemove(true);
    public static final JGroupsDiscoveryGroupRemove LEGACY_INSTANCE = new JGroupsDiscoveryGroupRemove(false);

    private final boolean needLegacyCall;

    private JGroupsDiscoveryGroupRemove(boolean needLegacyCall) {
        super();
        this.needLegacyCall = needLegacyCall;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if(needLegacyCall) {
            PathAddress target = context.getCurrentAddress().getParent().append(CommonAttributes.DISCOVERY_GROUP, context.getCurrentAddressValue());
            try {
                context.readResourceFromRoot(target);
                ModelNode op = operation.clone();
                op.get(OP_ADDR).set(target.toModelNode());
                // Fabricate a channel resource if it is missing
                context.addStep(op, DiscoveryGroupRemove.LEGACY_INSTANCE, OperationContext.Stage.MODEL, true);
            } catch( Resource.NoSuchResourceException ex) {
                // Legacy resource doesn't exist
            }
        }
        super.execute(context, operation);
    }
}