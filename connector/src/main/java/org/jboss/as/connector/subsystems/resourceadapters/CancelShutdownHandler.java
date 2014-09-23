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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.JNDINAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Arrays;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.services.resourceadapters.deployment.ResourceAdapterXmlDeploymentService;
import org.jboss.as.connector.subsystems.datasources.DataSourceStatisticsListener;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
  * Operation handler for cancel-shutdown-delay operation
  *
  * @author Stefano Maestri
 */
public class CancelShutdownHandler implements OperationStepHandler {
    static final CancelShutdownHandler INSTANCE = new CancelShutdownHandler();


    public CancelShutdownHandler() {
        super();
    }

    public void execute(OperationContext context, ModelNode operation) {

        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();

        if (context.isNormalServer()) {

            DataSourceStatisticsListener.removeStatisticsResources(resource);

            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, ModelNode operation) throws OperationFailedException {

                    final ModelNode address = operation.require(OP_ADDR);
                    int raIndex = PathAddress.pathAddress(address).size() - 2;
                    final String idName = PathAddress.pathAddress(address).getElement(raIndex).getValue();
                    final String raName = context.readResourceFromRoot(PathAddress.pathAddress(address).subAddress(0, raIndex + 1)).getModel().get("archive").asString();
                    final String jndiName = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().get(JNDINAME.getName()).asString();

                    final ServiceRegistry registry = context.getServiceRegistry(true);
                    boolean returnValue;
                    final ServiceName raDeploymentServiceName = ConnectorServices.getDeploymentServiceName(raName, idName);
                    final ResourceAdapterXmlDeploymentService raService = (ResourceAdapterXmlDeploymentService) registry.getService(raDeploymentServiceName).getService();
                    if (raService != null) {
                        int positionMatch = Arrays.asList(raService.getValue().getDeployment().getCfJndiNames()).indexOf(jndiName);
                        returnValue = raService.getValue().getDeployment().getConnectionManagers()[positionMatch].cancelShutdown();

                    } else {
                        throw new OperationFailedException(new ModelNode().set(ConnectorLogger.ROOT_LOGGER.serviceNotAvailable("resource adapter", idName)));
                    }
                    ModelNode operationResult = new ModelNode();
                    operationResult.add(returnValue);
                    context.getResult().set(operationResult);
                    context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                }

            }, OperationContext.Stage.RUNTIME);

        }
        context.stepCompleted();
    }

}
