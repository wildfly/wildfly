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

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.dmr.ModelType.LIST;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.rollbackOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.ManagementUtil.reportListOfStrings;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.createNonEmptyStringAttribute;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.runtimeReadOnlyOperation;
import static org.wildfly.extension.messaging.activemq.jms.JMSQueueService.JMS_QUEUE_PREFIX;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicService.JMS_TOPIC_PREFIX;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ServerSession;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Handles operations and attribute reads supported by a ActiveMQ {@link ActiveMQServerControl}.
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

    public static final JMSServerControlHandler INSTANCE = new JMSServerControlHandler();

    private JMSServerControlHandler() {
    }

    public JsonObject enrich(JsonObject source, String key, String value) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add(key, value);
        source.entrySet().
                forEach(e -> builder.add(e.getKey(), e.getValue()));
        return builder.build();
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (rollbackOperationIfServerNotActive(context, operation)) {
            return;
        }

        final String operationName = operation.require(OP).asString();
        final ActiveMQServer server = getServer(context, operation);
        if (server == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }
        final ActiveMQServerControl serverControl = server.getActiveMQServerControl();

        try {
            if (LIST_CONNECTIONS_AS_JSON.equals(operationName)) {
                String json = serverControl.listConnectionsAsJSON();
                context.getResult().set(json);

                final JsonArrayBuilder enrichedConnections = Json.createArrayBuilder();
                try (
                        JsonReader reader = Json.createReader(new StringReader(json));
                ) {
                    final JsonArray connections = reader.readArray();

                    for (int i = 0; i < connections.size(); i++) {

                        final JsonObject originalConnection = connections.getJsonObject(i);
                        final JsonObject enrichedConnection = enrichConnection(originalConnection, serverControl);
                        enrichedConnections.add(enrichedConnection);
                    }
                }

                String enrichedJSON = enrichedConnections.build().toString();
                context.getResult().set(enrichedJSON);
            } else if (LIST_CONSUMERS_AS_JSON.equals(operationName)) {
                String connectionID = CONNECTION_ID.resolveModelAttribute(context, operation).asString();
                String json = serverControl.listConsumersAsJSON(connectionID);

                final JsonArrayBuilder enrichedConsumers = Json.createArrayBuilder();
                try (
                        JsonReader reader = Json.createReader(new StringReader(json));
                ) {
                    final JsonArray consumers = reader.readArray();

                    for (int i = 0; i < consumers.size(); i++) {

                        final JsonObject originalConsumer = consumers.getJsonObject(i);
                        final JsonObject enrichedConsumer = enrichConsumer(originalConsumer, server);
                        enrichedConsumers.add(enrichedConsumer);
                    }
                }

                String enrichedJSON = enrichedConsumers.build().toString();
                context.getResult().set(enrichedJSON);
            } else if (LIST_ALL_CONSUMERS_AS_JSON.equals(operationName)) {
                String json = serverControl.listAllConsumersAsJSON();

                final JsonArrayBuilder enrichedConsumers = Json.createArrayBuilder();
                try (
                        JsonReader reader = Json.createReader(new StringReader(json));
                ) {
                    final JsonArray consumers = reader.readArray();

                    for (int i = 0; i < consumers.size(); i++) {

                        final JsonObject originalConsumer = consumers.getJsonObject(i);
                        final JsonObject enrichedConsumer = enrichConsumer(originalConsumer, server);
                        enrichedConsumers.add(enrichedConsumer);
                    }
                }

                String enrichedJSON = enrichedConsumers.build().toString();
                context.getResult().set(enrichedJSON);
            } else if (LIST_TARGET_DESTINATIONS.equals(operationName)) {
                String sessionID = SESSION_ID.resolveModelAttribute(context, operation).asString();
                // Artemis no longer defines the method. Its implementation from Artemis 1.5 has been inlined:
                String[] list = listTargetDestinations(server, sessionID);
                reportListOfStrings(context, list);
            } else if (GET_LAST_SENT_MESSAGE_ID.equals(operationName)) {
                String sessionID = SESSION_ID.resolveModelAttribute(context, operation).asString();
                String addressName = ADDRESS_NAME.resolveModelAttribute(context, operation).asString();
                // Artemis no longer defines the method. Its implementation from Artemis 1.5 has been inlined:
                ServerSession session = server.getSessionByID(sessionID);
                if (session != null) {
                    String messageID = session.getLastSentMessageID(addressName);
                    context.getResult().set(messageID);
                }
            } else if (GET_SESSION_CREATION_TIME.equals(operationName)) {
                String sessionID = SESSION_ID.resolveModelAttribute(context, operation).asString();
                // Artemis no longer defines the method. Its implementation from Artemis 1.5 has been inlined:
                ServerSession session = server.getSessionByID(sessionID);
                if (session != null) {
                    String time = String.valueOf(session.getCreationTime());
                    context.getResult().set(time);
                }
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
    }

    private JsonObject enrichConsumer(JsonObject originalConsumer, ActiveMQServer server) {
        JsonObjectBuilder enrichedConsumer = Json.createObjectBuilder();

        for (Map.Entry<String, JsonValue> entry : originalConsumer.entrySet()) {
            enrichedConsumer.add(entry.getKey(), entry.getValue());
        }
        String queueName = originalConsumer.getString("queueName");
        final QueueControl queueControl = QueueControl.class.cast(server.getManagementService().getResource(ResourceNames.QUEUE + queueName));
        if (queueControl == null) {
            return originalConsumer;
        }
        enrichedConsumer.add("durable", queueControl.isDurable());
        String routingType = queueControl.getRoutingType();
        String destinationType = routingType.equals("ANYCAST") ? "queue" : "topic";
        enrichedConsumer.add("destinationType", destinationType);
        String address = queueControl.getAddress();
        String destinationName = inferDestinationName(address);
        enrichedConsumer.add("destinationName", destinationName);

        return enrichedConsumer.build();
    }

    private JsonObject enrichConnection(JsonObject originalConnection, ActiveMQServerControl serverControl) throws Exception {
        JsonObjectBuilder enrichedConnection = Json.createObjectBuilder();

        for (Map.Entry<String, JsonValue> entry : originalConnection.entrySet()) {
            enrichedConnection.add(entry.getKey(), entry.getValue());
        }

        final String connectionID = originalConnection.getString("connectionID");
        final String sessionsAsJSON = serverControl.listSessionsAsJSON(connectionID);
        try (JsonReader sessionsReader = Json.createReader(new StringReader(sessionsAsJSON))) {
            final JsonArray sessions = sessionsReader.readArray();
            for (int j = 0; j < sessions.size(); j++) {
                final JsonObject session = sessions.getJsonObject(j);
                if (session.containsKey("metadata")) {
                    final JsonObject metadata = session.getJsonObject("metadata");
                    if (metadata.containsKey("jms-client-id")) {
                        String clientID = metadata.getString("jms-client-id");
                        enrichedConnection.add("clientID", clientID);
                        break;
                    }
                }
            }
        }

        return enrichedConnection.build();
    }

    /**
     * Infer the name of the JMS destination based on the queue's address.
     */
    private String inferDestinationName(String address) {
        if (address.startsWith(JMS_QUEUE_PREFIX)) {
            return address.substring(JMS_QUEUE_PREFIX.length());
        } else if (address.startsWith(JMS_TOPIC_PREFIX)) {
            return address.substring(JMS_TOPIC_PREFIX.length());
        } else {
            return address;
        }
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

    private ActiveMQServer getServer(final OperationContext context, final ModelNode operation) {
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        ActiveMQServer server = ActiveMQServer.class.cast(service.getValue());
        return server;
    }

    public String[] listTargetDestinations(ActiveMQServer server, String sessionID) throws Exception {
        ServerSession session = server.getSessionByID(sessionID);
        if (session == null) {
            return new String[0];
        }
        String[] addresses = session.getTargetAddresses();
        Map<String, QueueControl> allDests = new HashMap<>();

        Object[] queueControls = server.getManagementService().getResources(QueueControl.class);
        for (Object queue : queueControls) {
            QueueControl queueControl = (QueueControl)queue;
            allDests.put(queueControl.getAddress(), queueControl);
        }

        List<String> destinations = new ArrayList<>();
        for (String addresse : addresses) {
            QueueControl control = allDests.get(addresse);
            if (control != null) {
                destinations.add(control.getAddress());
            }
        }
        return destinations.toArray(new String[destinations.size()]);
    }
}
