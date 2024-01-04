/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms.bridge;


import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Removes a Jakarta Messaging Bridge.
 *
 * @author Jeff Mesnil (c) 2011 Red Hat Inc.
 */
class JMSBridgeRemove extends AbstractRemoveStepHandler {

    static final JMSBridgeRemove INSTANCE = new JMSBridgeRemove();

    private JMSBridgeRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        context.removeService(MessagingServices.getJMSBridgeServiceName(context.getCurrentAddressValue()));
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String bridgeName = context.getCurrentAddressValue();

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
