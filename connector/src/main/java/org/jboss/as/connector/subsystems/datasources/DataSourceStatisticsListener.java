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

import org.jboss.as.connector.dynamicresource.StatisticsResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;

/**
 * Listener that registers data source statistics with the management model
 */
public class DataSourceStatisticsListener extends AbstractServiceListener<Object> {

    private static final PathElement JDBC_STATISTICS = PathElement.pathElement("statistics", "jdbc");
    private static final PathElement POOL_STATISTICS = PathElement.pathElement("statistics", "pool");

    private final ManagementResourceRegistration overrideRegistration;
    private final Resource resource;
    private final String dsName;
    private final boolean statsEnabled;

    public DataSourceStatisticsListener(final ManagementResourceRegistration overrideRegistration, Resource resource, final String dsName, final boolean statsEnabled) {
        this.overrideRegistration = overrideRegistration;
        this.resource = resource;
        this.dsName = dsName;
        this.statsEnabled = statsEnabled;
    }

    public void transition(final ServiceController<? extends Object> controller,
                           final ServiceController.Transition transition) {

        switch (transition) {
            case STARTING_to_UP: {

                CommonDeployment deploymentMD = ((AbstractDataSourceService) controller.getService()).getDeploymentMD();

                StatisticsPlugin jdbcStats = deploymentMD.getDataSources()[0].getStatistics();
                StatisticsPlugin poolStats = deploymentMD.getDataSources()[0].getPool().getStatistics();
                jdbcStats.setEnabled(statsEnabled);
                poolStats.setEnabled(statsEnabled);

                int jdbcStatsSize = jdbcStats.getNames().size();
                int poolStatsSize = poolStats.getNames().size();
                if (jdbcStatsSize > 0 || poolStatsSize > 0) {
                    if (overrideRegistration != null) {
                        if (jdbcStatsSize > 0) {
                            ManagementResourceRegistration jdbcRegistration = overrideRegistration.registerSubModel(new StatisticsResourceDefinition(JDBC_STATISTICS,DataSourcesSubsystemProviders.RESOURCE_NAME, jdbcStats));
                            jdbcRegistration.setRuntimeOnly(true);
                            resource.registerChild(JDBC_STATISTICS, new PlaceholderResource.PlaceholderResourceEntry(JDBC_STATISTICS));
                        }

                        if (poolStatsSize > 0) {
                            ManagementResourceRegistration poolRegistration = overrideRegistration.registerSubModel(new StatisticsResourceDefinition(POOL_STATISTICS, DataSourcesSubsystemProviders.RESOURCE_NAME, poolStats));
                            poolRegistration.setRuntimeOnly(true);
                            resource.registerChild(POOL_STATISTICS, new PlaceholderResource.PlaceholderResourceEntry(JDBC_STATISTICS));
                        }
                    }
                }
                break;


            }
            case UP_to_STOP_REQUESTED: {

                if (overrideRegistration != null) {
                    overrideRegistration.unregisterSubModel(JDBC_STATISTICS);
                    overrideRegistration.unregisterSubModel(POOL_STATISTICS);
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
