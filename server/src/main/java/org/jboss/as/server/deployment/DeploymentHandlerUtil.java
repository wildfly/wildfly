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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility methods used by operation handlers involved with deployment.
 *
 * This class is part of the runtime operation and should not have any reference to dmr.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentHandlerUtil {


    private DeploymentHandlerUtil() {
    }

    public static void deploy(final OperationContext context, final String deploymentUnitName, final String runtimeName, final byte[] contents, final ResultHandler resultHandler) throws OperationFailedException {
        assert contents != null : "contents is null";
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(RuntimeTaskContext runtimeContext) throws OperationFailedException {
                    deploy(runtimeContext, deploymentUnitName, runtimeName, contents, resultHandler);
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
    }

    private static void deploy(final RuntimeTaskContext context, final String deploymentUnitName, final String runtimeName, final byte[] contentHash, final ResultHandler resultHandler) {
        final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
        final ServiceRegistry serviceRegistry = context.getServiceRegistry();
        final ServiceController<?> controller = serviceRegistry.getService(deploymentUnitServiceName);
        if (controller != null) {
            controller.setMode(ServiceController.Mode.ACTIVE);
        } else {
            final ServiceTarget serviceTarget = context.getServiceTarget();
            // TODO: overlay service
            final ServiceName contentsServiceName = deploymentUnitServiceName.append("contents");
            ContentServitor.addService(serviceTarget, contentsServiceName, contentHash);
            final RootDeploymentUnitService service = new RootDeploymentUnitService(deploymentUnitName, runtimeName, null);
            serviceTarget.addService(deploymentUnitServiceName, service)
                    .addDependency(Services.JBOSS_DEPLOYMENT_CHAINS, DeployerChains.class, service.getDeployerChainsInjector())
                    .addDependency(ServerDeploymentRepository.SERVICE_NAME, ServerDeploymentRepository.class, service.getServerDeploymentRepositoryInjector())
                    .addDependency(contentsServiceName, VirtualFile.class, service.contentsInjector)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            resultHandler.handleResultComplete();
        }
    }

    private static void executeWhenRemoved(final Runnable action, final ServiceRegistry serviceRegistry, final ServiceName... serviceNames) {
        final CountDownLatch latch = new CountDownLatch(serviceNames.length);
        final AtomicInteger ticket = new AtomicInteger(serviceNames.length);
        for (final ServiceName serviceName : serviceNames) {
            final ServiceController<?> controller = serviceRegistry.getService(serviceName);
            if (controller != null) {
                controller.addListener(new AbstractServiceListener<Object>() {
                    @Override
                    public void listenerAdded(ServiceController<? extends Object> serviceController) {
                        controller.setMode(ServiceController.Mode.REMOVE);
                    }

                    @Override
                    public void serviceRemoved(ServiceController<? extends Object> serviceController) {
                        controller.removeListener(this);
                        latch.countDown();
                        if(ticket.decrementAndGet() == 0)
                            action.run();
                    }
                });
            }
            else {
                latch.countDown();
                if(ticket.decrementAndGet() == 0)
                    action.run();
            }
        }
    }

    public static void redeploy(final OperationContext operationContext, final String deploymentUnitName, final String runtimeName, final byte[] contents, final ResultHandler resultHandler) throws OperationFailedException {
        assert contents != null : "contents is null";
        if (operationContext.getRuntimeContext() != null) {
            operationContext.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(final RuntimeTaskContext context) throws OperationFailedException {
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
                        deploy(context, deploymentUnitName, runtimeName, contents, resultHandler);
                    }
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
    }

    private static void remove(final ServiceRegistry serviceRegistry, final ServiceName serviceName) {
        final ServiceController<?> controller = serviceRegistry.getService(serviceName);
        controller.setMode(ServiceController.Mode.REMOVE);
    }

    public static void replace(final OperationContext operationContext, final String deploymentUnitName, final String runtimeName, final byte[] contents, final ResultHandler resultHandler) throws OperationFailedException {
        assert contents != null : "contents is null";
        if (operationContext.getRuntimeContext() != null) {
            operationContext.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                @Override
                public void execute(final RuntimeTaskContext runtimeContext) throws OperationFailedException {
                    final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
                    final ServiceName contentsServiceName = deploymentUnitServiceName.append("contents");
                    executeWhenRemoved(new Runnable() {
                        @Override
                        public void run() {
                            deploy(runtimeContext, deploymentUnitName, runtimeName, contents, resultHandler);
                        }
                    }, runtimeContext.getServiceRegistry(), deploymentUnitServiceName, contentsServiceName);
                    // TODO: not?
                    //resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
    }

    public static void undeploy(final OperationContext context, final String deploymentUnitName, final ResultHandler resultHandler) {
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
                    final ServiceRegistry serviceRegistry = context.getServiceRegistry();
                    remove(serviceRegistry, deploymentUnitServiceName.append("contents"));
                    remove(serviceRegistry, deploymentUnitServiceName);
                    resultHandler.handleResultComplete();
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
    }
}
