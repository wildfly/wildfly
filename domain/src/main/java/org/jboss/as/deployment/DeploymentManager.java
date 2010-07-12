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
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
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
import java.util.Collections;
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
    public final void deploy(final VirtualFile deploymentRoot) throws DeploymentException {
        final ServiceContainer serviceContainer = this.serviceContainer;
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        final String deploymentPath = deploymentRoot.getPathName();
        try {
            // Setup VFS mount service
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
            deploymentServiceListener.waitForCompletion();
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
}
