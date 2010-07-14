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

package org.jboss.as.deployment;

import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceUtils;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.services.VFSMountService;
import org.jboss.msc.value.Values;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

/**
 * Deployment manager implementation.
 *
 * @author John E. Bailey
 */
public class DeploymentManagerImpl implements DeploymentManager, Service<DeploymentManager> {
    private static final ServiceName MOUNT_SERVICE_NAME = ServiceName.JBOSS.append("mounts");
    private final ServiceContainer serviceContainer;
    private final ConcurrentMap<VirtualFile, ServiceName> deploymentMap = new ConcurrentHashMap<VirtualFile, ServiceName>();
    private TempFileProvider tempFileProvider;

    public DeploymentManagerImpl(final ServiceContainer serviceContainer) {
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
     * @param deploymentRoots The roots to deploy
     */
    public final void deploy(final VirtualFile... deploymentRoots) throws DeploymentException {
        final ServiceContainer serviceContainer = this.serviceContainer;
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();

        // Setup deployment listener
        final DeploymentServiceListener deploymentServiceListener = new DeploymentServiceListener();
        batchBuilder.addListener(deploymentServiceListener);

        for(VirtualFile deploymentRoot : deploymentRoots) {
            final String deploymentPath = deploymentRoot.getPathName();
            final ServiceName deploymentServiceName = DeploymentService.SERVICE_NAME.append(deploymentPath);
            if(deploymentMap.putIfAbsent(deploymentRoot, deploymentServiceName) != null)
                throw new DeploymentException("Deployment already exists for deployment root: " + deploymentRoot);
            try {
                // Setup VFS mount service
                // TODO: We should make sure this is an archive first...
                final ServiceName mountServiceName = MOUNT_SERVICE_NAME.append(deploymentPath);
                final VFSMountService vfsMountService = new VFSMountService(deploymentRoot.getPathName(), tempFileProvider, false);
                batchBuilder.addService(mountServiceName, vfsMountService);

                // Determine which deployment chain to use for this deployment
                final ServiceName deploymentChainServiceName = determineDeploymentChain();

                // Determine which deployment module loader to use for this deployment
                final ServiceName deploymentModuleLoaderServiceName = determineDeploymentModuleLoader();

                // Setup deployment service
                final DeploymentService deploymentService = new DeploymentService(deploymentPath);
                deploymentService.setDeploymentListener(deploymentServiceListener);
                final BatchServiceBuilder<?> deploymentServiceBuilder = batchBuilder.addService(deploymentServiceName, deploymentService);
                deploymentServiceBuilder.addDependency(mountServiceName)
                    .toMethod(DeploymentService.DEPLOYMENT_ROOT_SETTER, Collections.singletonList(Values.immediateValue(deploymentRoot)));
                deploymentServiceBuilder.addDependency(deploymentChainServiceName)
                    .toMethod(DeploymentService.DEPLOYMENT_CHAIN_SETTER, Collections.singletonList(Values.injectedValue()));
                deploymentServiceBuilder.addDependency(deploymentModuleLoaderServiceName)
                    .toMethod(DeploymentService.DEPLOYMENT_MODULE_LOADER_SETTER, Collections.singletonList(Values.injectedValue()));

                // Install the batch.
                batchBuilder.install();
                deploymentServiceListener.waitForCompletion(); // Waiting for now.  This should not block at this point long term...
            } catch(DeploymentException e) {
                throw e;
            } catch(Throwable t) {
                throw new DeploymentException(t);
            }
        }
    }

    @Override
    public void undeploy(VirtualFile... deploymentRoots) throws DeploymentException {
        final ServiceContainer serviceContainer = this.serviceContainer;
        final ConcurrentMap<VirtualFile, ServiceName> deploymentMap = this.deploymentMap;
        final List<ServiceController<?>> serviceControllers = new ArrayList<ServiceController<?>>(deploymentRoots.length);
        for(VirtualFile deploymentRoot : deploymentRoots) {
            final ServiceName serviceName = deploymentMap.remove(deploymentRoot);
            if(serviceName == null)
                continue; // Maybe should be an exceptions
            final ServiceController<?> serviceController = serviceContainer.getService(serviceName);
            if(serviceController == null)
                throw new DeploymentException("Failed to undeploy " + deploymentRoot + ". Deployment service does not exist.");
            serviceControllers.add(serviceController);
        }
        ServiceUtils.undeployAll(new Runnable() {
            @Override
            public void run() {
                // Do something..
            }
        }, serviceControllers);
    }

    protected ServiceName determineDeploymentChain() {
        return null; // TODO:  Determine the chain
    }

    protected ServiceName determineDeploymentModuleLoader() {
        return null; // TODO:  Determine the loader
    }
}
