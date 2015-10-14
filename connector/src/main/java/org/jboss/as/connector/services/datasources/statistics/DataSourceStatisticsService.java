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

package org.jboss.as.connector.services.datasources.statistics;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import org.jboss.as.connector.dynamicresource.StatisticsResourceDefinition;
import org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

public class DataSourceStatisticsService implements Service<ManagementResourceRegistration> {


    private static final PathElement JDBC_STATISTICS = PathElement.pathElement("statistics", "jdbc");
    private static final PathElement POOL_STATISTICS = PathElement.pathElement("statistics", "pool");

    private final ManagementResourceRegistration registration;
    private final boolean statsEnabled;

    protected final InjectedValue<CommonDeployment> injectedDeploymentMD = new InjectedValue<>();


    /**
     * create an instance *
     */
    public DataSourceStatisticsService(final ManagementResourceRegistration registration,
                                       final boolean statsEnabled) {
        super();
        this.registration = registration;
        this.statsEnabled = statsEnabled;

    }


    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting DataSourceStatisticsService");
        synchronized (JDBC_STATISTICS) {
            CommonDeployment deploymentMD = injectedDeploymentMD.getValue();


            StatisticsPlugin jdbcStats = deploymentMD.getDataSources()[0].getStatistics();
            StatisticsPlugin poolStats = deploymentMD.getDataSources()[0].getPool().getStatistics();
            jdbcStats.setEnabled(statsEnabled);
            poolStats.setEnabled(statsEnabled);

            int jdbcStatsSize = jdbcStats.getNames().size();
            int poolStatsSize = poolStats.getNames().size();
            if (jdbcStatsSize > 0 || poolStatsSize > 0) {
                if (registration != null) {
                    if (jdbcStatsSize > 0) {
                        if (registration.getSubModel(PathAddress.pathAddress(JDBC_STATISTICS)) == null) {
                            // TODO WFLY-5285 get rid of redundant .setRuntimeOnly once WFCORE-959 is integrated
                            ManagementResourceRegistration jdbcRegistration = registration.registerSubModel(new StatisticsResourceDefinition(JDBC_STATISTICS, DataSourcesSubsystemProviders.RESOURCE_NAME, jdbcStats));
                            jdbcRegistration.setRuntimeOnly(true);
                        }
                    }

                    if (poolStatsSize > 0) {
                        if (registration.getSubModel(PathAddress.pathAddress(POOL_STATISTICS)) == null) {
                            // TODO WFLY-5285 get rid of redundant .setRuntimeOnly once WFCORE-959 is integrated
                            ManagementResourceRegistration poolRegistration = registration.registerSubModel(new StatisticsResourceDefinition(POOL_STATISTICS, DataSourcesSubsystemProviders.RESOURCE_NAME, poolStats));
                            poolRegistration.setRuntimeOnly(true);
                        }
                    }
                }

            }
        }
    }

    @Override
    public void stop(StopContext context) {
        synchronized (JDBC_STATISTICS) {
            if (registration != null) {
                registration.unregisterSubModel(JDBC_STATISTICS);
                registration.unregisterSubModel(POOL_STATISTICS);
            }
        }
    }


    @Override
    public ManagementResourceRegistration getValue() throws IllegalStateException, IllegalArgumentException {
        return registration;
    }

    public Injector<CommonDeployment> getCommonDeploymentInjector() {
        return injectedDeploymentMD;
    }


    public static void registerStatisticsResources(Resource datasourceResource) {
        synchronized (JDBC_STATISTICS) {
            if (!datasourceResource.hasChild(JDBC_STATISTICS)) {
                datasourceResource.registerChild(JDBC_STATISTICS, new PlaceholderResource.PlaceholderResourceEntry(JDBC_STATISTICS));
            }
            if (!datasourceResource.hasChild(POOL_STATISTICS)) {
                datasourceResource.registerChild(POOL_STATISTICS, new PlaceholderResource.PlaceholderResourceEntry(POOL_STATISTICS));
            }
        }
    }

    public static void removeStatisticsResources(Resource datasourceResource) {
        synchronized (JDBC_STATISTICS) {
            if (datasourceResource.hasChild(JDBC_STATISTICS)) {
                datasourceResource.removeChild(JDBC_STATISTICS);
            }
            if (datasourceResource.hasChild(POOL_STATISTICS)) {
                datasourceResource.removeChild(POOL_STATISTICS);
            }
        }
    }

}
