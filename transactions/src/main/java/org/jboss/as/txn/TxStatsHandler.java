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

package org.jboss.as.txn;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.arjuna.ats.arjuna.coordinator.TxStats;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;

/**
 * Handler for transaction manager metrics
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TxStatsHandler implements OperationStepHandler {

    public enum TxStat {

        NUMBER_OF_TRANSACTIONS(CommonAttributes.NUMBER_OF_TRANSACTIONS),
        NUMBER_OF_NESTED_TRANSACTIONS(CommonAttributes.NUMBER_OF_NESTED_TRANSACTIONS),
        NUMBER_OF_HEURISTICS(CommonAttributes.NUMBER_OF_HEURISTICS),
        NUMBER_OF_COMMITTED_TRANSACTIONS(CommonAttributes.NUMBER_OF_COMMITTED_TRANSACTIONS),
        NUMBER_OF_ABORTED_TRANSACTIONS(CommonAttributes.NUMBER_OF_ABORTED_TRANSACTIONS),
        NUMBER_OF_INFLIGHT_TRANSACTIONS(CommonAttributes.NUMBER_OF_INFLIGHT_TRANSACTIONS),
        NUMBER_OF_TIMED_OUT_TRANSACTIONS(CommonAttributes.NUMBER_OF_TIMED_OUT_TRANSACTIONS),
        NUMBER_OF_APPLICATION_ROLLBACKS(CommonAttributes.NUMBER_OF_APPLICATION_ROLLBACKS),
        NUMBER_OF_RESOURCE_ROLLBACKS(CommonAttributes.NUMBER_OF_RESOURCE_ROLLBACKS);

        private static final Map<String, TxStat> MAP = new HashMap<String, TxStat>();
        static {
            for (TxStat stat : EnumSet.allOf(TxStat.class)) {
                MAP.put(stat.toString(), stat);
            }
        }
        private String stringForm;
        private TxStat(final String stringForm) {
            this.stringForm = stringForm;
        }

        @Override
        public final String toString() {
            return stringForm;
        }

        public static synchronized TxStat getStat(final String stringForm) {
            return MAP.get(stringForm);
        }
    }

    public static final TxStatsHandler INSTANCE = new  TxStatsHandler();

    private final TxStats txStats = TxStats.getInstance();

    private final ParametersValidator validator = new ParametersValidator();

    private TxStatsHandler() {
        validator.registerValidator(ModelDescriptionConstants.NAME, new StringLengthValidator(1));
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validator.validate(operation);

        TxStat stat = TxStat.getStat(operation.require(ModelDescriptionConstants.NAME).asString());
        if (stat == null) {
            context.getFailureDescription().set(String.format("Unknown metric %s", operation.require(ModelDescriptionConstants.NAME).asString()));
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
                default:
                    throw new IllegalStateException(String.format("Unknown metric %s", stat));
            }
            context.getResult().set(result);
        }

        context.completeStep();
    }
}
