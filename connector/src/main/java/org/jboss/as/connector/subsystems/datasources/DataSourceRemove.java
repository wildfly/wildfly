/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.common.jndi.Constants.JNDI_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import javax.sql.DataSource;

import org.jboss.as.connector._private.Capabilities;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Operation handler responsible for removing a (XA)DataSource.
 *
 * @author Stefano Maestrioperation2.get(OP).set("write-attribute");
 */
public class DataSourceRemove extends AbstractRemoveStepHandler {
    static final DataSourceRemove INSTANCE = new DataSourceRemove(false);
    static final DataSourceRemove XA_INSTANCE = new DataSourceRemove(true);

    private final boolean isXa;


    private DataSourceRemove(final boolean isXa) {
        this.isXa = isXa;
    }


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {


        if (context.isResourceServiceRestartAllowed()) {
                final ModelNode address = operation.require(OP_ADDR);
                final String dsName = PathAddress.pathAddress(address).getLastElement().getValue();
                final String jndiName = JNDI_NAME.resolveModelAttribute(context, model).asString();

                final ServiceName dataSourceServiceName = context.getCapabilityServiceName(Capabilities.DATA_SOURCE_CAPABILITY_NAME, dsName, DataSource.class);
                final ServiceName dataSourceCongServiceName = DataSourceConfigService.SERVICE_NAME_BASE.append(dsName);
                final ServiceName xaDataSourceConfigServiceName = XADataSourceConfigService.SERVICE_NAME_BASE.append(dsName);
                final ServiceName driverDemanderServiceName = ServiceName.JBOSS.append("driver-demander").append(jndiName);
                final ServiceName referenceFactoryServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE
                        .append(dsName);
                final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);

                context.removeService(bindInfo.getBinderServiceName());

                context.removeService(referenceFactoryServiceName);

                context.removeService(dataSourceServiceName.append(Constants.STATISTICS));

                if (!isXa) {
                    context.removeService(dataSourceCongServiceName);
                } else {
                    context.removeService(xaDataSourceConfigServiceName);
                }
                context.removeService(dataSourceServiceName);

                context.removeService(driverDemanderServiceName);
            } else {
                context.reloadRequired();
            }

        }
}
