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

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.credential.source.CredentialSource;

import javax.transaction.TransactionSynchronizationRegistry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Operations for adding and removing an xa-datasource resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
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
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String datasourceName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();

            ModelNode factoryModel = AbstractDataSourceDefinition.CONNECTION_FACTORY_ATTRIBUTE.resolveModelAttribute(context, model);
            AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration = AbstractDataSourceOperations.connectionFactoryConfiguration(context, factoryModel);

            // setup recovery security
            String recoveryUsername = XADataSourceDefinition.RECOVERY_USERNAME_ATTRIBUTE.resolveModelAttribute(context, model).asStringOrNull();
            String recoveryPassword = XADataSourceDefinition.RECOVERY_PASSWORD_ATTRIBUTE.resolveModelAttribute(context, model).asStringOrNull();
            if (recoveryUsername != null) {
                connectionFactoryConfiguration.recoveryPrincipal(new NamePrincipal(recoveryUsername));
            }
            if (recoveryPassword != null) {
                connectionFactoryConfiguration.recoveryCredential(new SimplePassword(recoveryPassword));
            }

            ModelNode poolModel = AbstractDataSourceDefinition.CONNECTION_POOL_ATTRIBUTE.resolveModelAttribute(context, model);
            AgroalConnectionPoolConfigurationSupplier connectionPoolConfiguration = AbstractDataSourceOperations.connectionPoolConfiguration(context, poolModel);
            connectionPoolConfiguration.connectionFactoryConfiguration(connectionFactoryConfiguration);

            AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();
            dataSourceConfiguration.connectionPoolConfiguration(connectionPoolConfiguration);
            dataSourceConfiguration.metricsEnabled(AbstractDataSourceDefinition.STATISTICS_ENABLED_ATTRIBUTE.resolveModelAttribute(context, model).asBoolean());

            String jndiName = AbstractDataSourceDefinition.JNDI_NAME_ATTRIBUTE.resolveModelAttribute(context, model).asString();
            String driverName = AbstractDataSourceDefinition.DRIVER_ATTRIBUTE.resolveModelAttribute(context, factoryModel).asString();

            DataSourceService dataSourceService = new DataSourceService(datasourceName, jndiName, false, false, true, dataSourceConfiguration);

            CapabilityServiceBuilder<AgroalDataSource> serviceBuilder = context.getCapabilityServiceTarget().addCapability(AbstractDataSourceDefinition.DATA_SOURCE_CAPABILITY.fromBaseCapability(datasourceName), dataSourceService);
            serviceBuilder.addCapabilityRequirement(DriverDefinition.AGROAL_DRIVER_CAPABILITY.getDynamicName(driverName), Class.class, dataSourceService.getDriverInjector());
            // TODO add a Stage.MODEL requirement
            serviceBuilder.addCapabilityRequirement("org.wildfly.transactions.transaction-synchronization-registry", TransactionSynchronizationRegistry.class, dataSourceService.getTransactionSynchronizationRegistryInjector());

            if (XADataSourceDefinition.RECOVERY.resolveModelAttribute(context, model).asBoolean()) {
                serviceBuilder.addDependency(TxnServices.JBOSS_TXN_ARJUNA_RECOVERY_MANAGER, XAResourceRecoveryRegistry.class, dataSourceService.getRecoveryRegistryInjector());
            }

            AbstractDataSourceOperations.setupElytronSecurity(context, factoryModel, dataSourceService, serviceBuilder);

            // setup recovery security (elytron)
            String recoveryAuthenticationContextName = XADataSourceDefinition.RECOVERY_AUTHENTICATION_CONTEXT.resolveModelAttribute(context, model).asStringOrNull();
            if (recoveryAuthenticationContextName != null) {
                ServiceName recoveryAuthenticationContextCapability = context.getCapabilityServiceName(AbstractDataSourceDefinition.AUTHENTICATION_CONTEXT_CAPABILITY, recoveryAuthenticationContextName, AuthenticationContext.class);
                serviceBuilder.addDependency(recoveryAuthenticationContextCapability, AuthenticationContext.class, dataSourceService.getRecoveryAuthenticationContextInjector());
            }
            if (XADataSourceDefinition.RECOVERY_CREDENTIAL_REFERENCE.resolveModelAttribute(context, model).isDefined()) {
                ExceptionSupplier<CredentialSource, Exception> recoveryCredentialSourceSupplier = CredentialReference.getCredentialSourceSupplier(context, AbstractDataSourceDefinition.CREDENTIAL_REFERENCE, model, serviceBuilder);
                dataSourceService.getRecoveryCredentialSourceSupplierInjector().inject(recoveryCredentialSourceSupplier);
            }

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
