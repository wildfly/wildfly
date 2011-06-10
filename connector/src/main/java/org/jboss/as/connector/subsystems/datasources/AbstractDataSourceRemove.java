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

import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Abstract operation handler responsible for removing a DataSource.
 * @author John Bailey
 */
public abstract class AbstractDataSourceRemove implements ModelRemoveOperationHandler {

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.subsystems.datasources");

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {
        final ModelNode opAddr = operation.require(OP_ADDR);
        final String serviceName = PathAddress.pathAddress(opAddr).getLastElement().getValue();

        // Compensating is add
        final ModelNode model = context.getSubModel();
        final ModelNode compensating = Util.getEmptyOperation(ADD, opAddr);

        final String rawJndiName = model.require(JNDINAME).asString();
        final String jndiName;
        if (!rawJndiName.startsWith("java:/") && model.hasDefined(USE_JAVA_CONTEXT) && model.get(USE_JAVA_CONTEXT).asBoolean()) {
            jndiName = "java:/" + rawJndiName;
        } else {
            jndiName = rawJndiName;
        }

        if (model.has(CONNECTION_PROPERTIES)) {
            for (ModelNode property : model.get(CONNECTION_PROPERTIES).asList()) {
                compensating.get(CONNECTION_PROPERTIES, property.asProperty().getName()).set(property.asString());
            }
        }
        for (final AttributeDefinition attribute : getModelProperties()) {
            if (model.hasDefined(attribute.getName())) {
                compensating.get(attribute.getName()).set(model.get(attribute.getName()));
            }
        }

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceRegistry registry = context.getServiceRegistry();

                    final ServiceName binderServiceName = ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName);
                    final ServiceController<?> binderController = registry.getService(binderServiceName);
                    if (binderController != null) {
                        binderController.setMode(ServiceController.Mode.REMOVE);
                    }

                    final ServiceName referenceFactoryServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE
                            .append(serviceName);
                    final ServiceController<?> referenceFactoryController = registry.getService(referenceFactoryServiceName);
                    if (referenceFactoryController != null) {
                        referenceFactoryController.setMode(ServiceController.Mode.REMOVE);
                    }

                    final ServiceName dataSourceConfigServiceName = DataSourceConfigService.SERVICE_NAME_BASE
                            .append(serviceName);
                    final ServiceController<?> dataSourceConfigController = registry.getService(dataSourceConfigServiceName);
                    if (dataSourceConfigController != null) {
                        dataSourceConfigController.setMode(ServiceController.Mode.REMOVE);
                    }

                    final ServiceName xaDataSourceConfigServiceName = XADataSourceConfigService.SERVICE_NAME_BASE
                            .append(serviceName);
                    final ServiceController<?> xaDataSourceConfigController = registry
                            .getService(xaDataSourceConfigServiceName);
                    if (xaDataSourceConfigController != null) {
                        xaDataSourceConfigController.setMode(ServiceController.Mode.REMOVE);
                    }

                    final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(serviceName);
                    final ServiceController<?> dataSourceController = registry.getService(dataSourceServiceName);
                    if (dataSourceController != null) {
                        dataSourceController.setMode(ServiceController.Mode.REMOVE);
                    }
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }

        return new BasicOperationResult(compensating);
    }

    protected abstract AttributeDefinition[] getModelProperties();
}
