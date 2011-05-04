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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Operation handler responsible for disabling an existing data-source.
 *
 * @author John Bailey
 */
public class DataSourceDisable implements ModelUpdateOperationHandler {
    static final DataSourceDisable INSTANCE = new DataSourceDisable();

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.subsystems.datasources");

    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        final ModelNode opAddr = operation.require(OP_ADDR);

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(ENABLE);
        compensatingOperation.get(OP_ADDR).set(opAddr);

        final String jndiName = PathAddress.pathAddress(opAddr).getLastElement().getValue();

        // update the model
        context.getSubModel().get(ENABLED).set(false);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(final RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceRegistry registry = context.getServiceRegistry();

                    final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
                    final ServiceController<?> dataSourceController = registry.getService(dataSourceServiceName);
                    if (dataSourceController != null) {
                        if (ServiceController.State.UP.equals(dataSourceController.getState())) {
                            dataSourceController.setMode(ServiceController.Mode.NEVER);
                            dataSourceController.addListener(new AbstractServiceListener<Object>() {
                                public void serviceStopped(ServiceController<?> serviceController) {
                                    resultHandler.handleResultComplete();
                                    serviceController.removeListener(this);
                                }
                            });
                        } else {
                            throw new OperationFailedException(new ModelNode().set("Data-source service [" + jndiName + "] is not enabled"));
                        }
                    } else {
                        throw new OperationFailedException(new ModelNode().set("Data-source service [" + jndiName + "] is not available"));
                    }

                    final ServiceName referenceServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE.append(jndiName);
                    final ServiceController<?> referenceController = registry.getService(referenceServiceName);
                    if (referenceController != null && ServiceController.State.UP.equals(referenceController.getState())) {
                        referenceController.setMode(ServiceController.Mode.NEVER);
                    }

                    final ServiceName binderServiceName = ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName);
                    final ServiceController<?> binderController = registry.getService(binderServiceName);
                    if (binderController != null && ServiceController.State.UP.equals(binderController.getState())) {
                        binderController.setMode(ServiceController.Mode.NEVER);
                    }
                }

            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensatingOperation);
    }
}
