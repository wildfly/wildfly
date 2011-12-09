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

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.sql.Driver;
import java.util.List;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.StatisticsDescriptionProvider;
import org.jboss.as.connector.pool.PoolMetrics;
import org.jboss.as.connector.registry.DriverRegistry;
import org.jboss.as.connector.subsystems.ClearStatisticsHandler;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.statistics.StatisticsPlugin;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.SubjectFactory;

/**
 * Abstract operation handler responsible for adding a DataSource.
 *
 * @author John Bailey
 */
public abstract class AbstractDataSourceAdd extends AbstractAddStepHandler {

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {

        final ModelNode address = operation.require(OP_ADDR);
        final String dsName = PathAddress.pathAddress(address).getLastElement().getValue();
        final String jndiName = operation.hasDefined(JNDINAME.getName()) ? operation.get(JNDINAME.getName()).asString() : dsName;


        final ServiceTarget serviceTarget = context.getServiceTarget();

        boolean enabled = false;
                //!operation.hasDefined(ENABLED.getName()) || operation.get(ENABLED.getName()).asBoolean();

        ModelNode node = operation.require(DATASOURCE_DRIVER.getName());


        AbstractDataSourceService dataSourceService = createDataSourceService(dsName);

        final ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();

        final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
        final ServiceBuilder<?> dataSourceServiceBuilder = serviceTarget
                .addService(dataSourceServiceName, dataSourceService)
                .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                        dataSourceService.getTransactionIntegrationInjector())
                .addDependency(ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE, ManagementRepository.class,
                        dataSourceService.getmanagementRepositoryInjector())
                .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                        dataSourceService.getSubjectFactoryInjector())
                .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                        dataSourceService.getDriverRegistryInjector()).addDependency(NamingService.SERVICE_NAME);

        dataSourceServiceBuilder.addListener(new AbstractServiceListener<Object>() {
            public void transition(final ServiceController<? extends Object> controller,
                                   final ServiceController.Transition transition) {

                switch (transition) {
                    case STARTING_to_UP: {

                        CommonDeployment deploymentMD = ((AbstractDataSourceService) controller.getService()).getDeploymentMD();

                        StatisticsPlugin jdbcStats = deploymentMD.getDataSources()[0].getStatistics();
                        if (jdbcStats.getNames().size() != 0) {
                            ManagementResourceRegistration subRegistration = registration.registerSubModel(PathElement.pathElement("statistics", "jdbc"), new StatisticsDescriptionProvider(jdbcStats));

                            for (String statName : jdbcStats.getNames()) {
                                subRegistration.registerMetric(statName, new PoolMetrics.ParametrizedPoolMetricsHandler(jdbcStats));
                            }
                            subRegistration.registerOperationHandler("clear-metrics", new ClearStatisticsHandler(jdbcStats), DataSourcesSubsystemProviders.CLEAR_STATISTICS_DESC, false);

                        }

                        StatisticsPlugin poolStats = deploymentMD.getDataSources()[0].getPool().getStatistics();
                        if (poolStats.getNames().size() != 0) {
                            ManagementResourceRegistration subRegistration = registration.registerSubModel(PathElement.pathElement("statistics", "pool"), new StatisticsDescriptionProvider(poolStats));

                            for (String statName : poolStats.getNames()) {
                                subRegistration.registerMetric(statName, new PoolMetrics.ParametrizedPoolMetricsHandler(poolStats));
                            }
                            subRegistration.registerOperationHandler("clear-statistics", new ClearStatisticsHandler(poolStats), DataSourcesSubsystemProviders.CLEAR_STATISTICS_DESC, false);
                        }
                        break;


                    }
                    case UP_to_STOP_REQUESTED: {

                        CommonDeployment deploymentMD = ((AbstractDataSourceService) controller.getService()).getDeploymentMD();

                        StatisticsPlugin jdbcStats = deploymentMD.getDataSources()[0].getStatistics();
                        if (jdbcStats.getNames().size() != 0) {
                            registration.unregisterSubModel(PathElement.pathElement("statistics", "jdbc"));
                        }

                        StatisticsPlugin poolStats = deploymentMD.getDataSources()[0].getPool().getStatistics();
                        if (poolStats.getNames().size() != 0) {
                            registration.unregisterSubModel(PathElement.pathElement("statistics", "pool"));
                        }
                        break;

                    }
                }
            }
        });
        startConfigAndAddDependency(dataSourceServiceBuilder, dataSourceService, dsName, serviceTarget, operation, verificationHandler);

        final String driverName = node.asString();
        final ServiceName driverServiceName = ServiceName.JBOSS.append("jdbc-driver", driverName.replaceAll("\\.", "_"));
        if (driverServiceName != null) {
            dataSourceServiceBuilder.addDependency(driverServiceName, Driver.class,
                    dataSourceService.getDriverInjector());
        }

        dataSourceServiceBuilder.setInitialMode(ServiceController.Mode.NEVER);

        controllers.add(dataSourceServiceBuilder.install());

    }

    static String cleanupJavaContext(String jndiName) {
        String bindName;
        if (jndiName.startsWith("java:/")) {
            bindName = jndiName.substring(6);
        } else if(jndiName.startsWith("java:")) {
            bindName = jndiName.substring(5);
        } else {
            bindName = jndiName;
        }
        return bindName;
    }

    protected abstract void startConfigAndAddDependency(ServiceBuilder<?> dataSourceServiceBuilder,
            AbstractDataSourceService dataSourceService, String jndiName, ServiceTarget serviceTarget, final ModelNode operation, final ServiceVerificationHandler serviceVerificationHandler)
            throws OperationFailedException;

    protected abstract void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException;

    protected abstract AbstractDataSourceService createDataSourceService(final String jndiName) throws OperationFailedException;

    static void populateAddModel(final ModelNode operation, final ModelNode modelNode,
            final String connectionPropertiesProp, final SimpleAttributeDefinition[] attributes) throws OperationFailedException {
        if (operation.hasDefined(connectionPropertiesProp)) {

            for (Property property : operation.get(connectionPropertiesProp).asPropertyList()) {
                modelNode.get(connectionPropertiesProp, property.getName()).set(property.getValue().asString());
            }
        }
        for (final SimpleAttributeDefinition attribute : attributes) {
            attribute.validateAndSet(operation, modelNode);
        }
        //modelNode.get(ENABLED.getName()).set(false);

    }

}
