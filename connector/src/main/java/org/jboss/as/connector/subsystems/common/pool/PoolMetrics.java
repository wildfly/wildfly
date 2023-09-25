/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.common.pool;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public abstract class PoolMetrics implements OperationStepHandler {

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                    final String jndiName = address.getLastElement().getValue();
                    final String attributeName = operation.require(NAME).asString();

                    final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                            ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE);
                    if (managementRepoService != null) {
                        try {
                            final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                            final ModelNode result = context.getResult();
                            List<StatisticsPlugin> stats = getMatchingStats(jndiName, repository);
                            for (StatisticsPlugin stat : stats) {

                                result.set("" + stat.getValue(attributeName));
                            }
                        } catch (Exception e) {
                            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToGetMetrics(e.getLocalizedMessage()));
                        }
                    }
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }

    protected abstract List<StatisticsPlugin> getMatchingStats(String jndiName, ManagementRepository repository);

    public static class ParametrizedPoolMetricsHandler implements OperationStepHandler {

        private final StatisticsPlugin stats;

        public ParametrizedPoolMetricsHandler(StatisticsPlugin stats) {
            this.stats = stats;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (context.isNormalServer()) {
                context.addStep(new OperationStepHandler() {
                    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                        final String attributeName = operation.require(NAME).asString();

                        final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                                ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE);
                        if (managementRepoService != null) {
                            try {
                                setModelValue(context.getResult(), attributeName);
                            } catch (Exception e) {
                               throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToGetMetrics(e.getLocalizedMessage()));
                            }
                        }
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        }

        private void setModelValue(ModelNode result, String attributeName) {
            if (stats.getType(attributeName) == int.class) {
                result.set((Integer) stats.getValue(attributeName));
            } else if (stats.getType(attributeName) == long.class) {
                result.set((Long) stats.getValue(attributeName));
            } else {
                result.set("" + stats.getValue(attributeName));
            }
        }
    }

}
