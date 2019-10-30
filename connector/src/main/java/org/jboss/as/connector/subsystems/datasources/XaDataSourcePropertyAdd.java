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
