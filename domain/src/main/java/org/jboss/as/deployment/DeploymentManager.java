/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.deployment;

import org.jboss.msc.service.BatchBuilder;
<<<<<<< HEAD
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
=======
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
>>>>>>> bf5e5784e281e51c89497aee3feab2adcaf0b0ea
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.services.VFSMountService;
import org.jboss.msc.value.Values;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.IOException;
<<<<<<< HEAD
import java.util.Arrays;
=======
import java.util.Collections;
>>>>>>> bf5e5784e281e51c89497aee3feab2adcaf0b0ea
import java.util.concurrent.Executors;

/**
 * (TEMPORARY) Deployment manager used to kick off a deployment for a virtual file root.
 *
 * @author John E. Bailey
 */
public class DeploymentManager implements Service<DeploymentManager> {
    private static final ServiceName MOUNT_SERVICE_NAME = ServiceName.JBOSS.append("mounts");
    private final ServiceContainer serviceContainer;
    private TempFileProvider tempFileProvider;

    public DeploymentManager(final ServiceContainer serviceContainer) {
        this.serviceContainer = serviceContainer;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        try {
            tempFileProvider = TempFileProvider.create("mount", Executors.newScheduledThreadPool(2));
        } catch(IOException e) {
            throw new StartException("Failed to create temp file provider", e);
        }
    }

    @Override
    public void stop(final StopContext context) {
        VFSUtils.safeClose(tempFileProvider);
    }

    @Override
    public DeploymentManager getValue() throws IllegalStateException {
        return this;
    }

    /**
     * Deploy a virtual file as a deployment.
     * 
     * @param deploymentRoot The root to deploy
     */
<<<<<<< HEAD
    public final void deploy(final VirtualFile deploymentRoot) {
=======
    public final void deploy(final VirtualFile deploymentRoot) throws DeploymentException {
>>>>>>> bf5e5784e281e51c89497aee3feab2adcaf0b0ea
        final ServiceContainer serviceContainer = this.serviceContainer;
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        final String deploymentPath = deploymentRoot.getPathName();
        try {
            // Setup VFS mount service
<<<<<<< HEAD
            final ServiceName mountServiceName = MOUNT_SERVICE_NAME.append(deploymentPath);
            final VFSMountService vfsMountService = new VFSMountService(deploymentRoot.getPathName(), tempFileProvider, false);
            batchBuilder.addServiceValueIfNotExist(mountServiceName, Values.immediateValue(vfsMountService))
                .setInitialMode(ServiceController.Mode.ON_DEMAND);

            // Setup deploymentRoot service
            final ServiceName deploymentServiceName = DeploymentService.DEPLOYMENT_SERVICE_NAME.append(deploymentPath);
            final DeploymentService deploymentService = new DeploymentService();
            batchBuilder.addService(deploymentServiceName, deploymentService)
                .addDependency(mountServiceName).toMethod(DeploymentService.DEPLOYMENT_ROOT_SETTER, Arrays.asList(vfsMountService));

            // Setup deploymentRoot processor service
            final ServiceName deploymentServiceProcessorName = DeploymentProcessorService.DEPLOYMENT_PROCESSOR_SERVICE_NAME.append(deploymentPath);
            final DeploymentProcessorService deploymentProcessorService = new DeploymentProcessorService();
            batchBuilder.addService(deploymentServiceProcessorName, deploymentProcessorService)
                .addDependency(deploymentServiceName).toMethod(DeploymentProcessorService.DEPLOYMENT_SERVICE_SETTER, Arrays.asList(deploymentService));

            // Install the batch.
            batchBuilder.install();
        } catch(Throwable t) {
            throw new RuntimeException(t); // Throw something real...
        }
    }

=======
            // TODO: We should make sure this is an archive first...
            final ServiceName mountServiceName = MOUNT_SERVICE_NAME.append(deploymentPath);
            final VFSMountService vfsMountService = new VFSMountService(deploymentRoot.getPathName(), tempFileProvider, false);
            batchBuilder.addService(mountServiceName, vfsMountService);
                //.setInitialMode(ServiceController.Mode.ON_DEMAND);

            // Determine which deployment chain to use for this deployment
            final ServiceName deploymentChainServiceName = determineDeploymentChain();

            // Determine which deployment module loader to use for this deployment
            final ServiceName deploymentModuleLoaderServiceName = determineDeploymentModuleLoader();

            // Setup deployment service
            final ServiceName deploymentServiceName = DeploymentService.SERVICE_NAME.append(deploymentPath);
            final DeploymentService deploymentService = new DeploymentService(deploymentPath);
            final BatchServiceBuilder<?> deploymentServiceBuilder = batchBuilder.addService(deploymentServiceName, deploymentService);
            deploymentServiceBuilder.addDependency(mountServiceName)
                .toMethod(DeploymentService.DEPLOYMENT_ROOT_SETTER, Collections.singletonList(Values.immediateValue(deploymentRoot)));
            deploymentServiceBuilder.addDependency(deploymentChainServiceName)
                .toMethod(DeploymentService.DEPLOYMENT_CHAIN_SETTER, Collections.singletonList(Values.injectedValue()));
            deploymentServiceBuilder.addDependency(deploymentModuleLoaderServiceName)
                .toMethod(DeploymentService.DEPLOYMENT_MODULE_LOADER_SETTER, Collections.singletonList(Values.injectedValue()));

            // Setup deployment listener
            final DeploymentServiceListener deploymentServiceListener = new DeploymentServiceListener();
            batchBuilder.addListener(deploymentServiceListener);

            // Install the batch.
            batchBuilder.install();
            deploymentServiceListener.waitForCompletion(); // Waiting for now.  This should not block at this point long term...
        } catch(DeploymentException e) {
            throw e;
        } catch(Throwable t) {
            throw new DeploymentException(t);
        }
    }

    protected ServiceName determineDeploymentChain() {
        return null; // TODO:  Determine the chain
    }

    protected ServiceName determineDeploymentModuleLoader() {
        return null; // TODO:  Determine the loader
    }
>>>>>>> bf5e5784e281e51c89497aee3feab2adcaf0b0ea
}
