/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 *
 */
package org.wildfly.extension.messaging.activemq;


import org.apache.activemq.artemis.api.core.SimpleString;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Removes a queue.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class QueueRemove extends AbstractRemoveStepHandler {

    static final QueueRemove INSTANCE = new QueueRemove();

    private QueueRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
        final String name = context.getCurrentAddressValue();
        final ServiceName queueServiceName = MessagingServices.getQueueBaseServiceName(serviceName).append(name);
        if (context.getServiceRegistry(false).getService(queueServiceName) != null) {
            context.removeService(queueServiceName);
        } else {
            ServiceController<?> serverService = context.getServiceRegistry(false).getService(serviceName);
            try {
                ((ActiveMQBroker) serverService.getValue()).destroyQueue(new SimpleString(name), null, false);
            } catch (Exception ex) {
                MessagingLogger.ROOT_LOGGER.failedToDestroy("queue", name);
            }
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }
}
