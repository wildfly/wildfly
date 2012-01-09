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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.EnumSet;
import java.util.Locale;

import org.hornetq.api.core.management.HornetQServerControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Handles operations and attribute reads supported by a HornetQ {@link org.hornetq.api.core.management.HornetQServerControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HornetQServerControlHandler extends AbstractRuntimeOnlyHandler {

    public static HornetQServerControlHandler INSTANCE = new HornetQServerControlHandler();

    public static final AttributeDefinition STARTED = new SimpleAttributeDefinition(CommonAttributes.STARTED, ModelType.BOOLEAN,
            false, AttributeAccess.Flag.STORAGE_RUNTIME);

    public static final AttributeDefinition VERSION = new SimpleAttributeDefinition(CommonAttributes.VERSION, ModelType.STRING,
            false, AttributeAccess.Flag.STORAGE_RUNTIME);

    private static final AttributeDefinition[] ATTRIBUTES = { STARTED, VERSION };
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
    public static final String LIST_CONNECTION_IDS= "list-connection-ids";
    public static final String LIST_PRODUCERS_INFO_AS_JSON = "list-producers-info-as-json";
    public static final String LIST_SESSIONS = "list-sessions";
    public static final String GET_ROLES = "get-roles";
    public static final String GET_ROLES_AS_JSON = "get-roles-as-json";
    public static final String GET_ADDRESS_SETTINGS_AS_JSON = "get-address-settings-as-json";
    public static final String FORCE_FAILOVER = "force-failover";
        // enableMessageCounters(maybe), disableMessageCounters(maybe), resetAllMessageCounters,
        // resetAllMessageCounterHistories, listPreparedTransactions,
        // listPreparedTransactionDetailsAsJSON, listPreparedTransactionDetailsAsHTML, listHeuristicCommittedTransactions
        // listHeuristicRolledBackTransactions, commitPreparedTransaction, rollbackPreparedTransaction,
        // listRemoteAddresses, listRemoteAddresses(String), closeConnectionsForAddress, listConnectionIDs,
        // listProducersInfoAsJSON, listSessions, getRoles, getRolesAsJSON, getAddressSettingsAsJSON,
        // forceFailover

    public static final String HQ_SERVER = "hornetq-server";
    public static final String TRANSACTION_AS_BASE_64 = "transaction-as-base-64";
    public static final String ADDRESS_MATCH = "address-match";
    public static final String CONNECTION_ID = "connection-id";
    public static final String IP_ADDRESS = "ip-address";

    private final ParametersValidator transactionValidator = new ParametersValidator();
    private final ParametersValidator addressValidator = new ParametersValidator();
    private final ParametersValidator ipAddressValidator = new ParametersValidator();
    private final ParametersValidator optionalIpAddressValidator = new ParametersValidator();
    private final ParametersValidator connectionIdValidator = new ParametersValidator();

    private HornetQServerControlHandler() {
        final StringLengthValidator stringLengthValidator = new StringLengthValidator(1);
        transactionValidator.registerValidator(TRANSACTION_AS_BASE_64, stringLengthValidator);
        addressValidator.registerValidator(ADDRESS_MATCH, stringLengthValidator);
        ipAddressValidator.registerValidator(IP_ADDRESS, stringLengthValidator);
        optionalIpAddressValidator.registerValidator(IP_ADDRESS, new StringLengthValidator(1, Integer.MAX_VALUE, true, false));
        connectionIdValidator.registerValidator(CONNECTION_ID, stringLengthValidator);
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        final String operationName = operation.require(OP).asString();
        final HornetQServerControl serverControl = getServerControl(context, operation);

        try {
            if (READ_ATTRIBUTE_OPERATION.equals(operationName)) {
                handleReadAttribute(context, operation, serverControl);
            } else if (GET_CONNECTORS_AS_JSON.equals(operationName)) {
                String json = serverControl.getConnectorsAsJSON();
                context.getResult().set(json);
            } else if (RESET_ALL_MESSAGE_COUNTERS.equals(operationName)) {
                serverControl.resetAllMessageCounters();
                context.getResult();
            } else if (RESET_ALL_MESSAGE_COUNTER_HISTORIES.equals(operationName)) {
                serverControl.resetAllMessageCounterHistories();
                context.getResult();
            } else if (LIST_PREPARED_TRANSACTIONS.equals(operationName)) {
                String[] list = serverControl.listPreparedTransactions();
                reportListOfString(context, list);
            } else if (LIST_PREPARED_TRANSACTION_DETAILS_AS_JSON.equals(operationName)) {
                String json = serverControl.listPreparedTransactionDetailsAsJSON();
                context.getResult().set(json);
            } else if (LIST_PREPARED_TRANSACTION_DETAILS_AS_HTML.equals(operationName)) {
                String html = serverControl.listPreparedTransactionDetailsAsHTML();
                context.getResult().set(html);
            } else if (LIST_HEURISTIC_COMMITTED_TRANSACTIONS.equals(operationName)) {
                String[] list = serverControl.listHeuristicCommittedTransactions();
                reportListOfString(context, list);
            } else if (LIST_HEURISTIC_ROLLED_BACK_TRANSACTIONS.equals(operationName)) {
                String[] list = serverControl.listHeuristicRolledBackTransactions();
                reportListOfString(context, list);
            } else if (COMMIT_PREPARED_TRANSACTION.equals(operationName)) {
                transactionValidator.validate(operation);
                String txId = operation.require(TRANSACTION_AS_BASE_64).asString();
                boolean committed = serverControl.commitPreparedTransaction(txId);
                context.getResult().set(committed);
            } else if (ROLLBACK_PREPARED_TRANSACTION.equals(operationName)) {
                transactionValidator.validate(operation);
                String txId = operation.require(TRANSACTION_AS_BASE_64).asString();
                boolean committed = serverControl.rollbackPreparedTransaction(txId);
                context.getResult().set(committed);
            } else if (LIST_REMOTE_ADDRESSES.equals(operationName)) {
                optionalIpAddressValidator.validate(operation);
                ModelNode addr = operation.get(IP_ADDRESS);
                String[] list = addr.isDefined() ? serverControl.listRemoteAddresses(addr.asString()) : serverControl.listRemoteAddresses();
                reportListOfString(context, list);
            } else if (CLOSE_CONNECTIONS_FOR_ADDRESS.equals(operationName)) {
                ipAddressValidator.validate(operation);
                boolean closed = serverControl.closeConnectionsForAddress(operation.require(IP_ADDRESS).asString());
                context.getResult().set(closed);
            } else if (LIST_CONNECTION_IDS.equals(operationName)) {
                String[] list = serverControl.listConnectionIDs();
                reportListOfString(context, list);
            } else if (LIST_PRODUCERS_INFO_AS_JSON.equals(operationName)) {
                String json = serverControl.listProducersInfoAsJSON();
                context.getResult().set(json);
            } else if (LIST_SESSIONS.equals(operationName)) {
                connectionIdValidator.validate(operation);
                String[] list = serverControl.listSessions(operation.require(CONNECTION_ID).asString());
                reportListOfString(context, list);
            } else if (GET_ROLES.equals(operationName)) {
                addressValidator.validate(operation);
                String json = serverControl.getRolesAsJSON(operation.require(ADDRESS_MATCH).asString());
                ModelNode camelCase = ModelNode.fromJSONString(json);
                ModelNode converted = CamelCaseUtil.convertSecurityRole(camelCase);
                context.getResult().set(converted);
            } else if (GET_ROLES_AS_JSON.equals(operationName)) {
                addressValidator.validate(operation);
                String json = serverControl.getRolesAsJSON(operation.require(ADDRESS_MATCH).asString());
                ModelNode camelCase = ModelNode.fromJSONString(json);
                ModelNode converted = CamelCaseUtil.convertSecurityRole(camelCase);
                json = converted.toJSONString(true);
                context.getResult().set(json);
            } else if (GET_ADDRESS_SETTINGS_AS_JSON.equals(operationName)) {
                addressValidator.validate(operation);
                String json = serverControl.getAddressSettingsAsJSON(operation.require(ADDRESS_MATCH).asString());
                context.getResult().set(json);
            } else if (FORCE_FAILOVER.equals(operationName)) {
                serverControl.forceFailover();
                context.getResult();
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

    public void registerAttributes(final ManagementResourceRegistration registry) {

        for (AttributeDefinition attr : ATTRIBUTES) {
            registry.registerReadOnlyAttribute(attr, this);
        }
    }

    public void registerOperations(final ManagementResourceRegistration registry) {

        final EnumSet<OperationEntry.Flag> readOnly = EnumSet.of(OperationEntry.Flag.READ_ONLY);

        registry.registerOperationHandler(GET_CONNECTORS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, GET_CONNECTORS_AS_JSON, HQ_SERVER, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(RESET_ALL_MESSAGE_COUNTERS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDescriptionOnlyOperation(locale, RESET_ALL_MESSAGE_COUNTERS, HQ_SERVER);
            }
        });

        registry.registerOperationHandler(RESET_ALL_MESSAGE_COUNTER_HISTORIES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDescriptionOnlyOperation(locale, RESET_ALL_MESSAGE_COUNTER_HISTORIES, HQ_SERVER);
            }
        });

        registry.registerOperationHandler(LIST_PREPARED_TRANSACTIONS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleListReplyOperation(locale, LIST_PREPARED_TRANSACTIONS, HQ_SERVER, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_PREPARED_TRANSACTION_DETAILS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_PREPARED_TRANSACTION_DETAILS_AS_JSON, HQ_SERVER, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_PREPARED_TRANSACTION_DETAILS_AS_HTML, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_PREPARED_TRANSACTION_DETAILS_AS_HTML, HQ_SERVER, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_HEURISTIC_COMMITTED_TRANSACTIONS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleListReplyOperation(locale, LIST_HEURISTIC_COMMITTED_TRANSACTIONS, HQ_SERVER, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_HEURISTIC_ROLLED_BACK_TRANSACTIONS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleListReplyOperation(locale, LIST_HEURISTIC_ROLLED_BACK_TRANSACTIONS, HQ_SERVER, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(COMMIT_PREPARED_TRANSACTION, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleReplyOperation(locale, COMMIT_PREPARED_TRANSACTION,
                        HQ_SERVER, TRANSACTION_AS_BASE_64, ModelType.STRING, true, ModelType.BOOLEAN, true);
            }
        });

        registry.registerOperationHandler(ROLLBACK_PREPARED_TRANSACTION, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleReplyOperation(locale, ROLLBACK_PREPARED_TRANSACTION,
                        HQ_SERVER, TRANSACTION_AS_BASE_64, ModelType.STRING, true, ModelType.BOOLEAN, true);
            }
        });

        registry.registerOperationHandler(LIST_REMOTE_ADDRESSES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleListReplyOperation(locale,  LIST_REMOTE_ADDRESSES,
                        HQ_SERVER, IP_ADDRESS, ModelType.STRING, true, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(CLOSE_CONNECTIONS_FOR_ADDRESS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleReplyOperation(locale, CLOSE_CONNECTIONS_FOR_ADDRESS,
                        HQ_SERVER, IP_ADDRESS, ModelType.STRING, false, ModelType.BOOLEAN, true);
            }
        });

        registry.registerOperationHandler(LIST_CONNECTION_IDS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleListReplyOperation(locale, LIST_CONNECTION_IDS, HQ_SERVER, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_PRODUCERS_INFO_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_PRODUCERS_INFO_AS_JSON, HQ_SERVER, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_SESSIONS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleListReplyOperation(locale,  LIST_SESSIONS,
                        HQ_SERVER, CONNECTION_ID, ModelType.STRING, true, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(GET_ROLES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getGetRoles(locale);
            }
        }, readOnly);

        registry.registerOperationHandler(GET_ROLES_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleReplyOperation(locale, GET_ROLES_AS_JSON, HQ_SERVER,
                        ADDRESS_MATCH, ModelType.STRING, false, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(GET_ADDRESS_SETTINGS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getSingleParamSimpleReplyOperation(locale, GET_ADDRESS_SETTINGS_AS_JSON, HQ_SERVER,
                        ADDRESS_MATCH, ModelType.STRING, false, ModelType.STRING, true);
            }
        }, readOnly);

        registry.registerOperationHandler(FORCE_FAILOVER, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDescriptionOnlyOperation(locale, FORCE_FAILOVER, HQ_SERVER);
            }
        });
    }

    private void handleReadAttribute(OperationContext context, ModelNode operation, final HornetQServerControl serverControl) {
        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        if (STARTED.getName().equals(name)) {
            boolean started = serverControl.isStarted();
            context.getResult().set(started);
        } else if (VERSION.getName().equals(name)) {
            String version = serverControl.getVersion();
            context.getResult().set(version);
        } else {
            // Bug
            throw MESSAGES.unsupportedAttribute(name);
        }
    }

    private HornetQServerControl getServerControl(final OperationContext context, ModelNode operation) throws OperationFailedException {
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        if (hqService == null || hqService.getState() != ServiceController.State.UP) {
            throw MESSAGES.hornetQServerNotInstalled(hqServiceName.getSimpleName());
        }
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        return hqServer.getHornetQServerControl();
    }

    private void reportListOfString(OperationContext context, String[] list) {
        final ModelNode result = context.getResult();
        result.setEmptyList();
        for (String tx : list) {
            result.add(tx);
        }
    }
}
