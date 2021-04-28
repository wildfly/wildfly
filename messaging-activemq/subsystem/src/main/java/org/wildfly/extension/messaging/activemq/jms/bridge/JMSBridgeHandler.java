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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STOP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAUSED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.STARTED;
import static org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeDefinition.ABORTED_MESSAGE_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeDefinition.PAUSE;
import static org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeDefinition.RESUME;

import org.apache.activemq.artemis.jms.bridge.JMSBridge;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.CommonAttributes;

/**
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JMSBridgeHandler extends AbstractRuntimeOnlyHandler {

    public static final JMSBridgeHandler INSTANCE = new JMSBridgeHandler(false);
    public static final JMSBridgeHandler READ_ONLY_INSTANCE = new JMSBridgeHandler(true);

    private final ParametersValidator readAttributeValidator = new ParametersValidator();
    private final boolean readOnly;

    private JMSBridgeHandler(boolean readOnly) {
        this.readOnly = readOnly;
        readAttributeValidator.registerValidator(NAME, new StringLengthValidator(1));
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String bridgeName = context.getCurrentAddressValue();
        final String operationName = operation.require(OP).asString();
        if (null == operationName) {
            throw MessagingLogger.ROOT_LOGGER.unsupportedOperation(operationName);
        }
        final boolean modify = !READ_ATTRIBUTE_OPERATION.equals(operationName);
        final ServiceName bridgeServiceName = MessagingServices.getJMSBridgeServiceName(bridgeName);
        final ServiceController<?> bridgeService = context.getServiceRegistry(modify).getService(bridgeServiceName);
        if (bridgeService == null) {
            if (!readOnly) {
                throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(context.getCurrentAddress());
            }
            return;
        }
        final JMSBridge bridge = JMSBridge.class.cast(bridgeService.getValue());
        switch (operationName) {
            case READ_ATTRIBUTE_OPERATION:
                readAttributeValidator.validate(operation);
                final String name = operation.require(NAME).asString();
                if (STARTED.equals(name)) {
                    context.getResult().set(bridge.isStarted());
                } else if (PAUSED.getName().equals(name)) {
                    context.getResult().set(bridge.isPaused());
                }  else if (CommonAttributes.MESSAGE_COUNT.getName().equals(name)) {
                    context.getResult().set(bridge.getMessageCount());
                }  else if (ABORTED_MESSAGE_COUNT.getName().equals(name)) {
                    context.getResult().set(bridge.getAbortedMessageCount());
                } else {
                    throw MessagingLogger.ROOT_LOGGER.unsupportedAttribute(name);
                }
                break;
            case START:
                try {
                    // we do not start the bridge directly but call startBridge() instead
                    // to ensure the class loader will be able to load any external resources
                    JMSBridgeService service = (JMSBridgeService) bridgeService.getService();
                    service.startBridge();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }   break;
            case STOP:
                try {
                    bridge.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }   break;
            case PAUSE:
                try {
                    bridge.pause();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }   break;
            case RESUME:
                try {
                    bridge.resume();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw MessagingLogger.ROOT_LOGGER.unsupportedOperation(operationName);
        }

        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                try {
                    switch (operationName) {
                        case START:
                            bridge.stop();
                            break;
                        case STOP:
                            JMSBridgeService service = (JMSBridgeService) bridgeService.getService();
                            service.startBridge();
                            break;
                        case PAUSE:
                            bridge.resume();
                            break;
                        case RESUME:
                            bridge.pause();
                            break;
                    }
                } catch (Exception e) {
                    MessagingLogger.ROOT_LOGGER.revertOperationFailed(e, getClass().getSimpleName(), operationName, context.getCurrentAddress());
                }
            }
        });
    }
}
