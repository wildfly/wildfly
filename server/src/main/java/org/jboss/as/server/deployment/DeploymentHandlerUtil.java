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

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.as.server.services.path.RelativePathService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jboss.msc.service.ServiceController.Mode.REMOVE;

/**
 * Utility methods used by operation handlers involved with deployment.
 * <p/>
 * This class is part of the runtime operation and should not have any reference to dmr.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DeploymentHandlerUtil {

    static class ContentItem {
        // either hash or <path, relativeTo, isArchive>
        private byte[] hash;
        private String path;
        private String relativeTo;
        private boolean isArchive;

        ContentItem(final byte[] hash) {
            assert hash != null : "hash is null";
            this.hash = hash;
        }

        ContentItem(final String path, final String relativeTo, final boolean isArchive) {
            assert path != null : "path is null";
            this.path = path;
            this.relativeTo = relativeTo;
            this.isArchive = isArchive;
        }
    }

    private DeploymentHandlerUtil() {
    }

    public static void deploy(final NewOperationContext context, final String deploymentUnitName, final String managementName, final ContentItem... contents) throws OperationFailedException {
        assert contents != null : "contents is null";

        if (context.getType() == NewOperationContext.Type.SERVER) {
            context.addStep(new NewStepHandler() {
                public void execute(NewOperationContext context, ModelNode operation) {
                    doDeploy(context, deploymentUnitName, managementName, contents);
                    context.completeStep();
                }
            }, NewOperationContext.Stage.RUNTIME);
        }
    }

    private static void doDeploy(final NewOperationContext context, final String deploymentUnitName, final String managementName, final ContentItem... contents) {
        final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        final ServiceController<?> controller = serviceRegistry.getService(deploymentUnitServiceName);
        if (controller != null) {
            controller.setMode(ServiceController.Mode.ACTIVE);
        } else {
            final ServiceTarget serviceTarget = context.getServiceTarget();
            final ServiceController<?> contentService;
            // TODO: overlay service
            final ServiceName contentsServiceName = deploymentUnitServiceName.append("contents");
            if (contents[0].hash != null)
                contentService = ContentServitor.addService(serviceTarget, contentsServiceName, contents[0].hash);
            else {
                final String path = contents[0].path;
                final String relativeTo = contents[0].relativeTo;

                final ServiceName relativeToPathServiceName = relativeTo != null ? RelativePathService.pathNameOf(relativeTo) : null;
                contentService = PathContentServitor.addService(serviceTarget, contentsServiceName, path, relativeToPathServiceName);
            }
            final RootDeploymentUnitService service = new RootDeploymentUnitService(deploymentUnitName, managementName, null);
            final ServiceController<DeploymentUnit> deploymentUnitController = serviceTarget.addService(deploymentUnitServiceName, service)
                    .addDependency(Services.JBOSS_DEPLOYMENT_CHAINS, DeployerChains.class, service.getDeployerChainsInjector())
                    .addDependency(ServerDeploymentRepository.SERVICE_NAME, ServerDeploymentRepository.class, service.getServerDeploymentRepositoryInjector())
                    .addDependency(contentsServiceName, VirtualFile.class, service.contentsInjector)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
            contentService.addListener(new AbstractServiceListener<Object>() {
                @Override
                public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                    if (transition == ServiceController.Transition.REMOVING_to_REMOVED) {
                        deploymentUnitController.setMode(REMOVE);
                    }
                }
            });
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
                    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                        if (transition == ServiceController.Transition.REMOVING_to_REMOVED) {
                            controller.removeListener(this);
                            latch.countDown();
                            if (ticket.decrementAndGet() == 0) {
                                action.run();
                            }
                        }
                    }
                });
            } else {
                latch.countDown();
                if (ticket.decrementAndGet() == 0)
                    action.run();
            }
        }
    }

    public static void redeploy(final NewOperationContext operationContext, final String deploymentUnitName, final String managementName, final ContentItem... contents) throws OperationFailedException {
        assert contents != null : "contents is null";

        if (operationContext.getType() == NewOperationContext.Type.SERVER) {
            operationContext.addStep(new NewStepHandler() {
                public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                    final ServiceController<?> controller = context.getServiceRegistry(false).getService(Services.deploymentUnitName(deploymentUnitName));
                    if (controller != null) {
                        controller.addListener(new AbstractServiceListener<Object>() {

                            @Override
                            public void listenerAdded(ServiceController<?> controller) {
                                if (!controller.compareAndSetMode(ServiceController.Mode.ACTIVE, ServiceController.Mode.NEVER)) {
                                    controller.removeListener(this);
                                }
                            }

                            public void serviceStopping(ServiceController<?> controller) {
                                controller.removeListener(this);
                                controller.compareAndSetMode(ServiceController.Mode.NEVER, ServiceController.Mode.ACTIVE);
                            }
                        });
                    } else {
                        doDeploy(context, deploymentUnitName, managementName, contents);
                    }
                    context.completeStep();
                }
            }, NewOperationContext.Stage.RUNTIME);
        }
        operationContext.completeStep();
    }

    private static void remove(final ServiceRegistry serviceRegistry, final ServiceName serviceName) {
        final ServiceController<?> controller = serviceRegistry.getService(serviceName);
        controller.setMode(REMOVE);
    }

    public static void replace(final NewOperationContext operationContext, final String deploymentUnitName, final String managementName,
                               final String replacedDeploymentUnitName, final ContentItem... contents) throws OperationFailedException {
        assert contents != null : "contents is null";

        if (operationContext.getType() == NewOperationContext.Type.SERVER) {
            operationContext.addStep(new NewStepHandler() {
                public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
                    final ServiceName replacedDeploymentUnitServiceName = Services.deploymentUnitName(replacedDeploymentUnitName);
                    final ServiceName replacedContentsServiceName = replacedDeploymentUnitServiceName.append("contents");
                    executeWhenRemoved(new Runnable() {
                        @Override
                        public void run() {
                            doDeploy(operationContext, deploymentUnitName, managementName, contents);
                        }
                    }, operationContext.getServiceRegistry(false), replacedDeploymentUnitServiceName, replacedContentsServiceName);
                    operationContext.removeService(replacedDeploymentUnitServiceName);
                    operationContext.removeService(replacedContentsServiceName);

                    context.completeStep();
                }
            }, NewOperationContext.Stage.RUNTIME);
        }
    }

    public static void undeploy(final NewOperationContext context, final String deploymentUnitName) {
        if (context.getType() == NewOperationContext.Type.SERVER) {
            context.addStep(new NewStepHandler() {
                public void execute(NewOperationContext context, ModelNode operation) {
                    final ServiceName deploymentUnitServiceName = Services.deploymentUnitName(deploymentUnitName);
                    context.removeService(deploymentUnitServiceName);
                    context.removeService(deploymentUnitServiceName.append("contents"));

                    context.completeStep();
                }
            }, NewOperationContext.Stage.RUNTIME);
        }
    }
}
