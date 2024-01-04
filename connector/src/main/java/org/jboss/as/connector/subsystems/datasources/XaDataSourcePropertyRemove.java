/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Operation handler responsible for removing a (XA)DataSource.
 *
 * @author Stefano Maestrioperation2.get(OP).set("write-attribute");
 */
public class XaDataSourcePropertyRemove extends AbstractRemoveStepHandler {

    public static final XaDataSourcePropertyRemove INSTANCE = new XaDataSourcePropertyRemove();


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {


        if (context.isResourceServiceRestartAllowed()) {
                final ModelNode address = operation.require(OP_ADDR);
                final PathAddress path = PathAddress.pathAddress(address);
                final String jndiName = path.getElement(path.size() - 2).getValue();
                final String configPropertyName = PathAddress.pathAddress(address).getLastElement().getValue();

                ServiceName configPropertyServiceName = XADataSourceConfigService.SERVICE_NAME_BASE.append(jndiName).
                        append("xa-datasource-properties").append(configPropertyName);

                context.removeService(configPropertyServiceName);

            } else {
                context.reloadRequired();
            }

        }
}
