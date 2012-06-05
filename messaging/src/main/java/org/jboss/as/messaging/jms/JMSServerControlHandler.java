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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.messaging.ManagementUtil.rollbackOperationWithNoHandler;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.Locale;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.JMSServerControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.MessagingDescriptions;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Handles operations and attribute reads supported by a HornetQ {@link org.hornetq.api.jms.management.JMSServerControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSServerControlHandler extends AbstractRuntimeOnlyHandler {

    public static JMSServerControlHandler INSTANCE = new JMSServerControlHandler();

    public static final String LIST_CONNECTIONS_AS_JSON = "list-connections-as-json";
    public static final String LIST_CONSUMERS_AS_JSON = "list-consumers-as-json";
    public static final String LIST_ALL_CONSUMERS_AS_JSON = "list-all-consumers-as-json";
    public static final String LIST_TARGET_DESTINATIONS = "list-target-destinations";
    public static final String GET_LAST_SENT_MESSAGE_ID = "get-last-sent-message-id";
    public static final String GET_SESSION_CREATION_TIME = "get-session-creation-time";
    public static final String LIST_SESSIONS_AS_JSON = "list-sessions-as-json";
    public static final String LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_JSON = "list-prepared-transaction-jms-details-as-json";
    public static final String LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_HTML = "list-prepared-transaction-jms-details-as-html";
    // already implement in hqservercontrolhandler
    // isStarted, getVersion, listRemoteAddresses, closeConnectionsForAddress, listConnectionIDs, listSessions,
    // JMS-only
    // listConnectionsAsJSON, listConsumersAsJSON, listAllConsumersAsJSON, listTargetDestinations, getLastSentMessageID,
    // getSessionCreationTime, listSessionsAsJSON, listPreparedTransactionDetailsAsJSON, listPreparedTransactionDetailsAsHTML

    public static final String JMS_SERVER = "jms-server";
    public static final String ADDRESS_NAME = "address-name";
    public static final String CONNECTION_ID = "connection-id";
    public static final String SESSION_ID = "session-id";

    private final ParametersValidator connectionIdValidator = new ParametersValidator();
    private final ParametersValidator sessionIdValidator = new ParametersValidator();
    private final ParametersValidator lastSentValidator = new ParametersValidator();

    private JMSServerControlHandler() {
        final StringLengthValidator stringLengthValidator = new StringLengthValidator(1);
        connectionIdValidator.registerValidator(CONNECTION_ID, stringLengthValidator);
        sessionIdValidator.registerValidator(SESSION_ID, stringLengthValidator);
        lastSentValidator.registerValidator(SESSION_ID, stringLengthValidator);
        lastSentValidator.registerValidator(ADDRESS_NAME, stringLengthValidator);
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String operationName = operation.require(OP).asString();
        final JMSServerControl serverControl = getServerControl(context, operation);
        if (serverControl == null) {
            rollbackOperationWithNoHandler(context, operation);
            return;
        }

        try {
            if (LIST_CONNECTIONS_AS_JSON.equals(operationName)) {
                String json = serverControl.listConnectionsAsJSON();
                context.getResult().set(json);
            } else if (LIST_CONSUMERS_AS_JSON.equals(operationName)) {
                connectionIdValidator.validate(operation);
                String connectionID = operation.require(CONNECTION_ID).asString();
                String json = serverControl.listConsumersAsJSON(connectionID);
                context.getResult().set(json);
            } else if (LIST_ALL_CONSUMERS_AS_JSON.equals(operationName)) {
                String json = serverControl.listAllConsumersAsJSON();
                context.getResult().set(json);
            } else if (LIST_TARGET_DESTINATIONS.equals(operationName)) {
                sessionIdValidator.validate(operation);
                String sessionID = operation.require(SESSION_ID).asString();
                String[] list = serverControl.listTargetDestinations(sessionID);
                reportListOfString(context, list);
            } else if (GET_LAST_SENT_MESSAGE_ID.equals(operationName)) {
                lastSentValidator.validate(operation);
                String sessionID = operation.require(SESSION_ID).asString();
                String addressName = operation.require(ADDRESS_NAME).asString();
                String msgId = serverControl.getLastSentMessageID(sessionID, addressName);
                context.getResult().set(msgId);
            } else if (GET_SESSION_CREATION_TIME.equals(operationName)) {
                sessionIdValidator.validate(operation);
                String sessionID = operation.require(SESSION_ID).asString();
                String time = serverControl.getSessionCreationTime(sessionID);
                context.getResult().set(time);
            } else if (LIST_SESSIONS_AS_JSON.equals(operationName)) {
                connectionIdValidator.validate(operation);
                String connectionID = operation.require(CONNECTION_ID).asString();
                String json = serverControl.listSessionsAsJSON(connectionID);
                context.getResult().set(json);
            } else if (LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_JSON.equals(operationName)) {
                String json = serverControl.listPreparedTransactionDetailsAsJSON();
                context.getResult().set(json);
            } else if (LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_HTML.equals(operationName)) {
                String html = serverControl.listPreparedTransactionDetailsAsHTML();
                context.getResult().set(html);
            } else {
                // Bug
                throw MESSAGES.unsupportedOperation(operationName);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }

        context.completeStep();
    }

    public void registerOperations(final ManagementResourceRegistration registry) {

        registry.registerOperationHandler(LIST_CONNECTIONS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_CONNECTIONS_AS_JSON, JMS_SERVER, ModelType.STRING, true);
            }
        });

        registry.registerOperationHandler(LIST_CONSUMERS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleReplyOperation(locale, LIST_CONSUMERS_AS_JSON, JMS_SERVER,
                        CONNECTION_ID, ModelType.STRING, false, ModelType.STRING, true);
            }
        });

        registry.registerOperationHandler(LIST_ALL_CONSUMERS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_ALL_CONSUMERS_AS_JSON, JMS_SERVER, ModelType.STRING, true);
            }
        });

        registry.registerOperationHandler(LIST_TARGET_DESTINATIONS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleListReplyOperation(locale, LIST_TARGET_DESTINATIONS,
                        JMS_SERVER, SESSION_ID, ModelType.STRING, false, ModelType.STRING, true);
            }
        });

        registry.registerOperationHandler(GET_LAST_SENT_MESSAGE_ID, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getGetLastSentMessageId(locale);
            }
        });

        registry.registerOperationHandler(GET_SESSION_CREATION_TIME, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleReplyOperation(locale, GET_SESSION_CREATION_TIME, JMS_SERVER,
                        SESSION_ID, ModelType.STRING, false, ModelType.STRING, true);
            }
        });

        registry.registerOperationHandler(LIST_SESSIONS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleReplyOperation(locale, LIST_SESSIONS_AS_JSON, JMS_SERVER,
                        CONNECTION_ID, ModelType.STRING, false, ModelType.STRING, true);
            }
        });

        registry.registerOperationHandler(LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_JSON, JMS_SERVER, ModelType.STRING, true);
            }
        });

        registry.registerOperationHandler(LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_HTML, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_HTML, JMS_SERVER, ModelType.STRING, true);
            }
        });
    }

    private JMSServerControl getServerControl(final OperationContext context, final ModelNode operation) {
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        return JMSServerControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_SERVER));
    }

    private void reportListOfString(OperationContext context, String[] list) {
        final ModelNode result = context.getResult();
        result.setEmptyList();
        for (String tx : list) {
            result.add(tx);
        }
    }
}
