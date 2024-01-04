/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;
import static org.wildfly.extension.datasources.agroal.AbstractDataSourceDefinition.CREDENTIAL_REFERENCE;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import jakarta.transaction.TransactionSynchronizationRegistry;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.credential.source.CredentialSource;

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
            final CapabilityServiceBuilder serviceBuilder = context.getCapabilityServiceTarget().addCapability(AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.fromBaseCapability(datasourceName));
            final Consumer<AgroalDataSource> consumer = serviceBuilder.provides(AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.fromBaseCapability(datasourceName));
            final Supplier<Class> driverSupplier = serviceBuilder.requiresCapability(DriverDefinition.AGROAL_DRIVER_CAPABILITY.getDynamicName(driverName), Class.class);
            final Supplier<AuthenticationContext> authenticationContextSupplier = AbstractDataSourceOperations.setupAuthenticationContext(context, factoryModel, serviceBuilder);
            final Supplier<ExceptionSupplier<CredentialSource, Exception>> credentialSourceSupplier = AbstractDataSourceOperations.setupCredentialReference(context, factoryModel, serviceBuilder);
            // TODO add a Stage.MODEL requirement
            final Supplier<TransactionSynchronizationRegistry> txnRegistrySupplier = jta ? serviceBuilder.requiresCapability("org.wildfly.transactions.transaction-synchronization-registry", TransactionSynchronizationRegistry.class) : null;
            DataSourceService dataSourceService = new DataSourceService(consumer, driverSupplier, authenticationContextSupplier, credentialSourceSupplier, txnRegistrySupplier, datasourceName, jndiName, jta, connectable, false, dataSourceConfiguration);
            serviceBuilder.setInstance(dataSourceService);
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
