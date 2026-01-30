/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ServerProducer;
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
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.ActiveMQBroker;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

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
    public static final String LIST_ALL_SESSIONS_AS_JSON = "list-all-sessions-as-json";
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
        final ActiveMQBroker server = getServer(context, operation);
        if (server == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }
        final ActiveMQServerControl serverControl = server.getActiveMQServerControl();

        try {
            if (null == operationName) {
                // Bug
                throw MessagingLogger.ROOT_LOGGER.unsupportedOperation(operationName);
            } else {
                switch (operationName) {
                    case LIST_CONNECTIONS_AS_JSON: {
                        String json = serverControl.listConnectionsAsJSON();
                        context.getResult().set(json);
                        final JsonArrayBuilder enrichedConnections = Json.createArrayBuilder();
                        try (
                                JsonReader reader = Json.createReader(new StringReader(json));) {
                            final JsonArray connections = reader.readArray();

                            for (int i = 0; i < connections.size(); i++) {

                                final JsonObject originalConnection = connections.getJsonObject(i);
                                final JsonObject enrichedConnection = enrichConnection(originalConnection, serverControl);
                                enrichedConnections.add(enrichedConnection);
                            }
                        }
                        String enrichedJSON = enrichedConnections.build().toString();
                        context.getResult().set(enrichedJSON);
                        break;
                    }
                    case LIST_CONSUMERS_AS_JSON: {
                        String connectionID = CONNECTION_ID.resolveModelAttribute(context, operation).asString();
                        String json = serverControl.listConsumersAsJSON(connectionID);
                        final JsonArrayBuilder enrichedConsumers = Json.createArrayBuilder();
                        try (
                                JsonReader reader = Json.createReader(new StringReader(json));) {
                            final JsonArray consumers = reader.readArray();

                            for (int i = 0; i < consumers.size(); i++) {

                                final JsonObject originalConsumer = consumers.getJsonObject(i);
                                final JsonObject enrichedConsumer = enrichConsumer(originalConsumer, server);
                                enrichedConsumers.add(enrichedConsumer);
                            }
                        }
                        String enrichedJSON = enrichedConsumers.build().toString();
                        context.getResult().set(enrichedJSON);
                        break;
                    }
                    case LIST_ALL_CONSUMERS_AS_JSON: {
                        String json = serverControl.listAllConsumersAsJSON();
                        final JsonArrayBuilder enrichedConsumers = Json.createArrayBuilder();
                        try (
                                JsonReader reader = Json.createReader(new StringReader(json));) {
                            final JsonArray consumers = reader.readArray();

                            for (int i = 0; i < consumers.size(); i++) {

                                final JsonObject originalConsumer = consumers.getJsonObject(i);
                                final JsonObject enrichedConsumer = enrichConsumer(originalConsumer, server);
                                enrichedConsumers.add(enrichedConsumer);
                            }
                        }
                        String enrichedJSON = enrichedConsumers.build().toString();
                        context.getResult().set(enrichedJSON);
                        break;
                    }
                    case LIST_TARGET_DESTINATIONS: {
                        String sessionID = SESSION_ID.resolveModelAttribute(context, operation).asString();
                        // Artemis no longer defines the method. Its implementation from Artemis 1.5 has been inlined:
                        String[] list = listTargetDestinations(server, sessionID);
                        reportListOfStrings(context, list);
                        break;
                    }
                    case GET_LAST_SENT_MESSAGE_ID: {
                        String sessionID = SESSION_ID.resolveModelAttribute(context, operation).asString();
                        String addressName = ADDRESS_NAME.resolveModelAttribute(context, operation).asString();
                        // Artemis no longer defines the method. Its implementation from Artemis 1.5 has been inlined:
                        ServerSession session = ((ActiveMQServer) server.getDelegate()).getSessionByID(sessionID);
                        if (session != null) {
                            for (ServerProducer producer : session.getServerProducers()) {
                                if (addressName.equals(producer.getAddress())) {
                                    context.getResult().set(producer.getLastProducedMessageID().toString());
                                    break;
                                }
                            }
                        }
                        break;
                    }
                    case GET_SESSION_CREATION_TIME: {
                        String sessionID = SESSION_ID.resolveModelAttribute(context, operation).asString();
                        // Artemis no longer defines the method. Its implementation from Artemis 1.5 has been inlined:
                        ServerSession session = ((ActiveMQServer) server.getDelegate()).getSessionByID(sessionID);
                        if (session != null) {
                            String time = String.valueOf(session.getCreationTime());
                            context.getResult().set(time);
                        }
                        break;
                    }
                    case LIST_ALL_SESSIONS_AS_JSON: {
                        //Each JMS session creates 2 sessions on the broker: st session is used for authentication then the 2nd for messages.
                        String json = serverControl.listAllSessionsAsJSON();
                        context.getResult().set(json);
                        break;
                    }
                    case LIST_SESSIONS_AS_JSON: {
                        String connectionID = CONNECTION_ID.resolveModelAttribute(context, operation).asString();
                        String json = serverControl.listSessionsAsJSON(connectionID);
                        context.getResult().set(json);
                        break;
                    }
                    case LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_JSON: {
                        String json = serverControl.listPreparedTransactionDetailsAsJSON();
                        context.getResult().set(json);
                        break;
                    }
                    case LIST_PREPARED_TRANSACTION_JMS_DETAILS_AS_HTML:
                        String html = serverControl.listPreparedTransactionDetailsAsHTML();
                        context.getResult().set(html);
                        break;
                    default:
                        // Bug
                        throw MessagingLogger.ROOT_LOGGER.unsupportedOperation(operationName);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }
    }

    private JsonObject enrichConsumer(JsonObject originalConsumer, ActiveMQBroker server) {
        JsonObjectBuilder enrichedConsumer = Json.createObjectBuilder();

        for (Map.Entry<String, JsonValue> entry : originalConsumer.entrySet()) {
            if ("lastProducedMessageID".equals(entry.getKey())) {
                enrichedConsumer.add("lastUUIDSent", entry.getValue());
            } else {
                enrichedConsumer.add(entry.getKey(), entry.getValue());
            }
        }
        String queueName = originalConsumer.getString("queueName");
        final QueueControl queueControl = QueueControl.class.cast(server.getResource(ResourceNames.QUEUE + queueName));
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
                .setStability(Stability.COMMUNITY)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_ALL_SESSIONS_AS_JSON, resolver)
                .setReplyType(LIST)
                .setReplyValueType(STRING)
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

    private ActiveMQBroker getServer(final OperationContext context, final ModelNode operation) {
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        ActiveMQBroker server = ActiveMQBroker.class.cast(service.getValue());
        return server;
    }

    public String[] listTargetDestinations(ActiveMQBroker server, String sessionID) throws Exception {
        ServerSession session = ((ActiveMQServer) server.getDelegate()).getSessionByID(sessionID);
        if (session == null) {
            return new String[0];
        }
        Map<String, QueueControl> allDests = new HashMap<>();

        Object[] queueControls = server.getResources(QueueControl.class);
        for (Object queue : queueControls) {
            QueueControl queueControl = (QueueControl) queue;
            allDests.put(queueControl.getAddress(), queueControl);
        }

        List<String> destinations = new ArrayList<>();
        for (ServerProducer producer : session.getServerProducers()) {
            QueueControl control = allDests.get(producer.getAddress());
            if (control != null) {
                destinations.add(control.getAddress());
            }
        }
        return destinations.toArray(String[]::new);
    }
}
