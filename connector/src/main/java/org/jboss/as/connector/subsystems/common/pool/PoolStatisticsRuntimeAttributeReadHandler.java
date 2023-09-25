/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.common.pool;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;

public class PoolStatisticsRuntimeAttributeReadHandler implements OperationStepHandler {

    private final StatisticsPlugin stats;

    public PoolStatisticsRuntimeAttributeReadHandler(StatisticsPlugin stats) {
        this.stats = stats;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final String attributeName = operation.require(NAME).asString();
                    try {
                        final ModelNode result = context.getResult();

                        switch (attributeName) {

                            case ModelDescriptionConstants.STATISTICS_ENABLED: {
                                result.set(stats.isEnabled());
                                break;
                            }
                        }

                    } catch (Exception e) {
                        throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToGetMetrics(e.getLocalizedMessage()));
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

}
