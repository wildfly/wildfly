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

import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Removes a bridge.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BridgeRemove extends AbstractRemoveStepHandler {

    public static final BridgeRemove INSTANCE = new BridgeRemove();

    private BridgeRemove() {
        super();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceController<?> service = registry.getService(serviceName);
        if (service != null && service.getState() == ServiceController.State.UP) {

            ActiveMQServer server = ActiveMQServer.class.cast(service.getValue());
            try {
                server.getActiveMQServerControl().destroyBridge(name);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // TODO should this be an OFE instead?
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceController<?> service = registry.getService(serviceName);
        if (service != null && service.getState() == ServiceController.State.UP) {

            final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
            final BridgeConfiguration bridgeConfiguration = BridgeAdd.createBridgeConfiguration(context, name, model);
            ActiveMQServer server = ActiveMQServer.class.cast(service.getValue());
            BridgeAdd.createBridge(name, bridgeConfiguration, server.getActiveMQServerControl());
        }
    }
}
