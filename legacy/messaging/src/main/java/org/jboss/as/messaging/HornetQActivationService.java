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

package org.jboss.as.messaging;

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A service that can be dependent on to ensure the HornetQ server is active.
 *
 * HornetQ server can be passive when they are configured as backup and wait for a live node to fail.
 * In this passive state, the server does not accept connections or create resources.
 *
 * This service must be used by any service depending on a HornetQ server being active.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class HornetQActivationService implements Service<Void> {

    public static ServiceName getHornetQActivationServiceName(ServiceName hqServerName) {
        return hqServerName.append("activation");
    }

    public static boolean isHornetQServerActive(OperationContext context, ModelNode operation) {
        PathAddress address = pathAddress(operation.get(OP_ADDR));
        return isHornetQServerActive(context.getServiceRegistry(false), MessagingServices.getHornetQServiceName(address));
    }

    public static boolean isHornetQServerActive(ServiceRegistry serviceRegistry, ServiceName hqServiceName) {
        ServiceController<?> service = serviceRegistry.getService(hqServiceName);
        if (service != null) {
            HornetQServer server = HornetQServer.class.cast(service.getValue());
            if (server.isStarted() && server.isActive()) {
                return true;
            }
        }
        return false;
    }

    public static boolean rollbackOperationIfServerNotActive(OperationContext context, ModelNode operation) {
        if (isHornetQServerActive(context, operation)) {
            return false;
        }
        context.getFailureDescription().set(MessagingLogger.ROOT_LOGGER.hqServerInBackupMode(pathAddress(operation.require(OP_ADDR))));
        context.setRollbackOnly();
        context.stepCompleted();
        return true;
    }

    public static boolean ignoreOperationIfServerNotActive(OperationContext context, ModelNode operation) {
        if (isHornetQServerActive(context, operation)) {
            return false;
        }
        // undefined result
        context.getResult();
        context.stepCompleted();
        return true;
    }

    static HornetQServer getHornetQServer(final OperationContext context, ModelNode operation) {
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(pathAddress(operation.get(OP_ADDR)));
        final ServiceController<?> controller = context.getServiceRegistry(false).getService(hqServiceName);
        if(controller != null) {
            return HornetQServer.class.cast(controller.getValue());
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
