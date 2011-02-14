/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.deployment;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.server.RuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.controller.descriptions.DeploymentDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handles removal of a deployment from the model.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentRemoveHandler implements ModelRemoveOperationHandler, RuntimeOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = REMOVE;

    public static final DeploymentRemoveHandler INSTANCE = new DeploymentRemoveHandler();

    private DeploymentRemoveHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getRemoveDeploymentOperation(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, final ResultHandler resultHandler) {
        try {
            final ModelNode model = context.getSubModel();
            if (model.get(START).asBoolean()) {
                String msg = String.format("Deployment %s must be undeployed before being removed", model.get(NAME).asString());
                resultHandler.handleFailed(new ModelNode().set(msg));
            }
            else {
                final ModelNode compensatingOp = DeploymentAddHandler.getOperation(operation.get(OP_ADDR), model);
                if (context instanceof RuntimeOperationContext) {
                    RuntimeOperationContext updateContext = (RuntimeOperationContext) context;
                    String deploymentUnitName = model.require(NAME).asString();
                    final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
                    final ServiceRegistry serviceRegistry = updateContext.getServiceRegistry();
                    final ServiceController<?> controller = serviceRegistry.getService(deploymentUnitServiceName);
                    controller.addListener(new AbstractServiceListener<Object>() {
                        @Override
                        public void serviceRemoved(ServiceController<? extends Object> controller) {
                            resultHandler.handleResultComplete(compensatingOp);
                            controller.removeListener(this);
                        }
                    });
                    controller.setMode(ServiceController.Mode.REMOVE);
                }
                else {
                    resultHandler.handleResultComplete(compensatingOp);
                }
            }
        }
        catch (Exception e) {
            resultHandler.handleFailed(new ModelNode().set(e.toString()));
        }
        return Cancellable.NULL;
    }
}
