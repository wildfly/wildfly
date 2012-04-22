/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.scanner;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossThreadFactory;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.ALL_ATTRIBUTES;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.AUTO_DEPLOY_EXPLODED;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.AUTO_DEPLOY_XML;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.AUTO_DEPLOY_ZIPPED;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.DEPLOYMENT_TIMEOUT;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.RELATIVE_TO;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.SCAN_ENABLED;
import static org.jboss.as.server.deployment.scanner.DeploymentScannerDefinition.SCAN_INTERVAL;

/**
 * Operation adding a new {@link DeploymentScannerService}.
 *
 * @author John E. Bailey
 * @author Emanuel Muckenhuber
 * @author Stuart Douglas
 */
class DeploymentScannerAdd implements OperationStepHandler {


    private final PathManager pathManager;

    public DeploymentScannerAdd(final PathManager pathManager) {
        this.pathManager = pathManager;
    }

    /**
     * {@inheritDoc
     */
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        populateModel(context, operation, resource);
        final ModelNode model = resource.getModel();

        boolean stepCompleted = false;

        if (context.isNormalServer()) {

            final Boolean enabled = SCAN_ENABLED.resolveModelAttribute(context, operation).asBoolean();
            final boolean bootTimeScan = context.isBooting() && (enabled == null || enabled == true);


            final String path = DeploymentScannerDefinition.PATH.resolveModelAttribute(context, operation).asString();
            final ModelNode relativeToNode = RELATIVE_TO.resolveModelAttribute(context, operation);
            final String relativeTo = relativeToNode.isDefined() ?  relativeToNode.asString() : null;
            final Boolean autoDeployZip = AUTO_DEPLOY_ZIPPED.resolveModelAttribute(context, operation).asBoolean();
            final Boolean autoDeployExp = AUTO_DEPLOY_EXPLODED.resolveModelAttribute(context, operation).asBoolean();
            final Boolean autoDeployXml = AUTO_DEPLOY_XML.resolveModelAttribute(context, operation).asBoolean();
            final Long deploymentTimeout = DEPLOYMENT_TIMEOUT.resolveModelAttribute(context, operation).asLong();
            final Integer scanInterval = SCAN_INTERVAL.resolveModelAttribute(context, operation).asInt();

            final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("DeploymentScanner-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
            final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2, threadFactory);

            final FileSystemDeploymentService bootTimeScanner;
            if (bootTimeScan) {
                final String pathName = pathManager.resolveRelativePathEntry(path, relativeTo);
                File relativePath = null;
                if (relativeTo != null) {
                    relativePath = new File(pathManager.getPathEntry(relativeTo).resolvePath());
                }

                bootTimeScanner = new FileSystemDeploymentService(relativeTo, new File(pathName), relativePath, null, scheduledExecutorService);
                bootTimeScanner.setAutoDeployExplodedContent(autoDeployExp);
                bootTimeScanner.setAutoDeployZippedContent(autoDeployZip);
                bootTimeScanner.setAutoDeployXMLContent(autoDeployXml);
                if (deploymentTimeout != null) {
                    bootTimeScanner.setDeploymentTimeout(deploymentTimeout);
                }
                if (scanInterval != null) {
                    bootTimeScanner.setScanInterval(scanInterval);
                }
            } else {
                bootTimeScanner = null;
            }

            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    performRuntime(context, operation, model, verificationHandler, controllers, scheduledExecutorService, bootTimeScanner);
                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            rollbackRuntime(context, operation, model, controllers);
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);


            if (bootTimeScan) {
                final AtomicReference<ModelNode> deploymentOperation = new AtomicReference<ModelNode>();
                final AtomicReference<ModelNode> deploymentResults = new AtomicReference<ModelNode>();
                final CountDownLatch scanDoneLatch = new CountDownLatch(1);
                final CountDownLatch deploymentDoneLatch = new CountDownLatch(1);
                final DeploymentOperations deploymentOps = new BootTimeScannerDeployment(deploymentOperation, deploymentDoneLatch, deploymentResults, scanDoneLatch);

                scheduledExecutorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            bootTimeScanner.oneOffScan(deploymentOps);
                        } finally {
                            scanDoneLatch.countDown();
                        }
                    }
                });
                boolean interrupted = false;
                try {
                    scanDoneLatch.await();

                    final ModelNode op = deploymentOperation.get();
                    if (op != null) {
                        final ModelNode result = new ModelNode();
                        final PathAddress opPath = PathAddress.pathAddress(op.get(OP_ADDR));
                        final OperationStepHandler handler = context.getRootResourceRegistration().getOperationHandler(opPath, op.get(OP).asString());
                        context.addStep(result, op, handler, OperationContext.Stage.MODEL);
                        try {
                            stepCompleted = true;
                            context.completeStep();
                        } finally {
                            deploymentResults.set(result);
                            deploymentDoneLatch.countDown();
                        }
                    } else {
                        stepCompleted = true;
                        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                    throw new RuntimeException(e);
                } finally {
                    deploymentDoneLatch.countDown();
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (!stepCompleted) {
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        for (SimpleAttributeDefinition atr : ALL_ATTRIBUTES) {
            atr.validateAndSet(operation, resource.getModel());
        }
    }

    protected void performRuntime(final OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> newControllers, final ScheduledExecutorService executorService,
                                  final FileSystemDeploymentService bootTimeScanner) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final String path = DeploymentScannerDefinition.PATH.resolveModelAttribute(context, operation).asString();
        final Boolean enabled = SCAN_ENABLED.resolveModelAttribute(context, operation).asBoolean();
        final Integer interval = SCAN_INTERVAL.resolveModelAttribute(context, operation).asInt();
        final String relativeTo = operation.hasDefined(CommonAttributes.RELATIVE_TO) ? RELATIVE_TO.resolveModelAttribute(context, operation).asString() : null;
        final Boolean autoDeployZip = AUTO_DEPLOY_ZIPPED.resolveModelAttribute(context, operation).asBoolean();
        final Boolean autoDeployExp = AUTO_DEPLOY_EXPLODED.resolveModelAttribute(context, operation).asBoolean();
        final Boolean autoDeployXml = AUTO_DEPLOY_XML.resolveModelAttribute(context, operation).asBoolean();
        final Long deploymentTimeout = DEPLOYMENT_TIMEOUT.resolveModelAttribute(context, operation).asLong();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        DeploymentScannerService.addService(serviceTarget, name, relativeTo, path, interval, TimeUnit.MILLISECONDS,
                autoDeployZip, autoDeployExp, autoDeployXml, enabled, deploymentTimeout, newControllers, bootTimeScanner, executorService, verificationHandler);

    }


    /**
     * Rollback runtime changes made in {@link #performRuntime(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, org.jboss.as.controller.ServiceVerificationHandler, java.util.List, ScheduledExecutorService, FileSystemDeploymentService}.
     * <p>
     * This default implementation removes all services in the given list of {@code controllers}. The contents of
     * {@code controllers} is the same as what was in the {@code newControllers} parameter passed to {@code performRuntime()}
     * when that method returned.
     * </p>
     *
     * @param context     the operation context
     * @param operation   the operation being executed
     * @param model       persistent configuration model node that corresponds to the address of {@code operation}
     * @param controllers holder for the {@link ServiceController} for any new services installed by
     *                    {@link #performRuntime(org.jboss.as.controller.OperationContext, org.jboss.dmr.ModelNode, org.jboss.dmr.ModelNode, org.jboss.as.controller.ServiceVerificationHandler, java.util.List}
     */
    protected void rollbackRuntime(OperationContext context, final ModelNode operation, final ModelNode model, List<ServiceController<?>> controllers) {
        for (ServiceController<?> controller : controllers) {
            context.removeService(controller.getName());
        }
    }

    private static class BootTimeScannerDeployment implements DeploymentOperations {
        private final AtomicReference<ModelNode> deploymentOperation;
        private final CountDownLatch deploymentDoneLatch;
        private final AtomicReference<ModelNode> deploymentResults;
        private final CountDownLatch scanDoneLatch;

        public BootTimeScannerDeployment(final AtomicReference<ModelNode> deploymentOperation, final CountDownLatch deploymentDoneLatch, final AtomicReference<ModelNode> deploymentResults, final CountDownLatch scanDoneLatch) {
            this.deploymentOperation = deploymentOperation;
            this.deploymentDoneLatch = deploymentDoneLatch;
            this.deploymentResults = deploymentResults;
            this.scanDoneLatch = scanDoneLatch;
        }

        @Override
        public Future<ModelNode> deploy(final ModelNode operation, final ScheduledExecutorService scheduledExecutor) {
            try {
                deploymentOperation.set(operation);
                final FutureTask<ModelNode> task = new FutureTask<ModelNode>(new Callable<ModelNode>() {
                    @Override
                    public ModelNode call() throws Exception {
                        deploymentDoneLatch.await();
                        return deploymentResults.get();
                    }
                });
                scheduledExecutor.submit(task);
                return task;
            } finally {
                scanDoneLatch.countDown();
            }
        }

        @Override
        public Set<String> getDeploymentNames() {
            return Collections.emptySet();
        }

        @Override
        public void close() throws IOException {

        }
    }
}
