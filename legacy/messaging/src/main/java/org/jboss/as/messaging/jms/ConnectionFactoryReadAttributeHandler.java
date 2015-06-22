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

package org.jboss.as.messaging.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.HA;
import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.HornetQActivationService.ignoreOperationIfServerNotActive;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.ConnectionFactoryControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Implements the {@code read-attribute} operation for runtime attributes exposed by a HornetQ
 * {@link org.hornetq.api.jms.management.ConnectionFactoryControl}.
 *
 * <strong>THIS SHOULD NOT INCLUDE ATTRIBUTES THAT ARE PART OF THE PERSISTENT CONFIGURATION. Those are
 * handled by {@link ConnectionFactoryWriteAttributeHandler}, since <i>all</i> persistent attributes
 * are writable (even those that require a restart to take effect in the runtime.)</strong>
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ConnectionFactoryReadAttributeHandler extends AbstractRuntimeOnlyHandler {

    public static final ConnectionFactoryReadAttributeHandler INSTANCE = new ConnectionFactoryReadAttributeHandler();

    private ParametersValidator validator = new ParametersValidator();

    private ConnectionFactoryReadAttributeHandler() {
        validator.registerValidator(NAME, new StringLengthValidator(1));
    }

    @Override
    public void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (ignoreOperationIfServerNotActive(context, operation)) {
            return;
        }

        validator.validate(operation);
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();

        String factoryName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        ConnectionFactoryControl control = ConnectionFactoryControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_CONNECTION_FACTORY + factoryName));

        if (control == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }

        if (HA.getName().equals(attributeName)) {
            context.getResult().set(control.isHA());
        } else if (ConnectionFactoryAttributes.Regular.FACTORY_TYPE.getName().equals(attributeName)) {
            context.getResult().set(control.getFactoryType());
        } else if (ConnectionFactoryAttributes.Regular.INITIAL_MESSAGE_PACKET_SIZE.getName().equals(attributeName)) {
            context.getResult().set(control.getInitialMessagePacketSize());
        } else {
            // Bug
            throw MessagingLogger.ROOT_LOGGER.unsupportedAttribute(attributeName);
        }
        context.stepCompleted();
    }
}
