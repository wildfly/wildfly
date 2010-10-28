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

package org.jboss.as.model;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.attachVirtualFile;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.jar.Manifest;

import org.jboss.as.deployment.DeploymentFailureListener;
import org.jboss.as.deployment.DeploymentService;
import org.jboss.as.deployment.ServerDeploymentRepository;
import org.jboss.as.deployment.attachment.ManifestAttachment;
import org.jboss.as.deployment.chain.DeploymentChain;
import org.jboss.as.deployment.chain.DeploymentChainProvider;
import org.jboss.as.deployment.module.MountHandle;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartException;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * Helper class that can handle the runtime aspects of deploying and undeploying
 * on a server.
 *
 * @author Brian Stansberry
 */
class ServerDeploymentStartStopHandler implements Serializable {
    private static final long serialVersionUID = 4804538479425267270L;
    private static final Logger log = Logger.getLogger("org.jboss.as.deployment");


    <P> void deploy(final String deploymentName, final String runtimeName, final byte[] deploymentHash, final ServiceContainer serviceContainer,
            final UpdateResultHandler<?, P> resultHandler, final P param) {
        try {
            BatchBuilder batchBuilder = serviceContainer.batchBuilder();
            deploy(deploymentName, runtimeName, deploymentHash, batchBuilder, serviceContainer, resultHandler, param);
            batchBuilder.install();
        }
        catch (Exception e) {
            resultHandler.handleFailure(e, param);
        }
    }

    <P> void deploy(final String deploymentName, final String runtimeName, final byte[] deploymentHash,
            final BatchBuilder batchBuilder, final ServiceContainer serviceContainer,
            final UpdateResultHandler<?, P> resultHandler, final P param) {
        try {
            ServiceName deploymentServiceName = DeploymentService.getServiceName(deploymentName);
            // Add a listener so we can get ahold of the DeploymentService
            batchBuilder.addListener(new DeploymentServiceTracker<P>(deploymentServiceName, resultHandler, param));

            activate(deploymentName, runtimeName, deploymentHash, deploymentServiceName, new ServiceActivatorContextImpl(batchBuilder), serviceContainer);
        }
        catch (RuntimeException e) {
            resultHandler.handleFailure(e, param);
        }
    }

    <P> void redeploy(final String deploymentName, final String runtimeName, final byte[] deploymentHash,
            final ServiceContainer serviceContainer, final UpdateResultHandler<?, P> resultHandler, final P param) {
        try {
            ServiceName deploymentServiceName = DeploymentService.getServiceName(deploymentName);
            @SuppressWarnings("unchecked")
            final ServiceController<DeploymentService> controller = (ServiceController<DeploymentService>) serviceContainer.getService(deploymentServiceName);
            if(controller != null && controller.getMode() != ServiceController.Mode.REMOVE) {
                RedeploymentServiceTracker tracker = new RedeploymentServiceTracker(deploymentName);
                controller.addListener(tracker);
                synchronized (tracker) {
                    controller.setMode(ServiceController.Mode.REMOVE);
                    try {
                        tracker.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        if (resultHandler != null) {
                            resultHandler.handleFailure(e, param);
                            return;
                        }
                    }
                }
            }

            deploy(deploymentName, runtimeName, deploymentHash, serviceContainer, resultHandler, param);
        }
        catch (RuntimeException e) {
            if (resultHandler != null) {
                resultHandler.handleFailure(e, param);
            }
        }
    }

    <P> void undeploy(final String deploymentName, final ServiceContainer serviceContainer,
            final UpdateResultHandler<?, P> resultHandler, final P param) {
        try {
            ServiceName deploymentServiceName = DeploymentService.getServiceName(deploymentName);
            @SuppressWarnings("unchecked")
            final ServiceController<DeploymentService> controller = (ServiceController<DeploymentService>) serviceContainer.getService(deploymentServiceName);
            if(controller != null) {
                controller.addListener(new UndeploymentServiceTracker<P>(deploymentName, resultHandler, param));
                controller.setMode(ServiceController.Mode.REMOVE);
            }
            else if (resultHandler != null) {
                resultHandler.handleSuccess(null, param);
            }
        }
        catch (RuntimeException e) {
            if (resultHandler != null) {
                resultHandler.handleFailure(e, param);
            }
        }
    }

    private void activate(final String deploymentName, String runtimeName, final byte[] deploymentHash, final ServiceName deploymentServiceName, final ServiceActivatorContext context, final ServiceContainer serviceContainer) {
        log.infof("Activating deployment: %s", deploymentName);

        MountHandle handle = null;
        try {
            final ServerDeploymentRepository deploymentRepo = getDeploymentRepository(serviceContainer);

            // The mount point we will use for the repository file
            final VirtualFile deploymentRoot = VFS.getChild("content/" + runtimeName);

            // Mount virtual file
            try {
                Closeable mount = deploymentRepo.mountDeploymentContent(deploymentName, runtimeName, deploymentHash, deploymentRoot);
                handle = new MountHandle(mount);
            } catch (IOException e) {
                throw new RuntimeException("Failed to mount deployment archive", e);
            }

            final BatchBuilder batchBuilder = context.getBatchBuilder();
            // Create deployment service
            DeploymentService deploymentService = new DeploymentService(handle);
            BatchServiceBuilder<Void> serviceBuilder = batchBuilder.addService(deploymentServiceName, deploymentService);

            // Create a sub-batch for this deployment
            final BatchBuilder deploymentSubBatch = batchBuilder.subBatchBuilder();

            // Setup a batch level dependency on deployment service
            deploymentSubBatch.addDependency(deploymentServiceName);

            // Add a deployment failure listener to the batch
            deploymentSubBatch.addListener(new DeploymentFailureListener(deploymentServiceName));

            // Create the deployment unit context
            final DeploymentUnitContext deploymentUnitContext = new DeploymentUnitContextImpl(deploymentServiceName.getSimpleName(), deploymentSubBatch, serviceBuilder);
            attachVirtualFile(deploymentUnitContext, deploymentRoot);
            deploymentUnitContext.putAttachment(MountHandle.ATTACHMENT_KEY, handle);

            // Get the optional Manifest for this deployment
            try {
                Manifest manifest = VFSUtils.getManifest(deploymentRoot);
                if (manifest != null)
                    ManifestAttachment.attachManifest(deploymentUnitContext, manifest);
            } catch (IOException e) {
                throw new RuntimeException("Failed to get manifest for deployment " + deploymentRoot, e);
            }

            // Execute the deployment chain
            final DeploymentChainProvider deploymentChainProvider = DeploymentChainProvider.INSTANCE;
            final DeploymentChain deploymentChain = deploymentChainProvider.determineDeploymentChain(deploymentUnitContext);
            log.debugf("Executing deployment '%s' with chain: %s", deploymentName, deploymentChain);
            if(deploymentChain == null)
                throw new RuntimeException("Failed determine the deployment chain for deployment root: " + deploymentRoot);
            try {
                deploymentChain.processDeployment(deploymentUnitContext);
            } catch (DeploymentUnitProcessingException e) {
                throw new RuntimeException("Failed to process deployment chain.", e);
            }
        } catch(Throwable t) {
            VFSUtils.safeClose(handle);
            if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            throw new RuntimeException("Failed to activate deployment unit " + deploymentName, t);
        }
    }

    private ServerDeploymentRepository getDeploymentRepository(ServiceContainer serviceContainer) throws ServiceNotFoundException {
        @SuppressWarnings("unchecked")
        ServiceController<ServerDeploymentRepository> serviceController = (ServiceController<ServerDeploymentRepository>) serviceContainer.getRequiredService(ServerDeploymentRepository.SERVICE_NAME);
        return serviceController.getValue();
    }

//    private static String getFullyQualifiedDeploymentPath(String name) {
//        final String fileName = name;
//        String path = System.getProperty("jboss.server.deploy.dir");
//        return (path.endsWith(File.separator)) ? path + fileName : path + File.separator + fileName;
//    }

    private abstract static class AbstractDeploymentServiceTracker<P> extends AbstractServiceListener<Object> {

        protected final UpdateResultHandler<?, P> resultHandler;
        protected final P param;

        protected AbstractDeploymentServiceTracker(final UpdateResultHandler<?, P> resultHandler,
                final P param) {
            this.resultHandler = resultHandler;
            this.param = param;
        }

        @Override
        public void serviceFailed(ServiceController<?> controller, StartException reason) {

            if (resultHandler != null && isCorrectService(controller)) {
                resultHandler.handleFailure(reason, param);
                // Ignore any further notifications
                controller.removeListener(this);
            }
        }

        protected void recordResult(ServiceController<? extends Object> controller) {
            resultHandler.handleSuccess(null, param);
            // Ignore any further notifications
            controller.removeListener(this);
        }

        protected abstract boolean isCorrectService(ServiceController<?> service);

    }

    private static class DeploymentServiceTracker<P> extends AbstractDeploymentServiceTracker<P> {

        private final ServiceName deploymentServiceName;

        private DeploymentServiceTracker(final ServiceName deploymentServiceName,
                final UpdateResultHandler<?, P> resultHandler, final P param) {
            super(resultHandler, param);
            assert deploymentServiceName != null : "deploymentServiceName is null";
            this.deploymentServiceName = deploymentServiceName;
        }

        @Override
        public void serviceStarted(ServiceController<?> controller) {
            if (resultHandler != null && isCorrectService(controller)) {
                recordResult(controller);
            }
        }

        @Override
        protected boolean isCorrectService(ServiceController<?> controller) {
            return deploymentServiceName.equals(controller.getName());
        }

    }

    private static class UndeploymentServiceTracker<P> extends AbstractDeploymentServiceTracker<P> {

        private final String deploymentName;

        private UndeploymentServiceTracker(final String deploymentName,
                final UpdateResultHandler<?, P> resultHandler, final P param) {
            super(resultHandler, param);
            this.deploymentName = deploymentName;
        }

        @Override
        public void serviceRemoved(ServiceController<?> controller) {
            if (resultHandler != null) {
                recordResult(controller);
            }
            log.infof("Undeployed %s", deploymentName);
        }

        @Override
        protected boolean isCorrectService(ServiceController<?> controller) {
            // We are only registered with a single service
            return true;
        }

    }

    private static class RedeploymentServiceTracker extends AbstractServiceListener<Object> {

        private final String deploymentName;

        RedeploymentServiceTracker(final String deploymentName) {
            this.deploymentName = deploymentName;
        }

        @Override
        public void serviceRemoved(ServiceController<?> controller) {
            synchronized (this) {
                notifyAll();
            }
            log.infof("Undeployed %s", deploymentName);
        }

    }
}
