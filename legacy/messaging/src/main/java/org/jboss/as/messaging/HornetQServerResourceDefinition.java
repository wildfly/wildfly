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

import static org.jboss.as.messaging.CommonAttributes.ALLOW_FAILBACK;
import static org.jboss.as.messaging.CommonAttributes.ASYNC_CONNECTION_EXECUTION_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.BACKUP;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL_OVERRIDE;
import static org.jboss.as.messaging.CommonAttributes.CREATE_BINDINGS_DIR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_JOURNAL_DIR;
import static org.jboss.as.messaging.CommonAttributes.FAILBACK_DELAY;
import static org.jboss.as.messaging.CommonAttributes.FAILOVER_ON_SHUTDOWN;
import static org.jboss.as.messaging.CommonAttributes.ID_CACHE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JMX_DOMAIN;
import static org.jboss.as.messaging.CommonAttributes.JMX_MANAGEMENT_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_BUFFER_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_COMPACT_PERCENTAGE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_FILE_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MAX_IO;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_MIN_FILES;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_SYNC_NON_TRANSACTIONAL;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_SYNC_TRANSACTIONAL;
import static org.jboss.as.messaging.CommonAttributes.JOURNAL_TYPE;
import static org.jboss.as.messaging.CommonAttributes.LOG_JOURNAL_WRITE_RATE;
import static org.jboss.as.messaging.CommonAttributes.MANAGEMENT_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.MANAGEMENT_NOTIFICATION_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_MAX_DAY_HISTORY;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNTER_SAMPLE_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_EXPIRY_SCAN_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_EXPIRY_THREAD_PRIORITY;
import static org.jboss.as.messaging.CommonAttributes.PAGE_MAX_CONCURRENT_IO;
import static org.jboss.as.messaging.CommonAttributes.PERF_BLAST_PAGES;
import static org.jboss.as.messaging.CommonAttributes.PERSISTENCE_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY;
import static org.jboss.as.messaging.CommonAttributes.PERSIST_ID_CACHE;
import static org.jboss.as.messaging.CommonAttributes.RUN_SYNC_SPEED_TEST;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_INVALIDATION_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.SERVER_DUMP_INTERVAL;
import static org.jboss.as.messaging.CommonAttributes.SHARED_STORE;
import static org.jboss.as.messaging.CommonAttributes.STATISTICS_ENABLED;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_TIMEOUT;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION_TIMEOUT_SCAN_PERIOD;
import static org.jboss.as.messaging.CommonAttributes.WILD_CARD_ROUTING_ENABLED;

import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyAddStepHandler;
import org.jboss.as.controller.ModelOnlyResourceDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for the messaging subsystem HornetQServer resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HornetQServerResourceDefinition extends ModelOnlyResourceDefinition {

    public static final PathElement HORNETQ_SERVER_PATH = PathElement.pathElement(CommonAttributes.HORNETQ_SERVER);

    public static final AttributeDefinition[] ATTRIBUTES_WITH_EXPRESSION_ALLOWED_IN_1_2_0 = { ASYNC_CONNECTION_EXECUTION_ENABLED, PERSISTENCE_ENABLED, SECURITY_ENABLED, SECURITY_INVALIDATION_INTERVAL,
            WILD_CARD_ROUTING_ENABLED, MANAGEMENT_ADDRESS, MANAGEMENT_NOTIFICATION_ADDRESS, JMX_MANAGEMENT_ENABLED, JMX_DOMAIN,
            STATISTICS_ENABLED, MESSAGE_COUNTER_ENABLED, MESSAGE_COUNTER_SAMPLE_PERIOD, MESSAGE_COUNTER_MAX_DAY_HISTORY,
            CONNECTION_TTL_OVERRIDE, TRANSACTION_TIMEOUT, TRANSACTION_TIMEOUT_SCAN_PERIOD,
            MESSAGE_EXPIRY_SCAN_PERIOD, MESSAGE_EXPIRY_THREAD_PRIORITY, ID_CACHE_SIZE, PERSIST_ID_CACHE,
            BACKUP, ALLOW_FAILBACK, FAILBACK_DELAY, FAILOVER_ON_SHUTDOWN,
            SHARED_STORE, PERSIST_DELIVERY_COUNT_BEFORE_DELIVERY, PAGE_MAX_CONCURRENT_IO,
            CREATE_BINDINGS_DIR, CREATE_JOURNAL_DIR, JOURNAL_TYPE, JOURNAL_BUFFER_TIMEOUT, JOURNAL_BUFFER_SIZE,
            JOURNAL_SYNC_TRANSACTIONAL, JOURNAL_SYNC_NON_TRANSACTIONAL, LOG_JOURNAL_WRITE_RATE,
            JOURNAL_FILE_SIZE, JOURNAL_MIN_FILES, JOURNAL_COMPACT_PERCENTAGE, JOURNAL_COMPACT_MIN_FILES, JOURNAL_MAX_IO,
            PERF_BLAST_PAGES, RUN_SYNC_SPEED_TEST, SERVER_DUMP_INTERVAL};

    static final HornetQServerResourceDefinition INSTANCE = new HornetQServerResourceDefinition();

    private HornetQServerResourceDefinition() {
        super(HORNETQ_SERVER_PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.HORNETQ_SERVER),
                new ModelOnlyAddStepHandler(CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
                    @Override
                    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
                        super.populateModel(operation, model);

                        if (model.hasDefined(MESSAGE_COUNTER_ENABLED.getName())) {
                            ModelNode mceVal = model.get(MESSAGE_COUNTER_ENABLED.getName());
                            ModelNode seVal = model.get(STATISTICS_ENABLED.getName());
                            if (seVal.isDefined() && !seVal.equals(mceVal)) {
                                throw MessagingLogger.ROOT_LOGGER.inconsistentStatisticsSettings(MESSAGE_COUNTER_ENABLED.getName(), STATISTICS_ENABLED.getName());
                            }
                            seVal.set(mceVal);
                            model.remove(MESSAGE_COUNTER_ENABLED.getName());
                        }
                    }
                },
                CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES);
        setDeprecated(MessagingExtension.DEPRECATED_SINCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new ModelOnlyWriteAttributeHandler(CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES);
        for (AttributeDefinition ad : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            if (ad.getName().equals(CLUSTERED.getName())) {
                resourceRegistration.registerReadWriteAttribute(CLUSTERED,
                        ClusteredAttributeHandlers.READ_HANDLER,
                        ClusteredAttributeHandlers.WRITE_HANDLER);
            } else if (ad.getName().equals(MESSAGE_COUNTER_ENABLED.getName())) {
                MessageCounterEnabledHandler handler = new MessageCounterEnabledHandler();
                resourceRegistration.registerReadWriteAttribute(MESSAGE_COUNTER_ENABLED, handler, handler);
            } else {
                resourceRegistration.registerReadWriteAttribute(ad, null, writeHandler);
            }
        }

        // handle deprecate attributes
        resourceRegistration.registerReadWriteAttribute(CommonAttributes.LIVE_CONNECTOR_REF, null, DeprecatedAttributeWriteHandler.INSTANCE);
    }

    private static class MessageCounterEnabledHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode aliased = getAliasedOperation(operation);
            context.addStep(aliased, getHandlerForOperation(context, operation), OperationContext.Stage.MODEL, true);
        }

        private static ModelNode getAliasedOperation(ModelNode operation) {
            ModelNode aliased = operation.clone();
            aliased.get(ModelDescriptionConstants.NAME).set(CommonAttributes.STATISTICS_ENABLED.getName());
            return aliased;
        }

        private static OperationStepHandler getHandlerForOperation(OperationContext context, ModelNode operation) {
            ImmutableManagementResourceRegistration imrr = context.getResourceRegistration();
            return imrr.getOperationHandler(PathAddress.EMPTY_ADDRESS, operation.get(ModelDescriptionConstants.OP).asString());
        }
    }

    /**
     * The clustered configuration parameter no longer exists for HornetQ configuration (a hornetq server is automatically clustered if it has cluster-connections)
     * but we continue to support it for legacy versions.
     *
     * For AS7 new versions, we compute its value based on the presence of cluster-connection children and ignore any write-attribute operation on it.
     * We only warn the user if he wants to disable the clustered state of the server by setting it to false.
     */
    private static final class ClusteredAttributeHandlers {
        static final OperationStepHandler READ_HANDLER = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                boolean clustered = isClustered(context);
                context.getResult().set(clustered);
            }
        };

        static final OperationStepHandler WRITE_HANDLER = new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                // the real clustered HornetQ state
                boolean clustered = isClustered(context);
                // whether the user wants the server to be clustered
                ModelNode mock = new ModelNode();
                mock.get(CLUSTERED.getName()).set(operation.get(ModelDescriptionConstants.VALUE));
                boolean wantsClustered = CLUSTERED.resolveModelAttribute(context, mock).asBoolean();
                if (clustered && !wantsClustered) {
                    PathAddress serverAddress = context.getCurrentAddress();
                    MessagingLogger.ROOT_LOGGER.warn(MessagingLogger.ROOT_LOGGER.canNotChangeClusteredAttribute(serverAddress));
                }
                // ignore the operation
            }
        };

        private static boolean isClustered(OperationContext context) {
            Set<String> clusterConnectionNames = context.readResource(PathAddress.EMPTY_ADDRESS).getChildrenNames(ClusterConnectionDefinition.PATH.getKey());
            return !clusterConnectionNames.isEmpty();
        }
    }

}
