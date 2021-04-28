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

/**
 *
 */
package org.wildfly.extension.messaging.activemq;


import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

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
                ((ActiveMQServer) serverService.getValue()).destroyQueue(new SimpleString(name), null, false);
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
