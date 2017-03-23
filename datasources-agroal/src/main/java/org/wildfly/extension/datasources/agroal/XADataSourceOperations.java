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
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Operations for adding and removing an xa-datasource resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class XADataSourceOperations extends AbstractAddStepHandler {

    static final String XADATASOURCE_SERVICE_NAME = "xa-datasource";

    static final ServiceName XADATASOURCE_SERVICE_PREFIX = AgroalExtension.BASE_SERVICE_NAME.append(XADATASOURCE_SERVICE_NAME);

    // --- //

    static final OperationStepHandler ADD_OPERATION = new XADataSourceAdd();

    static final OperationStepHandler REMOVE_OPERATION = new XADataSourceRemove();

    // --- //

    private static class XADataSourceAdd extends AbstractAddStepHandler {

        private XADataSourceAdd() {
            super(XADataSourceDefinition.DATA_SOURCE_CAPABILITY, XADataSourceDefinition.ATTRIBUTES);
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

            DataSourceService dataSourceService = new DataSourceService(datasourceName, jndiName, false, false, true, dataSourceConfiguration);

            ServiceBuilder<AgroalDataSource> serviceBuilder = context.getServiceTarget().addService(XADATASOURCE_SERVICE_PREFIX.append(datasourceName), dataSourceService);

            AbstractDataSourceOperations.setupElytronSecurity(context, factoryModel, dataSourceService, serviceBuilder);

            serviceBuilder.addDependency(DriverOperations.DRIVER_SERVICE_PREFIX.append(driverName), Class.class, dataSourceService.getDriverInjector());
            serviceBuilder.install();
        }
    }

    // --- //

    private static class XADataSourceRemove extends AbstractRemoveStepHandler {

        private XADataSourceRemove(){
            super(XADataSourceDefinition.DATA_SOURCE_CAPABILITY);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String datasourceName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
            ServiceName datasourceServiceName = ServiceName.of(XADATASOURCE_SERVICE_PREFIX, datasourceName);
            context.removeService(datasourceServiceName);
        }
    }
}
