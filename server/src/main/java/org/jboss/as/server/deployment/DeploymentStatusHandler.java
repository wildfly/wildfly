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

package org.jboss.as.server.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author Jason T. Greene
 */
public class DeploymentStatusHandler implements NewStepHandler {
    public static final String ATTRIBUTE_NAME = "status";
    public static final NewStepHandler INSTANCE = new DeploymentStatusHandler();
    private static final ModelNode NO_METRICS = new ModelNode().set("no metrics available");

    @Override
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final PathElement element = address.getLastElement();

        context.addStep(new NewStepHandler() {
                    @Override
                    public void execute(final NewOperationContext context, final ModelNode operation) throws OperationFailedException {
                        final ModelNode result = context.getResult();
                        final ServiceController<?> controller = context.getServiceRegistry(false).getService(Services.deploymentUnitName(element.getValue()));
                        if(controller != null) {
                            if (controller.getSubstate() == ServiceController.Substate.WONT_START &&
                                      controller.getState() == ServiceController.State.DOWN) {
                                result.set(AbstractDeploymentUnitService.DeploymentStatus.STOPPED.toString());
                            } else {
                                result.set(((AbstractDeploymentUnitService)controller.getService()).getStatus().toString());
                            }
                        } else {
                            result.set(NO_METRICS);
                        }
                        context.completeStep();
                    }
                }, NewOperationContext.Stage.RUNTIME);
        context.completeStep();
    }
}
