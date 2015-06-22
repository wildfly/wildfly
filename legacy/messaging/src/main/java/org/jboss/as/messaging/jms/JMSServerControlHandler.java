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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.HornetQActivationService.rollbackOperationIfServerNotActive;
import static org.jboss.as.messaging.ManagementUtil.reportListOfStrings;
import static org.jboss.as.messaging.OperationDefinitionHelper.createNonEmptyStringAttribute;
import static org.jboss.as.messaging.OperationDefinitionHelper.runtimeReadOnlyOperation;
import static org.jboss.dmr.ModelType.LIST;
import static org.jboss.dmr.ModelType.STRING;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.JMSServerControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Handles operations and attribute reads supported by a HornetQ {@link org.hornetq.api.jms.management.JMSServerControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSServerControlHandler extends AbstractRuntimeOnlyHandler {

    private static final AttributeDefinition ADDRESS_NAME = createNonEmptyStringAttribute("address-name");
    private static final AttributeDefinition SESSION_ID = createNonEmptyStringAttribute("session-id");
    private static final AttributeDefinition CONNECTION_ID = createNonEmptyStringAttribute("connection-id");

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

    public static final JMSServerControlHandler INSTANCE = new JMSServerControlHandler();

    private JMSServerControlHandler() {
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (rollbackOperationIfServerNotActive(context, operation)) {
            return;
        }

        final String operationName = operation.require(OP).asString();
        final JMSServerControl serverControl = getServerControl(context, operation);
        if (serverControl == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }

        try {
            if (LIST_CONNECTIONS_AS_JSON.equals(operationName)) {
                String json = serverControl.listConnectionsAsJSON();
                context.getResult().set(json);
            } else if (LIST_CONSUMERS_AS_JSON.equals(operationName)) {
                String connectionID = CONNECTION_ID.resolveModelAttribute(context, operation).asString();
                String json = serverControl.listConsumersAsJSON(connectionID);
                context.getResult().set(json);
            } else if (LIST_ALL_CONSUMERS_AS_JSON.equals(operationName)) {
                String json = serverControl.listAllConsumersAsJSON();
                context.getResult().set(json);
            } else if (LIST_TARGET_DESTINATIONS.equals(operationName)) {
                String sessionID = SESSION_ID.resolveModelAttribute(context, operation).asString();
                String[] list = serverControl.listTargetDestinations(sessionID);
                reportListOfStrings(context, list);
            } else if (GET_LAST_SENT_MESSAGE_ID.equals(operationName)) {
                String sessionID = SESSION_ID.resolveModelAttribute(context, operation).asString();
                String addressName = ADDRESS_NAME.resolveModelAttribute(context, operation).asString();
                String msgId = serverControl.getLastSentMessageID(sessionID, addressName);
                context.getResult().set(msgId);
            } else if (GET_SESSION_CREATION_TIME.equals(operationName)) {
                String sessionID = SESSION_ID.resolveModelAttribute(context, operation).asString();
                String time = serverControl.getSessionCreationTime(sessionID);
               context.getResult().set(time);
            } else if (LIST_SESSIONS_AS_JSON.equals(operationName)) {
                String connectionID = CONNECTION_ID.resolveModelAttribute(context, operation).asString();
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
                throw MessagingLogger.ROOT_LOGGER.unsupportedOperation(operationName);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }

        context.stepCompleted();
    }

    public void registerOperations(final ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_CONNECTIONS_AS_JSON, resolver)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_CONSUMERS_AS_JSON, resolver)
                .setParameters(CONNECTION_ID)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_ALL_CONSUMERS_AS_JSON, resolver)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_TARGET_DESTINATIONS, resolver)
                .setParameters(SESSION_ID)
                .setReplyType(LIST)
                .setReplyValueType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(GET_LAST_SENT_MESSAGE_ID, resolver)
                .setParameters(SESSION_ID, ADDRESS_NAME)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(GET_SESSION_CREATION_TIME, resolver)
                .setParameters(SESSION_ID)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_SESSIONS_AS_JSON, resolver)
                .setParameters(CONNECTION_ID)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_HTML, resolver)
                .setReplyType(STRING)
                .build(),
                this);
    }

    private JMSServerControl getServerControl(final OperationContext context, final ModelNode operation) {
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        return JMSServerControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_SERVER));
    }
}
