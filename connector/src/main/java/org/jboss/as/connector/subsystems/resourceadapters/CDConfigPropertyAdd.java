/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.CONFIG_PROPERTY_VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * Adds a recovery-environment to the Transactions subsystem
 *
 */
public class CDConfigPropertyAdd extends AbstractAddStepHandler {

    public static final CDConfigPropertyAdd INSTANCE = new CDConfigPropertyAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode modelNode) throws OperationFailedException {
        CONFIG_PROPERTY_VALUE.validateAndSet(operation, modelNode);

    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode recoveryEnvModel) throws OperationFailedException {

        final String configPropertyValue = CONFIG_PROPERTY_VALUE.resolveModelAttribute(context, recoveryEnvModel).asString();
        final ModelNode address = operation.require(OP_ADDR);
        PathAddress path = PathAddress.pathAddress(address);
        final String archiveName = path.getElement(path.size() -3).getValue();
        final String cfName = path.getElement(path.size() -2).getValue();
        final String configPropertyName = PathAddress.pathAddress(address).getLastElement().getValue();

        ServiceName serviceName = ServiceName.of(ConnectorServices.RA_SERVICE, archiveName, cfName, configPropertyName);
        ServiceName cfServiceName = ServiceName.of(ConnectorServices.RA_SERVICE, archiveName, cfName);

        final ServiceTarget serviceTarget = context.getServiceTarget();

        final CDConfigPropertiesService service = new CDConfigPropertiesService(configPropertyName, configPropertyValue);
        serviceTarget.addService(serviceName, service).setInitialMode(ServiceController.Mode.ACTIVE)
                    .addDependency(cfServiceName, ModifiableConnDef.class, service.getRaInjector() )
                    .install();
    }
}
