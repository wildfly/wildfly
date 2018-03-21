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
