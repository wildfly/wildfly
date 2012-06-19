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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP;
import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.CommonAttributes.PAUSED;
import static org.jboss.as.messaging.CommonAttributes.STARTED;
import static org.jboss.as.messaging.MessagingLogger.ROOT_LOGGER;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;
import static org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition.PAUSE;
import static org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition.RESUME;

import org.hornetq.jms.bridge.JMSBridge;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JMSBridgeHandler extends AbstractRuntimeOnlyHandler {

    public static final JMSBridgeHandler INSTANCE = new JMSBridgeHandler();

    private ParametersValidator readAttributeValidator = new ParametersValidator();

    private JMSBridgeHandler() {
        readAttributeValidator.registerValidator(NAME, new StringLengthValidator(1));
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String bridgeName = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement()
                .getValue();
        final String operationName = operation.require(OP).asString();

        final ServiceName bridgeServiceName = MessagingServices.getJMSBridgeServiceName(bridgeName);
        ServiceController<?> bridgeService = context.getServiceRegistry(true).getService(bridgeServiceName);
        if (bridgeService == null) {
            throw new OperationFailedException(ControllerMessages.MESSAGES.noHandler(READ_ATTRIBUTE_OPERATION, PathAddress.pathAddress(operation.require(OP_ADDR))));
        }

        JMSBridge bridge = JMSBridge.class.cast(bridgeService.getValue());

        if (READ_ATTRIBUTE_OPERATION.equals(operationName)) {
            readAttributeValidator.validate(operation);
            final String name = operation.require(NAME).asString();
            if (STARTED.equals(name)) {
                context.getResult().set(bridge.isStarted());
            } else if (PAUSED.equals(name)) {
                context.getResult().set(bridge.isPaused());
            } else {
                throw MESSAGES.unsupportedAttribute(name);
            }
        }
        else if (START.equals(operationName)) {
            try {
                // we do not start the bridge directly but call startBridge() instead
                // to ensure the class loader will be able to load any external resources
                JMSBridgeService service = (JMSBridgeService) bridgeService.getService();
                service.startBridge();
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }
        } else if (STOP.equals(operationName)) {
            try {
                bridge.stop();
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }
        } else if (PAUSE.equals(operationName)) {
            try {
                bridge.pause();
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }
        } else if (RESUME.equals(operationName)) {
            try {
                bridge.resume();
            } catch (Exception e) {
                context.getFailureDescription().set(e.getLocalizedMessage());
            }
        } else {
            throw MESSAGES.unsupportedOperation(operationName);
        }

        if (context.completeStep() != OperationContext.ResultAction.KEEP) {
            try {
                if (START.equals(operationName)) {
                    bridge.stop();
                } else if (STOP.equals(operationName)) {
                    JMSBridgeService service = (JMSBridgeService) bridgeService.getService();
                    service.startBridge();
                } else if (PAUSE.equals(operationName)) {
                    bridge.resume();
                } else if (RESUME.equals(operationName)) {
                    bridge.pause();
                }
            } catch (Exception e) {
                ROOT_LOGGER.revertOperationFailed(e, getClass().getSimpleName(), operation
                        .require(ModelDescriptionConstants.OP).asString(), PathAddress.pathAddress(operation
                                .require(ModelDescriptionConstants.OP_ADDR)));
            }
        }
    }
}
