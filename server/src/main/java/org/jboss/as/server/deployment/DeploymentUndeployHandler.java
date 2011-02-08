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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.START;

import java.util.Locale;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.as.server.controller.descriptions.DeploymentDescription;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handles undeployment from the runtime.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentUndeployHandler implements ModelUpdateOperationHandler, RuntimeOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "undeploy";

    static final ModelNode getOperation(ModelNode address) {
        return Util.getEmptyOperation(OPERATION_NAME, address);
    }

    public static final DeploymentUndeployHandler INSTANCE = new DeploymentUndeployHandler();

    private DeploymentUndeployHandler() {
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return DeploymentDescription.getUndeployDeploymentOperation(locale);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {
        try {
            ModelNode model = context.getSubModel();
            model.get(START).set(false);
            ModelNode compensatingOp = DeploymentDeployHandler.getOperation(operation.get(OP_ADDR));
            undeploy(model, context, resultHandler, compensatingOp);
        }
        catch (Exception e) {
            resultHandler.handleFailed(new ModelNode().set(e.getLocalizedMessage()));
        }
        return Cancellable.NULL;
    }

    private void undeploy(ModelNode model, NewOperationContext context, ResultHandler resultHandler,
            ModelNode compensatingOp) {
        if (context instanceof NewRuntimeOperationContext) {
            NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;
            String deploymentUnitName = model.require(NAME).asString();
            final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
            final ServiceRegistry serviceRegistry = updateContext.getServiceRegistry();

            final ServiceController<?> controller = serviceRegistry.getService(deploymentUnitServiceName);
            controller.setMode(ServiceController.Mode.NEVER);
            // TODO - connect to service lifecycle properly
            resultHandler.handleResultComplete(compensatingOp);
        }
        else {
            resultHandler.handleResultComplete(compensatingOp);
        }

    }
}
