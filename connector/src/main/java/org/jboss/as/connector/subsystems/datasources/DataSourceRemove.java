/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.JNDI_NAME;
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
        super(Capabilities.DATA_SOURCE_CAPABILITY);
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
