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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Utility methods used by operation handlers involved with deployment.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 *
 */
public class DeploymentHandlerUtil {

    private DeploymentHandlerUtil() {
    }

    public static void deploy(ModelNode deploymentModel, NewOperationContext context, ResultHandler resultHandler,
            ModelNode compensatingOp) {
        if (context instanceof NewRuntimeOperationContext) {
            NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;
            String deploymentUnitName = deploymentModel.require(NAME).asString();
            final ServiceName deploymentUnitServiceName = Services.JBOSS_DEPLOYMENT.append(deploymentUnitName);
            final ServiceRegistry serviceRegistry = updateContext.getServiceRegistry();
            final ServiceController<?> controller = serviceRegistry.getService(deploymentUnitServiceName);
            if(controller != null) {
                controller.setMode(ServiceController.Mode.ACTIVE);
            } else {
                final ServiceTarget serviceTarget = updateContext.getServiceTarget();
                final String runtimeName = deploymentModel.require(RUNTIME_NAME).asString();
                final byte[] hash = deploymentModel.require(HASH).asBytes();
                final RootDeploymentUnitService service = new RootDeploymentUnitService(deploymentUnitName, runtimeName, hash, null);
                serviceTarget.addService(deploymentUnitServiceName, service)
                    .addDependency(Services.JBOSS_DEPLOYMENT_CHAINS, DeployerChains.class, service.getDeployerChainsInjector())
                    .addDependency(ServerDeploymentRepository.SERVICE_NAME, ServerDeploymentRepository.class, service.getServerDeploymentRepositoryInjector())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            }

            // TODO - connect to service lifecycle properly
            resultHandler.handleResultComplete(compensatingOp);
        }
        else {
            resultHandler.handleResultComplete(compensatingOp);
        }
    }

    public static void replace(final ModelNode deploymentModel, final String toReplace, final NewOperationContext context, final ResultHandler resultHandler,
            final ModelNode compensatingOp) {

        if (context instanceof NewRuntimeOperationContext) {
            NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;
            final ServiceController<?> controller = updateContext.getServiceRegistry().getService(Services.JBOSS_DEPLOYMENT_UNIT.append(toReplace));
            if(controller != null) {
                controller.addListener(new AbstractServiceListener<Object>() {
                    @Override
                    public void serviceRemoved(ServiceController<? extends Object> serviceController) {
                        deploy(deploymentModel, context, resultHandler, compensatingOp);
                    }
                });
                controller.setMode(ServiceController.Mode.REMOVE);
            } else {
                deploy(deploymentModel, context, resultHandler, compensatingOp);
            }
        }
        else {
            resultHandler.handleResultComplete(compensatingOp);
        }
    }
}
