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

import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;


/**
 * Abstract operation handler responsible for removing a DataSource.
 * @author John Bailey
 */
public abstract class AbstractDataSourceRemove extends AbstractRemoveStepHandler {


    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {

        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ModelNode address = operation.require(OP_ADDR);
        final String dsName = PathAddress.pathAddress(address).getLastElement().getValue();
        final String jndiName = model.get(JNDINAME.getName()).asString();

        final ServiceName binderServiceName = ContextNames.bindInfoFor(jndiName).getBinderServiceName();
        final ServiceController<?> binderController = registry.getService(binderServiceName);
        if (binderController != null) {
            context.removeService(binderServiceName);
        }

        final ServiceName referenceFactoryServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE
                .append(dsName);
        final ServiceController<?> referenceFactoryController = registry.getService(referenceFactoryServiceName);
        if (referenceFactoryController != null) {
            context.removeService(referenceFactoryServiceName);
        }

        final ServiceName dataSourceConfigServiceName = DataSourceConfigService.SERVICE_NAME_BASE.append(dsName);
        final ServiceName xaDataSourceConfigServiceName = XADataSourceConfigService.SERVICE_NAME_BASE
                        .append(dsName);
        final List<ServiceName> serviceNames = registry.getServiceNames();


        for (ServiceName name : serviceNames) {
            if (dataSourceConfigServiceName.append("connection-properties").isParentOf(name)) {
                context.removeService(name);
            }
            if (xaDataSourceConfigServiceName.append("xa-datasource-properties").isParentOf(name)) {
                context.removeService(name);
            }
        }


        final ServiceController<?> dataSourceConfigController = registry.getService(dataSourceConfigServiceName);
        if (dataSourceConfigController != null) {
            context.removeService(dataSourceConfigServiceName);
        }

        final ServiceController<?> xaDataSourceConfigController = registry
                .getService(xaDataSourceConfigServiceName);
        if (xaDataSourceConfigController != null) {
            context.removeService(xaDataSourceConfigServiceName);
        }

        final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
        final ServiceController<?> dataSourceController = registry.getService(dataSourceServiceName);
        if (dataSourceController != null) {
            context.removeService(dataSourceServiceName);
        }

    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }

    protected abstract SimpleAttributeDefinition[] getModelProperties();
}
