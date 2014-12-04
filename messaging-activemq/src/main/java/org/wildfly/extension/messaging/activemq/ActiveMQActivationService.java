/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A service that can be dependent on to ensure the ActiveMQ server is active.
 *
 * ActiveMQ servers can be passive when they are configured as backup and wait for a live node to fail.
 * In this passive state, the server does not accept connections or create resources.
 *
 * This service must be used by any service depending on an ActiveMQ server being active.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class ActiveMQActivationService implements Service<Void> {

    public static ServiceName getServiceName(ServiceName serverName) {
        return serverName.append("activation");
    }

    public static boolean isActiveMQServerActive(OperationContext context, ModelNode operation) {
        PathAddress address = pathAddress(operation.get(OP_ADDR));
        return isActiveMQServerActive(context.getServiceRegistry(false), MessagingServices.getActiveMQServiceName(address));
    }

    public static boolean isActiveMQServerActive(ServiceRegistry serviceRegistry, ServiceName activeMQServerServiceName) {
        ServiceController<?> service = serviceRegistry.getService(activeMQServerServiceName);
        if (service != null) {
            ActiveMQServer server = ActiveMQServer.class.cast(service.getValue());
            if (server.isStarted() && server.isActive()) {
                return true;
            }
        }
        return false;
    }

    public static boolean rollbackOperationIfServerNotActive(OperationContext context, ModelNode operation) {
        if (isActiveMQServerActive(context, operation)) {
            return false;
        }
        context.getFailureDescription().set(MessagingLogger.ROOT_LOGGER.serverInBackupMode(pathAddress(operation.require(OP_ADDR))));
        context.setRollbackOnly();
        return true;
    }

    public static boolean ignoreOperationIfServerNotActive(OperationContext context, ModelNode operation) {
        if (isActiveMQServerActive(context, operation)) {
            return false;
        }
        // undefined result
        context.getResult();
        return true;
    }

    static ActiveMQServer getActiveMQServer(final OperationContext context, ModelNode operation) {
        final ServiceName activMQServerServiceName = MessagingServices.getActiveMQServiceName(pathAddress(operation.get(OP_ADDR)));
        final ServiceController<?> controller = context.getServiceRegistry(false).getService(activMQServerServiceName);
        if(controller != null) {
            return ActiveMQServer.class.cast(controller.getValue());
        }
        return null;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public Void getValue() throws IllegalStateException, IllegalArgumentException {
        return null;
    }
}
