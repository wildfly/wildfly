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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
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
 */
public class DeploymentHandlerUtil {


    private DeploymentHandlerUtil() {
    }

    public static void deploy(final ModelNode deploymentModel, OperationContext context, final ResultHandler resultHandler) throws OperationFailedException {
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext runtimeContext) throws OperationFailedException {
                    deploy(deploymentModel, runtimeContext, resultHandler);
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
    }

    private static void deploy(final ModelNode deploymentModel, RuntimeTaskContext context, final ResultHandler resultHandler) {
        String deploymentUnitName = deploymentModel.require(NAME).asString();
        final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
        final ServiceRegistry serviceRegistry = context.getServiceRegistry();
        final ServiceController<?> controller = serviceRegistry.getService(deploymentUnitServiceName);
        if (controller != null) {
            controller.setMode(ServiceController.Mode.ACTIVE);
        } else {
            final ServiceTarget serviceTarget = context.getServiceTarget();
            final String runtimeName = deploymentModel.require(RUNTIME_NAME).asString();
            final byte[] hash = deploymentModel.require(HASH).asBytes();
            final RootDeploymentUnitService service = new RootDeploymentUnitService(deploymentUnitName, runtimeName, hash, null);
            serviceTarget.addService(deploymentUnitServiceName, service)
                    .addDependency(Services.JBOSS_DEPLOYMENT_CHAINS, DeployerChains.class, service.getDeployerChainsInjector())
                    .addDependency(ServerDeploymentRepository.SERVICE_NAME, ServerDeploymentRepository.class, service.getServerDeploymentRepositoryInjector())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            resultHandler.handleResultComplete();
        }
    }

    public static void redeploy(final ModelNode deploymentModel, final OperationContext operationContext, final ResultHandler resultHandler) throws OperationFailedException {
        if (operationContext.getRuntimeContext() != null) {
            operationContext.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(final RuntimeTaskContext context) throws OperationFailedException {
                    final String deploymentUnitName = deploymentModel.require(NAME).asString();
                    final ServiceController<?> controller = context.getServiceRegistry().getService(Services.deploymentUnitName(deploymentUnitName));
                    if (controller != null) {
                        controller.addListener(new AbstractServiceListener<Object>() {

                            @Override
                            public void listenerAdded(ServiceController<?> controller) {
                                if (! controller.compareAndSetMode(ServiceController.Mode.ACTIVE, ServiceController.Mode.NEVER)) {
                                    controller.removeListener(this);
                                }
                            }

                            public void serviceStopping(ServiceController<?> controller) {
                                controller.removeListener(this);
                                controller.compareAndSetMode(ServiceController.Mode.NEVER, ServiceController.Mode.ACTIVE);
                            }
                        });
                    } else {
                        deploy(deploymentModel, context, resultHandler);
                    }
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
    }

    public static void replace(final ModelNode deploymentModel, final String toReplace, final OperationContext operationContext, final ResultHandler resultHandler) throws OperationFailedException {
        if (operationContext.getRuntimeContext() != null) {
            operationContext.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(final RuntimeTaskContext runtimeContext) throws OperationFailedException {
                    final ServiceController<?> controller = runtimeContext.getServiceRegistry()
                            .getService(Services.deploymentUnitName(toReplace));
                    if (controller != null) {
                        controller.addListener(new AbstractServiceListener<Object>() {
                            @Override
                            public void listenerAdded(ServiceController<? extends Object> serviceController) {
                                controller.setMode(ServiceController.Mode.REMOVE);
                            }


                            @Override
                            public void serviceRemoved(ServiceController<? extends Object> serviceController) {
                                controller.removeListener(this);
                                deploy(deploymentModel, runtimeContext, resultHandler);
                            }
                        });
                    } else {
                        deploy(deploymentModel, runtimeContext, resultHandler);
                    }
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
    }
}
