/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTY_VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a recovery-environment to the Transactions subsystem
 *
 */
public class XaDataSourcePropertyAdd extends AbstractAddStepHandler {

    public static final XaDataSourcePropertyAdd INSTANCE = new XaDataSourcePropertyAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode modelNode) throws OperationFailedException {
        XADATASOURCE_PROPERTY_VALUE.validateAndSet(operation, modelNode);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel) throws OperationFailedException {

        final String configPropertyValue = XADATASOURCE_PROPERTY_VALUE.resolveModelAttribute(context, recoveryEnvModel).asString();
        final ModelNode address = operation.require(OP_ADDR);
        PathAddress path = PathAddress.pathAddress(address);
        final String dsName = path.getElement(path.size() - 2).getValue();
        final String configPropertyName = PathAddress.pathAddress(address).getLastElement().getValue();

        ServiceName serviceName = XADataSourceConfigService.SERVICE_NAME_BASE.append(dsName).append("xa-datasource-properties").append(configPropertyName);
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceName dataSourceConfigServiceName = XADataSourceConfigService.SERVICE_NAME_BASE
                .append(dsName);
        final ServiceController<?> dataSourceConfigController = registry
                .getService(dataSourceConfigServiceName);
        if (dataSourceConfigController == null || !((XaDataSource) dataSourceConfigController.getValue()).isEnabled()) {


            final ServiceTarget serviceTarget = context.getServiceTarget();

            final XaDataSourcePropertiesService service = new XaDataSourcePropertiesService(configPropertyName, configPropertyValue);
            ServiceController<?> controller = serviceTarget.addService(serviceName, service).setInitialMode(ServiceController.Mode.NEVER)
                    .install();
        } else {
            context.reloadRequired();
        }
    }

}
