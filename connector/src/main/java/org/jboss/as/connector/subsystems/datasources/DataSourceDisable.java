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

import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
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

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.subsystems.datasources");

    public void execute(OperationContext context, ModelNode operation) {

        final ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        model.get(ENABLED).set(false);

        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, ModelNode operation) throws OperationFailedException {

                    final String jndiName = Util.getJndiName(model);

                    final ServiceRegistry registry = context.getServiceRegistry(true);

                    final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
                    final ServiceController<?> dataSourceController = registry.getService(dataSourceServiceName);
                    if (dataSourceController != null) {
                        if (ServiceController.State.UP.equals(dataSourceController.getState())) {
                            dataSourceController.setMode(ServiceController.Mode.NEVER);
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

                    final ServiceName binderServiceName = Util.getBinderServiceName(jndiName);
                    final ServiceController<?> binderController = registry.getService(binderServiceName);
                    if (binderController != null && ServiceController.State.UP.equals(binderController.getState())) {
                        binderController.setMode(ServiceController.Mode.NEVER);
                    }
                    context.completeStep();
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.completeStep();
    }
}
