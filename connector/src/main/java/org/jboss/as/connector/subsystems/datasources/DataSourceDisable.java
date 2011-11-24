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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;

import static org.jboss.as.connector.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;
import static org.jboss.as.connector.ConnectorMessages.MESSAGES;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Operation handler responsible for disabling an existing data-source.
 *
 * @author John Bailey
 */
public class DataSourceDisable implements OperationStepHandler {
    static final DataSourceDisable INSTANCE = new DataSourceDisable();

    public void execute(OperationContext context, ModelNode operation) {

        final ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        model.get(ENABLED).set(false);

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, ModelNode operation) throws OperationFailedException {

                    final ModelNode address = operation.require(OP_ADDR);
                    final String dsName = PathAddress.pathAddress(address).getLastElement().getValue();
                    final String jndiName = model.get(JNDINAME.getName()).asString();

                    final ServiceRegistry registry = context.getServiceRegistry(true);

                    final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
                    final ServiceController<?> dataSourceController = registry.getService(dataSourceServiceName);
                    if (dataSourceController != null) {
                        if (ServiceController.State.UP.equals(dataSourceController.getState())) {
                            dataSourceController.setMode(ServiceController.Mode.NEVER);
                        } else {
                            throw new OperationFailedException(new ModelNode().set(MESSAGES.serviceNotEnabled("Data-source", dsName)));
                        }
                    } else {
                        throw new OperationFailedException(new ModelNode().set(MESSAGES.serviceNotAvailable("Data-source", dsName)));
                    }

                    final ServiceName referenceServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE.append(dsName);
                    final ServiceController<?> referenceController = registry.getService(referenceServiceName);
                    if (referenceController != null ) {
                        referenceController.setMode(ServiceController.Mode.REMOVE);
                    }

                    final ServiceName binderServiceName = ContextNames.bindInfoFor(jndiName).getBinderServiceName();

                    final ServiceController<?> binderController = registry.getService(binderServiceName);
                    if (binderController != null ) {
                        binderController.setMode(ServiceController.Mode.REMOVE);
                    }

                    final ServiceName dataSourceConfigServiceName = DataSourceConfigService.SERVICE_NAME_BASE.append(dsName);
                    final ServiceController<?> dataSourceConfigController = registry.getService(dataSourceConfigServiceName);


                    final List<ServiceName> serviceNames = registry.getServiceNames();


                    final ServiceName xaDataSourceConfigServiceName = XADataSourceConfigService.SERVICE_NAME_BASE.append(dsName);
                    final ServiceController<?> xaDataSourceConfigController = registry.getService(xaDataSourceConfigServiceName);


                    for (ServiceName name : serviceNames) {
                        if (dataSourceConfigServiceName.append("connetion-properties").isParentOf(name)) {
                            final ServiceController<?> connProperyController = registry.getService(name);

                            if (connProperyController != null) {
                                if (ServiceController.State.UP.equals(connProperyController.getState())) {
                                    connProperyController.setMode(ServiceController.Mode.NEVER);
                                } else {
                                    throw new OperationFailedException(new ModelNode().set(MESSAGES.serviceAlreadyStarted("Data-source.connectionProperty", name)));
                                }
                            } else {
                                throw new OperationFailedException(new ModelNode().set(MESSAGES.serviceNotAvailable("Data-source.connectionProperty", name)));
                            }
                        }
                        if (xaDataSourceConfigServiceName.append("xa-datasource-properties").isParentOf(name)) {
                            final ServiceController<?> xaConfigProperyController = registry.getService(name);

                            if (xaConfigProperyController != null) {
                                if (ServiceController.State.UP.equals(xaConfigProperyController.getState())) {
                                    xaConfigProperyController.setMode(ServiceController.Mode.NEVER);
                                } else {
                                    throw new OperationFailedException(new ModelNode().set(MESSAGES.serviceAlreadyStarted("Data-source.xa-config-property", name)));
                                }
                            } else {
                                throw new OperationFailedException(new ModelNode().set(MESSAGES.serviceNotAvailable("Data-source.xa-config-property", name)));
                            }
                        }
                    }


                    if (xaDataSourceConfigController != null) {
                        xaDataSourceConfigController.setMode(ServiceController.Mode.REMOVE);
                    }

                    if (dataSourceConfigController != null) {
                        dataSourceConfigController.setMode(ServiceController.Mode.REMOVE);
                    }

                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }
}
