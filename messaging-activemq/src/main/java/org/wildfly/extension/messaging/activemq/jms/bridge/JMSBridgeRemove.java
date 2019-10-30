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

package org.wildfly.extension.messaging.activemq.jms.bridge;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Removes a JMS Bridge.
 *
 * @author Jeff Mesnil (c) 2011 Red Hat Inc.
 */
class JMSBridgeRemove extends AbstractRemoveStepHandler {

    static final JMSBridgeRemove INSTANCE = new JMSBridgeRemove();

    private JMSBridgeRemove() {
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String bridgeName = address.getLastElement().getValue();
        context.removeService(MessagingServices.getJMSBridgeServiceName(bridgeName));
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String bridgeName = address.getLastElement().getValue();

        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName jmsBridgeServiceName = MessagingServices.getJMSBridgeServiceName(bridgeName);
        final ServiceController<?> jmsBridgeServiceController = registry.getService(jmsBridgeServiceName);
        if (jmsBridgeServiceController != null && jmsBridgeServiceController.getState() == ServiceController.State.UP) {
            JMSBridgeService jmsBridgeService = (JMSBridgeService) jmsBridgeServiceController.getService();
            try {
                jmsBridgeService.startBridge();
            } catch (Exception e) {
                throw MessagingLogger.ROOT_LOGGER.failedToRecover(e, bridgeName);
            }
        }
    }
}
