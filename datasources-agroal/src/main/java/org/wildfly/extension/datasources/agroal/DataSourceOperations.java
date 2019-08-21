/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal;

import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;
import static org.wildfly.extension.datasources.agroal.AbstractDataSourceDefinition.CREDENTIAL_REFERENCE;

import javax.transaction.TransactionSynchronizationRegistry;

import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Operations for adding and removing a datasource resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class DataSourceOperations {

    static final String DATASOURCE_SERVICE_NAME = "datasource";

    // --- //

    static final OperationStepHandler ADD_OPERATION = new DataSourceAdd();

    static final OperationStepHandler REMOVE_OPERATION = new DataSourceRemove();

    // --- //

    private static class DataSourceAdd extends AbstractAddStepHandler {

        private DataSourceAdd() {
            super(DataSourceDefinition.ATTRIBUTES);
        }

        protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws  OperationFailedException {
            super.populateModel(context, operation, resource);
            handleCredentialReferenceUpdate(context, resource.getModel());
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String datasourceName = context.getCurrentAddressValue();

            ModelNode factoryModel = AbstractDataSourceDefinition.CONNECTION_FACTORY_ATTRIBUTE.resolveModelAttribute(context, model);
            AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration = AbstractDataSourceOperations.connectionFactoryConfiguration(context, factoryModel);

            ModelNode poolModel = AbstractDataSourceDefinition.CONNECTION_POOL_ATTRIBUTE.resolveModelAttribute(context, model);
            AgroalConnectionPoolConfigurationSupplier connectionPoolConfiguration = AbstractDataSourceOperations.connectionPoolConfiguration(context, poolModel);
            connectionPoolConfiguration.connectionFactoryConfiguration(connectionFactoryConfiguration);

            AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();
            dataSourceConfiguration.connectionPoolConfiguration(connectionPoolConfiguration);
            dataSourceConfiguration.metricsEnabled(AbstractDataSourceDefinition.STATISTICS_ENABLED_ATTRIBUTE.resolveModelAttribute(context, model).asBoolean());

            String jndiName = AbstractDataSourceDefinition.JNDI_NAME_ATTRIBUTE.resolveModelAttribute(context, model).asString();
            boolean jta = DataSourceDefinition.JTA_ATTRIBUTE.resolveModelAttribute(context, model).asBoolean();
            boolean connectable = DataSourceDefinition.CONNECTABLE_ATTRIBUTE.resolveModelAttribute(context, model).asBoolean();
            String driverName = AbstractDataSourceDefinition.DRIVER_ATTRIBUTE.resolveModelAttribute(context, factoryModel).asString();

            DataSourceService dataSourceService = new DataSourceService(datasourceName, jndiName, jta, connectable, false, dataSourceConfiguration);

            CapabilityServiceBuilder serviceBuilder = context.getCapabilityServiceTarget().addCapability(AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.fromBaseCapability(datasourceName))
                    .setInstance(dataSourceService)
                    .addCapabilityRequirement(DriverDefinition.AGROAL_DRIVER_CAPABILITY.getDynamicName(driverName), Class.class, dataSourceService.getDriverInjector());
            if (jta) {
                // TODO add a Stage.MODEL requirement
                serviceBuilder.addCapabilityRequirement("org.wildfly.transactions.transaction-synchronization-registry", TransactionSynchronizationRegistry.class, dataSourceService.getTransactionSynchronizationRegistryInjector());
            }
            AbstractDataSourceOperations.setupElytronSecurity(context, factoryModel, dataSourceService, serviceBuilder);

            serviceBuilder.install();
        }

        @Override
        protected void rollbackRuntime(OperationContext context, final ModelNode operation, final Resource resource) {
            rollbackCredentialStoreUpdate(CREDENTIAL_REFERENCE, context, resource);
        }
    }

    // --- //

    private static class DataSourceRemove extends AbstractRemoveStepHandler {

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String datasourceName = context.getCurrentAddressValue();
            ServiceName datasourceServiceName = AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.getCapabilityServiceName(datasourceName);
            context.removeService(datasourceServiceName);
        }
    }
}
