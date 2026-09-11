/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.LIST;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.rollbackOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.ManagementUtil.reportListOfStrings;
import static org.wildfly.extension.messaging.activemq.ManagementUtil.reportRoles;
import static org.wildfly.extension.messaging.activemq.ManagementUtil.reportRolesAsJSON;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.createNonEmptyStringAttribute;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.runtimeOnlyOperation;
import static org.wildfly.extension.messaging.activemq.OperationDefinitionHelper.runtimeReadOnlyOperation;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.StringAllowedValuesValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Handles operations and attribute reads supported by a ActiveMQ
 * {@link org.apache.activemq.api.core.management.ActiveMQServerControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ActiveMQServerControlHandler extends AbstractRuntimeOnlyHandler {

    private static final String[] ALLOWED_RUNTIME_JOURNAL_TYPE = {"ASYNCIO", "NIO", "DATABASE", "NONE"};

    static final ActiveMQServerControlHandler INSTANCE = new ActiveMQServerControlHandler();

    public static final AttributeDefinition ACTIVE = create("active", BOOLEAN)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition STARTED = new SimpleAttributeDefinitionBuilder(CommonAttributes.STARTED, ModelType.BOOLEAN,
            false).setStorageRuntime().build();

    public static final AttributeDefinition RUNTIME_JOURNAL_TYPE = create("runtime-journal-type", STRING)
            .setStorageRuntime()
            .setValidator(new StringAllowedValuesValidator(ALLOWED_RUNTIME_JOURNAL_TYPE))
            .build();

    public static final AttributeDefinition VERSION = new SimpleAttributeDefinitionBuilder(CommonAttributes.VERSION, ModelType.STRING,
            true).setStorageRuntime().build();

    private static final AttributeDefinition[] ATTRIBUTES = {STARTED, VERSION, ACTIVE, RUNTIME_JOURNAL_TYPE};
    public static final String GET_CONNECTORS_AS_JSON = "get-connectors-as-json";
//    public static final String ENABLE_MESSAGE_COUNTERS = "enable-message-counters";
//    public static final String DISABLE_MESSAGE_COUNTERS = "disable-message-counters";
    public static final String RESET_ALL_MESSAGE_COUNTERS = "reset-all-message-counters";
    public static final String RESET_ALL_MESSAGE_COUNTER_HISTORIES = "reset-all-message-counter-histories";
    public static final String LIST_PREPARED_TRANSACTIONS = "list-prepared-transactions";
    public static final String LIST_PREPARED_TRANSACTION_DETAILS_AS_JSON = "list-prepared-transaction-details-as-json";
    public static final String LIST_PREPARED_TRANSACTION_DETAILS_AS_HTML = "list-prepared-transaction-details-as-html";
    public static final String LIST_HEURISTIC_COMMITTED_TRANSACTIONS = "list-heuristic-committed-transactions";
    public static final String LIST_HEURISTIC_ROLLED_BACK_TRANSACTIONS = "list-heuristic-rolled-back-transactions";
    public static final String COMMIT_PREPARED_TRANSACTION = "commit-prepared-transaction";
    public static final String ROLLBACK_PREPARED_TRANSACTION = "rollback-prepared-transaction";
    public static final String LIST_REMOTE_ADDRESSES = "list-remote-addresses";
    public static final String CLOSE_CONNECTIONS_FOR_ADDRESS = "close-connections-for-address";
    public static final String CLOSE_CONNECTIONS_FOR_USER = "close-connections-for-user";
    public static final String CLOSE_CONSUMER_CONNECTIONS_FOR_ADDRESS = "close-consumer-connections-for-address";
    public static final String LIST_CONNECTION_IDS = "list-connection-ids";
    public static final String LIST_PRODUCERS_INFO_AS_JSON = "list-producers-info-as-json";
    public static final String LIST_SESSIONS = "list-sessions";
    public static final String GET_ROLES = "get-roles";
    // we keep the operation for backwards compatibility but it duplicates the "get-roles" operation
    // (except it returns a String instead of a List)
    @Deprecated
    public static final String GET_ROLES_AS_JSON = "get-roles-as-json";
    public static final String GET_ADDRESS_SETTINGS_AS_JSON = "get-address-settings-as-json";
    public static final String FORCE_FAILOVER = "force-failover";

    public static final AttributeDefinition TRANSACTION_AS_BASE_64 = createNonEmptyStringAttribute("transaction-as-base-64");
    public static final AttributeDefinition ADDRESS_MATCH = createNonEmptyStringAttribute("address-match");
    public static final AttributeDefinition USER = createNonEmptyStringAttribute("user");
    public static final AttributeDefinition CONNECTION_ID = createNonEmptyStringAttribute("connection-id");
    public static final AttributeDefinition REQUIRED_IP_ADDRESS = createNonEmptyStringAttribute("ip-address");
    public static final AttributeDefinition OPTIONAL_IP_ADDRESS = SimpleAttributeDefinitionBuilder.create("ip-address", ModelType.STRING)
            .setRequired(false)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .build();

    private ActiveMQServerControlHandler() {
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String operationName = operation.require(OP).asString();

        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        if (READ_ATTRIBUTE_OPERATION.equals(operationName) || GET_ADDRESS_SETTINGS_AS_JSON.equals(operationName)) {
            ActiveMQBroker server = null;
            if (context.getRunningMode() == RunningMode.NORMAL) {
                ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
                if (service == null || service.getState() != ServiceController.State.UP) {
                    throw MessagingLogger.ROOT_LOGGER.activeMQServerNotInstalled(serviceName.getSimpleName());
                }
                server = ActiveMQBroker.class.cast(service.getValue());
            }
            if (READ_ATTRIBUTE_OPERATION.equals(operationName)) {
                handleReadAttribute(context, operation, server);
                return;
            }
            String addressMatch = ADDRESS_MATCH.resolveModelAttribute(context, operation).asString();
            context.getResult().set(server.getAddressSettingsAsJSON(addressMatch));
            return;
        }

        if (rollbackOperationIfServerNotActive(context, operation)) {
            return;
        }

        final ActiveMQServerControl serverControl = getServerControl(context, operation);

        try {
            if (null == operationName) {
                // Bug
                throw MessagingLogger.ROOT_LOGGER.unsupportedOperation(operationName);
            } else {
                switch (operationName) {
                    case GET_CONNECTORS_AS_JSON: {
                        String json = serverControl.getConnectorsAsJSON();
                        context.getResult().set(json);
                        break;
                    }
                    case RESET_ALL_MESSAGE_COUNTERS:
                        serverControl.resetAllMessageCounters();
                        context.getResult();
                        break;
                    case RESET_ALL_MESSAGE_COUNTER_HISTORIES:
                        serverControl.resetAllMessageCounterHistories();
                        context.getResult();
                        break;
                    case LIST_PREPARED_TRANSACTIONS: {
                        String[] list = serverControl.listPreparedTransactions();
                        reportListOfStrings(context, list);
                        break;
                    }
                    case LIST_PREPARED_TRANSACTION_DETAILS_AS_JSON: {
                        String json = serverControl.listPreparedTransactionDetailsAsJSON();
                        context.getResult().set(json);
                        break;
                    }
                    case LIST_PREPARED_TRANSACTION_DETAILS_AS_HTML:
                        String html = serverControl.listPreparedTransactionDetailsAsHTML();
                        context.getResult().set(html);
                        break;
                    case LIST_HEURISTIC_COMMITTED_TRANSACTIONS: {
                        String[] list = serverControl.listHeuristicCommittedTransactions();
                        reportListOfStrings(context, list);
                        break;
                    }
                    case LIST_HEURISTIC_ROLLED_BACK_TRANSACTIONS: {
                        String[] list = serverControl.listHeuristicRolledBackTransactions();
                        reportListOfStrings(context, list);
                        break;
                    }
                    case COMMIT_PREPARED_TRANSACTION: {
                        String txId = TRANSACTION_AS_BASE_64.resolveModelAttribute(context, operation).asString();
                        boolean committed = serverControl.commitPreparedTransaction(txId);
                        context.getResult().set(committed);
                        break;
                    }
                    case ROLLBACK_PREPARED_TRANSACTION: {
                        String txId = TRANSACTION_AS_BASE_64.resolveModelAttribute(context, operation).asString();
                        boolean committed = serverControl.rollbackPreparedTransaction(txId);
                        context.getResult().set(committed);
                        break;
                    }
                    case LIST_REMOTE_ADDRESSES: {
                        ModelNode address = OPTIONAL_IP_ADDRESS.resolveModelAttribute(context, operation);
                        String[] list = address.isDefined() ? serverControl.listRemoteAddresses(address.asString()) : serverControl.listRemoteAddresses();
                        reportListOfStrings(context, list);
                        break;
                    }
                    case CLOSE_CONNECTIONS_FOR_ADDRESS: {
                        String address = REQUIRED_IP_ADDRESS.resolveModelAttribute(context, operation).asString();
                        boolean closed = serverControl.closeConnectionsForAddress(address);
                        context.getResult().set(closed);
                        break;
                    }
                    case CLOSE_CONNECTIONS_FOR_USER: {
                        String user = USER.resolveModelAttribute(context, operation).asString();
                        boolean closed = serverControl.closeConnectionsForUser(user);
                        context.getResult().set(closed);
                        break;
                    }
                    case CLOSE_CONSUMER_CONNECTIONS_FOR_ADDRESS: {
                        String address = ADDRESS_MATCH.resolveModelAttribute(context, operation).asString();
                        boolean closed = serverControl.closeConsumerConnectionsForAddress(address);
                        context.getResult().set(closed);
                        break;
                    }
                    case LIST_CONNECTION_IDS: {
                        String[] list = serverControl.listConnectionIDs();
                        reportListOfStrings(context, list);
                        break;
                    }
                    case LIST_PRODUCERS_INFO_AS_JSON: {
                        String json = serverControl.listProducersInfoAsJSON();
                        json = json.replace("lastProducedMessageID", "lastUUIDSent");
                        context.getResult().set(json);
                        break;
                    }
                    case LIST_SESSIONS: {
                        String connectionID = CONNECTION_ID.resolveModelAttribute(context, operation).asString();
                        String[] list = serverControl.listSessions(connectionID);
                        reportListOfStrings(context, list);
                        break;
                    }
                    case GET_ROLES: {
                        String addressMatch = ADDRESS_MATCH.resolveModelAttribute(context, operation).asString();
                        reportRoles(context, serverControl.getRoles(addressMatch));
                        break;
                    }
                    case GET_ROLES_AS_JSON: {
                        String addressMatch = ADDRESS_MATCH.resolveModelAttribute(context, operation).asString();
                        String json = serverControl.getRolesAsJSON(addressMatch);
                        reportRolesAsJSON(context, json);
                        break;
                    }
                    case GET_ADDRESS_SETTINGS_AS_JSON: {
                        String addressMatch = ADDRESS_MATCH.resolveModelAttribute(context, operation).asString();
                        String json = serverControl.getAddressSettingsAsJSON(addressMatch);
                        context.getResult().set(ManagementUtil.convertAddressSettingInfosAsJSON(json));
                        break;
                    }
                    case FORCE_FAILOVER:
                        serverControl.forceFailover();
                        context.getResult();
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

    public void registerAttributes(final ManagementResourceRegistration registry) {

        for (AttributeDefinition attr : ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, this);
        }
    }

    public void registerOperations(final ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {

        registry.registerOperationHandler(runtimeReadOnlyOperation(GET_CONNECTORS_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(RESET_ALL_MESSAGE_COUNTERS, resolver)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(RESET_ALL_MESSAGE_COUNTER_HISTORIES, resolver)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_PREPARED_TRANSACTIONS, resolver)
                .setReplyType(LIST)
                .setReplyValueType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_PREPARED_TRANSACTION_DETAILS_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_PREPARED_TRANSACTION_DETAILS_AS_HTML, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_HEURISTIC_COMMITTED_TRANSACTIONS, resolver)
                .setReplyType(LIST)
                .setReplyValueType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_HEURISTIC_ROLLED_BACK_TRANSACTIONS, resolver)
                .setReplyType(LIST)
                .setReplyValueType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(COMMIT_PREPARED_TRANSACTION, resolver)
                .setParameters(TRANSACTION_AS_BASE_64)
                .setReplyType(BOOLEAN)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(ROLLBACK_PREPARED_TRANSACTION, resolver)
                .setParameters(TRANSACTION_AS_BASE_64)
                .setReplyType(BOOLEAN)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_REMOTE_ADDRESSES, resolver)
                .setParameters(OPTIONAL_IP_ADDRESS)
                .setReplyType(LIST)
                .setReplyValueType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(CLOSE_CONNECTIONS_FOR_ADDRESS, resolver)
                .setParameters(REQUIRED_IP_ADDRESS)
                .setReplyType(BOOLEAN)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(CLOSE_CONNECTIONS_FOR_USER, resolver)
                .setParameters(USER)
                .setReplyType(BOOLEAN)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(CLOSE_CONSUMER_CONNECTIONS_FOR_ADDRESS, resolver)
                .setParameters(ADDRESS_MATCH)
                .setReplyType(BOOLEAN)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_CONNECTION_IDS, resolver)
                .setReplyType(LIST)
                .setReplyValueType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_PRODUCERS_INFO_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_SESSIONS, resolver)
                .setParameters(CONNECTION_ID)
                .setReplyType(LIST)
                .setReplyValueType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(GET_ROLES_AS_JSON, resolver)
                .setParameters(ADDRESS_MATCH)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(GET_ADDRESS_SETTINGS_AS_JSON, resolver)
                .setParameters(ADDRESS_MATCH)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(FORCE_FAILOVER, resolver)
                .build(),
                this);

        registry.registerOperationHandler(runtimeReadOnlyOperation(GET_ROLES, resolver)
                .setParameters(ADDRESS_MATCH)
                .setReplyType(LIST)
                .setReplyParameters(SecurityRoleDefinition.NAME,
                        SecurityRoleDefinition.SEND,
                        SecurityRoleDefinition.CONSUME,
                        SecurityRoleDefinition.CREATE_DURABLE_QUEUE,
                        SecurityRoleDefinition.DELETE_DURABLE_QUEUE,
                        SecurityRoleDefinition.CREATE_NON_DURABLE_QUEUE,
                        SecurityRoleDefinition.DELETE_NON_DURABLE_QUEUE,
                        SecurityRoleDefinition.MANAGE,
                        SecurityRoleDefinition.BROWSE,
                        SecurityRoleDefinition.CREATE_ADDRESS,
                        SecurityRoleDefinition.DELETE_ADDRESS)
                .build(),
                this);
    }

    private void handleReadAttribute(OperationContext context, ModelNode operation, final ActiveMQBroker broker) throws OperationFailedException {
        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        ActiveMQServer server = broker == null ? null : ActiveMQServer.class.cast(broker.getDelegate());
        if (STARTED.getName().equals(name)) {
            boolean started = server != null ? server.isStarted() : false;
            context.getResult().set(started);
        } else if (VERSION.getName().equals(name)) {
            if (server != null) {
                String version = server.getVersion().getFullVersion();
                context.getResult().set(version);
            }
        } else if (ACTIVE.getName().equals(name)) {
            boolean active = server != null ? server.isActive() : false;
            context.getResult().set(active);
        } else if (RUNTIME_JOURNAL_TYPE.getName().equals(name)) {
            if (server != null) {
                // if the configured journal type is not supported (e.g. using ASYNCIO without having installed libaio),
                // ActiveMQ will override its configuration to use the NIO journal type (that is available on any platform).
                if (server.getConfiguration().isPersistenceEnabled()) {
                    if (server.getConfiguration().getStoreConfiguration() != null && "DATABASE".equals(server.getConfiguration().getStoreConfiguration().getStoreType().name())) {
                        context.getResult().set("DATABASE");
                    } else {
                        context.getResult().set(server.getConfiguration().getJournalType().toString());
                    }
                } else {
                    context.getResult().set("NONE");
                }
            }
        } else {
            // Bug
            throw MessagingLogger.ROOT_LOGGER.unsupportedAttribute(name);
        }
    }

    private ActiveMQServerControl getServerControl(final OperationContext context, ModelNode operation) throws OperationFailedException {
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        if (service == null || service.getState() != ServiceController.State.UP) {
            throw MessagingLogger.ROOT_LOGGER.activeMQServerNotInstalled(serviceName.getSimpleName());
        }
        ActiveMQBroker server = ActiveMQBroker.class.cast(service.getValue());
        return server.getActiveMQServerControl();
    }
}
