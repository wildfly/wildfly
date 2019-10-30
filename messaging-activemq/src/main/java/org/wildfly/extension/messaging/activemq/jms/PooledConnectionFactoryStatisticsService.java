/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

import org.jboss.as.connector.dynamicresource.StatisticsResourceDefinition;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
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

/**
 * Service providing statistics for the pooled-connection-factory's pool.
 *
 * copied from {@link org.jboss.as.connector.services.datasources.statistics.DataSourceStatisticsService}
 */
public class PooledConnectionFactoryStatisticsService implements Service<ManagementResourceRegistration> {

    private static final PathElement POOL_STATISTICS = PathElement.pathElement("statistics", "pool");

    private final ManagementResourceRegistration registration;
    private final boolean statsEnabled;

    protected final InjectedValue<ResourceAdapterDeployment> injectedRADeployment = new InjectedValue<>();


    /**
     * create an instance *
     */
    public PooledConnectionFactoryStatisticsService(final ManagementResourceRegistration registration,
                                                    final boolean statsEnabled) {
        super();
        this.registration = registration;
        this.statsEnabled = statsEnabled;

    }


    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("start PooledConnectionFactoryStatisticsService");
        synchronized (POOL_STATISTICS) {
            ResourceAdapterDeployment raDeployment = injectedRADeployment.getValue();
            CommonDeployment deployment = raDeployment.getDeployment();


            StatisticsPlugin poolStats = deployment.getConnectionManagers()[0].getPool().getStatistics();
            poolStats.setEnabled(statsEnabled);

            int poolStatsSize = poolStats.getNames().size();
            if (poolStatsSize > 0) {
                if (registration != null) {
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
        synchronized (POOL_STATISTICS) {
            if (registration != null) {
                registration.unregisterSubModel(POOL_STATISTICS);
            }
        }
    }


    @Override
    public ManagementResourceRegistration getValue() throws IllegalStateException, IllegalArgumentException {
        return registration;
    }

    public Injector<ResourceAdapterDeployment> getRADeploymentInjector() {
        return injectedRADeployment;
    }


    public static void registerStatisticsResources(Resource resource) {
        synchronized (POOL_STATISTICS) {
            if (!resource.hasChild(POOL_STATISTICS)) {
                resource.registerChild(POOL_STATISTICS, new PlaceholderResource.PlaceholderResourceEntry(POOL_STATISTICS));
            }
        }
    }

    public static void removeStatisticsResources(Resource resource) {
        synchronized (POOL_STATISTICS) {
            if (resource.hasChild(POOL_STATISTICS)) {
                resource.removeChild(POOL_STATISTICS);
            }
        }
    }

}
