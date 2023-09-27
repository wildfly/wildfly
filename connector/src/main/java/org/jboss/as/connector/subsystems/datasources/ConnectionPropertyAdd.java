/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTY_VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a recovery-environment to the Transactions subsystem
 *
 */
public class ConnectionPropertyAdd extends AbstractAddStepHandler {

    public static final ConnectionPropertyAdd INSTANCE = new ConnectionPropertyAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode modelNode) throws OperationFailedException {
        CONNECTION_PROPERTY_VALUE.validateAndSet(operation, modelNode);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel) throws OperationFailedException {

        final String configPropertyValue = CONNECTION_PROPERTY_VALUE.resolveModelAttribute(context, recoveryEnvModel).asString();
        final ModelNode address = operation.require(OP_ADDR);
        PathAddress path = PathAddress.pathAddress(address);
        final String jndiName = path.getElement(path.size() - 2).getValue();
        final String configPropertyName = PathAddress.pathAddress(address).getLastElement().getValue();

        ServiceName serviceName = DataSourceConfigService.SERVICE_NAME_BASE.append(jndiName).append("connection-properties").append(configPropertyName);

        final ServiceRegistry registry = context.getServiceRegistry(true);


        final ServiceName dataSourceConfigServiceName = DataSourceConfigService.SERVICE_NAME_BASE
                .append(jndiName);
        final ServiceController<?> dataSourceConfigController = registry
                .getService(dataSourceConfigServiceName);
        if (dataSourceConfigController == null || !((DataSource) dataSourceConfigController.getValue()).isEnabled()) {


            final ServiceTarget serviceTarget = context.getServiceTarget();

            final ConnectionPropertiesService service = new ConnectionPropertiesService(configPropertyName, configPropertyValue);
            serviceTarget.addService(serviceName, service).setInitialMode(ServiceController.Mode.NEVER)
                    .install();
        } else {
            context.reloadRequired();
        }
    }

}
