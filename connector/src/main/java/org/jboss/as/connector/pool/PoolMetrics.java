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

package org.jboss.as.connector.pool;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.jboss.as.connector.ConnectorMessages.MESSAGES;
import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.management.Connector;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPoolStatisticsImpl;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public abstract class PoolMetrics implements OperationStepHandler {

    static final String[] NO_LOCATION = new String[0];

    public static final Set<String> ATTRIBUTES =  (new ManagedConnectionPoolStatisticsImpl(1)).getNames();

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
                    final String jndiName = address.getLastElement().getValue();
                    final String attributeName = operation.require(NAME).asString();

                    final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                            ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE);
                    if (managementRepoService != null) {
                        try {
                            final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
                            final ModelNode result = context.getResult();
                            List<StatisticsPlugin> stats = getMatchingStats(jndiName, repository);
                            for (StatisticsPlugin stat : stats) {

                                result.set("" + stat.getValue(attributeName));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new OperationFailedException(new ModelNode().set(MESSAGES.failedToGetMetrics(e.getLocalizedMessage())));
                        }
                    }
                   context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        } else {
            context.getResult().set(MESSAGES.noMetricsAvailable());
        }

        context.completeStep();
    }

    protected abstract List<StatisticsPlugin> getMatchingStats(String jndiName, ManagementRepository repository);

    public static class LocalAndXaDataSourcePoolMetricsHandler extends PoolMetrics {
        public static LocalAndXaDataSourcePoolMetricsHandler INSTANCE = new LocalAndXaDataSourcePoolMetricsHandler();

        protected List<StatisticsPlugin> getMatchingStats(String jndiName, ManagementRepository repository) {
            ArrayList<StatisticsPlugin> result = new ArrayList<StatisticsPlugin>(repository.getDataSources().size());
            if (repository.getDataSources() != null) {
                for (DataSource ds : repository.getDataSources()) {
                    if (jndiName.equalsIgnoreCase(ds.getJndiName()) && ds.getPool() != null) {
                        result.add(ds.getPool().getStatistics());
                    }

                }
            }
            result.trimToSize();
            return result;
        }

    }

    public static class RaPoolMetricsHandler extends PoolMetrics {
        public static RaPoolMetricsHandler INSTANCE = new RaPoolMetricsHandler();

        protected List<StatisticsPlugin> getMatchingStats(String jndiName, ManagementRepository repository) {
            ArrayList<StatisticsPlugin> result = new ArrayList<StatisticsPlugin>(repository.getConnectors().size());
            if (repository.getConnectors() != null) {
                for (Connector c : repository.getConnectors()) {
                    if (jndiName.equalsIgnoreCase(c.getUniqueId())) {
                        if (c.getConnectionFactories() == null || c.getConnectionFactories().get(0) == null
                                || c.getConnectionFactories().get(0).getPool() == null)
                            continue;
                        result.add(c.getConnectionFactories().get(0).getPool().getStatistics());
                    }

                }
            }
            result.trimToSize();
            return result;
        }

    }

}
