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

package org.jboss.as.txn.subsystem;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.arjuna.ats.arjuna.coordinator.TxStats;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.txn.logging.TransactionLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler for transaction manager metrics
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TxStatsHandler extends AbstractRuntimeOnlyHandler {

    public enum TxStat {

        NUMBER_OF_TRANSACTIONS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_TRANSACTIONS, ModelType.LONG, true).build()),
        NUMBER_OF_NESTED_TRANSACTIONS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_NESTED_TRANSACTIONS, ModelType.LONG, true).build()),
        NUMBER_OF_HEURISTICS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_HEURISTICS, ModelType.LONG, true).build()),
        NUMBER_OF_COMMITTED_TRANSACTIONS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_COMMITTED_TRANSACTIONS, ModelType.LONG, true).build()),
        NUMBER_OF_ABORTED_TRANSACTIONS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_ABORTED_TRANSACTIONS, ModelType.LONG, true).build()),
        NUMBER_OF_INFLIGHT_TRANSACTIONS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_INFLIGHT_TRANSACTIONS, ModelType.LONG, true).build()),
        NUMBER_OF_TIMED_OUT_TRANSACTIONS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_TIMED_OUT_TRANSACTIONS, ModelType.LONG, true).build()),
        NUMBER_OF_APPLICATION_ROLLBACKS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_APPLICATION_ROLLBACKS, ModelType.LONG, true).build()),
        NUMBER_OF_RESOURCE_ROLLBACKS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_RESOURCE_ROLLBACKS, ModelType.LONG, true).build()),
        NUMBER_OF_SYSTEM_ROLLBACKS(SimpleAttributeDefinitionBuilder.create(CommonAttributes.NUMBER_OF_SYSTEM_ROLLBACKS, ModelType.LONG, true).build()),
        AVERAGE_COMMIT_TIME(SimpleAttributeDefinitionBuilder.create(CommonAttributes.AVERAGE_COMMIT_TIME, ModelType.LONG, true).build());

        private static final Map<String, TxStat> MAP = new HashMap<String, TxStat>();
        static {
            for (TxStat stat : EnumSet.allOf(TxStat.class)) {
                MAP.put(stat.toString(), stat);
            }
        }
        final AttributeDefinition definition;
        private TxStat(final AttributeDefinition definition) {
            this.definition = definition;
        }

        @Override
        public final String toString() {
            return definition.getName();
        }

        public static synchronized TxStat getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    public static final TxStatsHandler INSTANCE = new  TxStatsHandler();

    private final TxStats txStats = TxStats.getInstance();

    private TxStatsHandler() {
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        TxStat stat = TxStat.getStat(operation.require(ModelDescriptionConstants.NAME).asString());
        if (stat == null) {
            context.getFailureDescription().set(TransactionLogger.ROOT_LOGGER.unknownMetric(operation.require(ModelDescriptionConstants.NAME).asString()));
        }
        else {
            ModelNode result = new ModelNode();
            switch (stat) {
                case NUMBER_OF_TRANSACTIONS:
                    result.set(txStats.getNumberOfTransactions());
                    break;
                case NUMBER_OF_NESTED_TRANSACTIONS:
                    result.set(txStats.getNumberOfNestedTransactions());
                    break;
                case NUMBER_OF_HEURISTICS:
                    result.set(txStats.getNumberOfHeuristics());
                    break;
                case NUMBER_OF_COMMITTED_TRANSACTIONS:
                    result.set(txStats.getNumberOfCommittedTransactions());
                    break;
                case NUMBER_OF_ABORTED_TRANSACTIONS:
                    result.set(txStats.getNumberOfAbortedTransactions());
                    break;
                case NUMBER_OF_INFLIGHT_TRANSACTIONS:
                    result.set(txStats.getNumberOfInflightTransactions());
                    break;
                case NUMBER_OF_TIMED_OUT_TRANSACTIONS:
                    result.set(txStats.getNumberOfTimedOutTransactions());
                    break;
                case NUMBER_OF_APPLICATION_ROLLBACKS:
                    result.set(txStats.getNumberOfApplicationRollbacks());
                    break;
                case NUMBER_OF_RESOURCE_ROLLBACKS:
                    result.set(txStats.getNumberOfResourceRollbacks());
                    break;
                case NUMBER_OF_SYSTEM_ROLLBACKS:
                    result.set(txStats.getNumberOfSystemRollbacks());
                    break;
                case AVERAGE_COMMIT_TIME:
                    result.set(txStats.getAverageCommitTime());
                    break;
                default:
                    throw new IllegalStateException(TransactionLogger.ROOT_LOGGER.unknownMetric(stat));
            }
            context.getResult().set(result);
        }
   }

    void registerMetrics(final ManagementResourceRegistration resourceRegistration) {
        for (TxStat stat : TxStat.values()) {
            resourceRegistration.registerMetric(stat.definition, this);
        }
    }
}
