/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.datasources;

import org.jboss.as.connector.dynamicresource.descriptionproviders.StatisticsDescriptionProvider;
import org.jboss.as.connector.dynamicresource.operations.ClearStatisticsHandler;
import org.jboss.as.connector.subsystems.common.pool.PoolMetrics;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;

/**
*
 * Listener that registers data source statistics with the management model
 *
*/
public class DataSourceStatisticsListener extends AbstractServiceListener<Object> {

    private static final PathElement JDBC_STATISTICS = PathElement.pathElement("statistics", "jdbc");
    private static final PathElement POOL_STATISTICS = PathElement.pathElement("statistics", "pool");

    private final ManagementResourceRegistration registration;
    private final Resource resource;
    private final String dsName;

    public DataSourceStatisticsListener(final ManagementResourceRegistration registration, Resource resource, final String dsName) {
        this.registration = registration;
        this.resource = resource;
        this.dsName = dsName;
    }

    public void transition(final ServiceController<? extends Object> controller,
                           final ServiceController.Transition transition) {

        switch (transition) {
            case STARTING_to_UP: {

                CommonDeployment deploymentMD = ((AbstractDataSourceService) controller.getService()).getDeploymentMD();

                StatisticsPlugin jdbcStats = deploymentMD.getDataSources()[0].getStatistics();
                StatisticsPlugin poolStats = deploymentMD.getDataSources()[0].getPool().getStatistics();
                int jdbcStatsSize = jdbcStats.getNames().size();
                int poolStatsSize = poolStats.getNames().size();
                if (jdbcStatsSize > 0 || poolStatsSize > 0) {
                    ManagementResourceRegistration subRegistration = registration.isAllowsOverride() ? registration.registerOverrideModel(dsName, DataSourcesSubsystemProviders.OVERRIDE_DS_DESC) : registration;

                    if (jdbcStatsSize > 0) {
                        ManagementResourceRegistration jdbcRegistration = subRegistration.registerSubModel(JDBC_STATISTICS, new StatisticsDescriptionProvider(DataSourcesSubsystemProviders.RESOURCE_NAME, "statistics", jdbcStats));
                        jdbcRegistration.setRuntimeOnly(true);
                        jdbcRegistration.registerOperationHandler(Constants.CLEAR_STATISTICS, new ClearStatisticsHandler(jdbcStats));

                        for (String statName : jdbcStats.getNames()) {
                            jdbcRegistration.registerMetric(statName, new PoolMetrics.ParametrizedPoolMetricsHandler(jdbcStats));
                        }

                        resource.registerChild(JDBC_STATISTICS, new PlaceholderResource.PlaceholderResourceEntry(JDBC_STATISTICS));
                    }

                    if (poolStatsSize > 0) {
                        ManagementResourceRegistration poolRegistration = subRegistration.registerSubModel(POOL_STATISTICS, new StatisticsDescriptionProvider(DataSourcesSubsystemProviders.RESOURCE_NAME, "statistics", poolStats));
                        poolRegistration.setRuntimeOnly(true);
                        poolRegistration.registerOperationHandler(Constants.CLEAR_STATISTICS, new ClearStatisticsHandler(poolStats));

                        for (String statName : poolStats.getNames()) {
                            poolRegistration.registerMetric(statName, new PoolMetrics.ParametrizedPoolMetricsHandler(poolStats));
                        }

                        resource.registerChild(POOL_STATISTICS, new PlaceholderResource.PlaceholderResourceEntry(JDBC_STATISTICS));
                    }
                }
                break;


            }
            case UP_to_STOP_REQUESTED: {

                ManagementResourceRegistration subRegistration = registration.getOverrideModel(dsName);
                if (subRegistration != null) {
                    subRegistration.unregisterSubModel(JDBC_STATISTICS);
                    subRegistration.unregisterSubModel(POOL_STATISTICS);
                    registration.unregisterOverrideModel(dsName);
                }

                if (resource.hasChild(JDBC_STATISTICS)) {
                    resource.removeChild(JDBC_STATISTICS);
                }

                if (resource.hasChild(POOL_STATISTICS)) {
                    resource.removeChild(POOL_STATISTICS);
                }
                break;

            }
        }
    }
}
