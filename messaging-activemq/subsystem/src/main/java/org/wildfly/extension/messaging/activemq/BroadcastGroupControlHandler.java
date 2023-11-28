/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.BroadcastGroupDefinition.GET_CONNECTOR_PAIRS_AS_JSON;

import org.apache.activemq.artemis.api.core.management.BaseBroadcastGroupControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Handler for runtime operations that interact with a ActiveMQ {@link org.apache.activemq.artemis.api.core.management.BroadcastGroupControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BroadcastGroupControlHandler extends AbstractActiveMQComponentControlHandler<BaseBroadcastGroupControl> {

    public static final BroadcastGroupControlHandler INSTANCE = new BroadcastGroupControlHandler();

    private BroadcastGroupControlHandler() {
    }

    @Override
    protected BaseBroadcastGroupControl getActiveMQComponentControl(ActiveMQBroker activeMQBroker, PathAddress address) {
        final String resourceName = address.getLastElement().getValue();
        return BaseBroadcastGroupControl.class.cast(activeMQBroker.getResource(ResourceNames.BROADCAST_GROUP + resourceName));
    }

    @Override
    protected String getDescriptionPrefix() {
        return CommonAttributes.BROADCAST_GROUP;
    }

    @Override
    protected Object handleOperation(String operationName, OperationContext context, ModelNode operation) throws OperationFailedException {
        if (GET_CONNECTOR_PAIRS_AS_JSON.equals(operationName)) {
            BaseBroadcastGroupControl control = getActiveMQComponentControl(context, operation, false);
            try {
                if(control != null) {
                    context.getResult().set(control.getConnectorPairsAsJSON());
                }
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }
        } else {
            unsupportedOperation(operationName);
        }

        return null;
    }
}