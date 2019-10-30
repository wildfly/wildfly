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

import static org.wildfly.extension.messaging.activemq.BroadcastGroupDefinition.GET_CONNECTOR_PAIRS_AS_JSON;

import org.apache.activemq.artemis.api.core.management.BroadcastGroupControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Handler for runtime operations that interact with a ActiveMQ {@link org.apache.activemq.artemis.api.core.management.BroadcastGroupControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BroadcastGroupControlHandler extends AbstractActiveMQComponentControlHandler<BroadcastGroupControl> {

    public static final BroadcastGroupControlHandler INSTANCE = new BroadcastGroupControlHandler();

    private BroadcastGroupControlHandler() {
    }

    @Override
    protected BroadcastGroupControl getActiveMQComponentControl(ActiveMQServer activeMQServer, PathAddress address) {
        final String resourceName = address.getLastElement().getValue();
        return BroadcastGroupControl.class.cast(activeMQServer.getManagementService().getResource(ResourceNames.BROADCAST_GROUP + resourceName));
    }

    @Override
    protected String getDescriptionPrefix() {
        return CommonAttributes.BROADCAST_GROUP;
    }

    @Override
    protected Object handleOperation(String operationName, OperationContext context, ModelNode operation) throws OperationFailedException {
        if (GET_CONNECTOR_PAIRS_AS_JSON.equals(operationName)) {
            BroadcastGroupControl control = getActiveMQComponentControl(context, operation, false);
            try {
                context.getResult().set(control.getConnectorPairsAsJSON());
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }
        } else {
            unsupportedOperation(operationName);
        }

        return null;
    }
}