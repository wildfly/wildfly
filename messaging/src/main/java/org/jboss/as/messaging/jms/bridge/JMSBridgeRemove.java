/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms.bridge;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;

/**
 * Removes a JMS Bridge.
 *
 * @author Jeff Mesnil (c) 2011 Red Hat Inc.
 */
public class JMSBridgeRemove extends AbstractRemoveStepHandler {

    public static final String OPERATION_NAME = REMOVE;

    public static JMSBridgeRemove INSTANCE = new JMSBridgeRemove();

    private JMSBridgeRemove() {
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String bridgeName = address.getLastElement().getValue();
        context.removeService(MessagingServices.getJMSBridgeServiceName(bridgeName));
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
    }
}
