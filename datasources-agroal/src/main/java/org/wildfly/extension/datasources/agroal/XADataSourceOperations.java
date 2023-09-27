/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.datasources.agroal;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import io.agroal.api.AgroalDataSource;
import jakarta.transaction.TransactionSynchronizationRegistry;

import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.credential.source.CredentialSource;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Operations for adding and removing an xa-datasource resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class XADataSourceOperations extends AbstractAddStepHandler {

    static final String XADATASOURCE_SERVICE_NAME = "xa-datasource";

    // --- //

    static final OperationStepHandler ADD_OPERATION = new XADataSourceAdd();

    static final OperationStepHandler REMOVE_OPERATION = new XADataSourceRemove();

    // --- //

    private static class XADataSourceAdd extends AbstractAddStepHandler {

        private XADataSourceAdd() {
            super(XADataSourceDefinition.ATTRIBUTES);
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String datasourceName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();

            ModelNode factoryModel = AbstractDataSourceDefinition.CONNECTION_FACTORY_ATTRIBUTE.resolveModelAttribute(context, model);
            AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration = AbstractDataSourceOperations.connectionFactoryConfiguration(context, factoryModel);

            ModelNode poolModel = AbstractDataSourceDefinition.CONNECTION_POOL_ATTRIBUTE.resolveModelAttribute(context, model);
            AgroalConnectionPoolConfigurationSupplier connectionPoolConfiguration = AbstractDataSourceOperations.connectionPoolConfiguration(context, poolModel);
            connectionPoolConfiguration.connectionFactoryConfiguration(connectionFactoryConfiguration);

            AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();
            dataSourceConfiguration.connectionPoolConfiguration(connectionPoolConfiguration);
            dataSourceConfiguration.metricsEnabled(AbstractDataSourceDefinition.STATISTICS_ENABLED_ATTRIBUTE.resolveModelAttribute(context, model).asBoolean());

            String jndiName = AbstractDataSourceDefinition.JNDI_NAME_ATTRIBUTE.resolveModelAttribute(context, model).asString();
            String driverName = AbstractDataSourceDefinition.DRIVER_ATTRIBUTE.resolveModelAttribute(context, factoryModel).asString();
            CapabilityServiceBuilder serviceBuilder = context.getCapabilityServiceTarget().addCapability(AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.fromBaseCapability(datasourceName));
            final Consumer<AgroalDataSource> consumer = serviceBuilder.provides(AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.fromBaseCapability(datasourceName));
            final Supplier<Class> driverSupplier = serviceBuilder.requiresCapability(DriverDefinition.AGROAL_DRIVER_CAPABILITY.getDynamicName(driverName), Class.class);
            final Supplier<AuthenticationContext> authenticationContextSupplier = AbstractDataSourceOperations.setupAuthenticationContext(context, factoryModel, serviceBuilder);
            final Supplier<ExceptionSupplier<CredentialSource, Exception>> credentialSourceSupplier = AbstractDataSourceOperations.setupCredentialReference(context, factoryModel, serviceBuilder);
            // TODO add a Stage.MODEL requirement
            final Supplier<TransactionSynchronizationRegistry> txnRegistrySupplier = serviceBuilder.requiresCapability("org.wildfly.transactions.transaction-synchronization-registry", TransactionSynchronizationRegistry.class);
            DataSourceService dataSourceService = new DataSourceService(consumer, driverSupplier, authenticationContextSupplier, credentialSourceSupplier, txnRegistrySupplier, datasourceName, jndiName, false, false, true, dataSourceConfiguration);
            serviceBuilder.setInstance(dataSourceService);
            serviceBuilder.install();
        }
    }

    // --- //

    private static class XADataSourceRemove extends AbstractRemoveStepHandler {

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String datasourceName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
            ServiceName datasourceServiceName = AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.getCapabilityServiceName(datasourceName);
            context.removeService(datasourceServiceName);
        }
    }
}
