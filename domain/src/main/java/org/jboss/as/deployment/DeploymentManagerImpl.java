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

import org.jboss.as.deployment.item.DeploymentItem;
import org.jboss.as.deployment.item.DeploymentItemContext;
import org.jboss.as.deployment.item.DeploymentItemContextImpl;
import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleLoaderSelector;
import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.as.deployment.unit.DeploymentChain;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceUtils;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.services.VFSMountService;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.attachVirtualFile;

/**
 * Deployment manager implementation.
 *
 * @author John E. Bailey
 */
public class DeploymentManagerImpl implements DeploymentManager, Service<DeploymentManager> {
    private static final ServiceName MOUNT_SERVICE_NAME = ServiceName.JBOSS.append("mounts");
    private static Logger logger = Logger.getLogger("org.jboss.as.deployment");

    private final ServiceContainer serviceContainer;
    private final ConcurrentMap<VirtualFile, Deployment> deploymentMap = new ConcurrentHashMap<VirtualFile, Deployment>();
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
        // Phase 1
        final List<Deployment> deployments = deployDeploymentServices(deploymentRoots);
        // Phase 2
        executeDeploymentProcessors(deployments);
        // Phase 3
        executeDeploymentItems(deployments);
    }

    /**
     * Phase 1 - Initialize Deployment - Mount and deployment service batch
     */
    private List<Deployment> deployDeploymentServices(final VirtualFile... deploymentRoots) throws DeploymentException {
        final List<Deployment> deployments = new ArrayList<Deployment>(deploymentRoots.length);

        // Setup batch
        final ServiceContainer serviceContainer = this.serviceContainer;
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();

        // Setup deployment listener
        final DeploymentServiceListener deploymentServiceListener = new DeploymentServiceListener();
        batchBuilder.addListener(deploymentServiceListener);
        try {
            for(VirtualFile deploymentRoot : deploymentRoots) {
                final String deploymentName = deploymentRoot.getName();
                if(!deploymentRoot.exists())
                    throw new DeploymentException("Deployment root does not exist." + deploymentRoot);

                // Create the deployment unit context
                final DeploymentUnitContextImpl deploymentUnitContext = new DeploymentUnitContextImpl(deploymentName);
                attachVirtualFile(deploymentUnitContext, deploymentRoot);

                // Setup VFS mount service
                // TODO: We should make sure this is an archive first...
                final ServiceName mountServiceName = MOUNT_SERVICE_NAME.append(deploymentName);
                final VFSMountService vfsMountService = new VFSMountService(deploymentRoot.getPathName(), tempFileProvider, false);
                batchBuilder.addService(mountServiceName, vfsMountService)
                    .setInitialMode(ServiceController.Mode.ON_DEMAND);

                // Setup deployment service
                final ServiceName deploymentServiceName = DeploymentService.SERVICE_NAME.append(deploymentName);
                batchBuilder.addService(deploymentServiceName, new DeploymentService(deploymentName))
                    .setInitialMode(ServiceController.Mode.IMMEDIATE)
                    .addDependency(mountServiceName);

                final Deployment deployment = new Deployment(deploymentName, deploymentRoot, deploymentServiceName, deploymentUnitContext);
                // Register the deployment
                if(deploymentMap.putIfAbsent(deploymentRoot, deployment) != null)
                    throw new DeploymentException("Deployment already exists for deployment root: " + deploymentRoot);
                deployments.add(deployment);
            }
            // Install the batch.
            batchBuilder.install();
            deploymentServiceListener.waitForCompletion(); // Waiting for now.  This should not block at this point long term...
        } catch(DeploymentException e) {
            throw e;
        } catch(Throwable t) {
            throw new DeploymentException(t);
        }
        return deployments;
    }

    /**
     * Phase 2 - Execute deployment processors
     */
    private void executeDeploymentProcessors(final List<Deployment> deployments) throws DeploymentException {
        for(Deployment deployment : deployments) {
            final VirtualFile deploymentRoot = deployment.deploymentRoot;

            // Determine which deployment chain to use for this deployment
            final DeploymentChain deploymentChain = determineDeploymentChain(deploymentRoot);

            // Determine which deployment module loader to use for this deployment
            final DeploymentModuleLoader deploymentModuleLoader = determineDeploymentModuleLoader(deploymentRoot);
            DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(deploymentModuleLoader);

            final DeploymentUnitContext deploymentUnitContext = deployment.deploymentUnitContext;

            // Execute the deployment chain
            logger.debugf("Deployment processor starting with chain: %s", deploymentChain);
            try {
                deploymentChain.processDeployment(deploymentUnitContext);
            } catch(DeploymentUnitProcessingException e) {
                throw new DeploymentException("Failed to process deployment chain.", e);
            } finally {
                DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(null);
            }
        }
    }

    /**
     * Phase 3 - Create the module and execute the deployment items
     */
    private void executeDeploymentItems(final List<Deployment> deployments) throws DeploymentException {
        // Setup batch
        final ServiceContainer serviceContainer = this.serviceContainer;
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();

        // Setup deployment listener
        final DeploymentServiceListener deploymentServiceListener = new DeploymentServiceListener();
        batchBuilder.addListener(deploymentServiceListener);

        for(Deployment deployment : deployments) {
            final DeploymentUnitContextImpl deploymentUnitContext = deployment.deploymentUnitContext;

            // Setup deployment module
            // Determine which deployment module loader to use for this deployment
            final DeploymentModuleLoader deploymentModuleLoader = determineDeploymentModuleLoader(deployment.deploymentRoot);
            Module module = null;
            final ModuleConfig moduleConfig = deploymentUnitContext.getAttachment(ModuleConfig.ATTACHMENT_KEY);
            if(moduleConfig != null) {
                try {
                    module = deploymentModuleLoader.loadModule(moduleConfig.getIdentifier());
                } catch(ModuleLoadException e) {
                    throw new DeploymentException("Faild to load deployment module.  The module spec was likely not added to the deployment module loader", e);
                }
            }

            // Create a sub batch for the deployment
            final BatchBuilder subBatchBuilder = batchBuilder.subBatchBuilder();

            // Install dependency on the deployment service
            subBatchBuilder.addDependency(deployment.deploymentServiceName);

            final ClassLoader currentCl = getContextClassLoader();
            try {
                if(module != null) {
                    setContextClassLoader(module.getClassLoader());
                }
                DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(deploymentModuleLoader);

                // Construct an item context
                final DeploymentItemContext deploymentItemContext = new DeploymentItemContextImpl(module, subBatchBuilder);

                // Process all the deployment items with the item context
                final Collection<DeploymentItem> deploymentItems = deploymentUnitContext.getDeploymentItems();
                for(DeploymentItem deploymentItem : deploymentItems) {
                    deploymentItem.install(deploymentItemContext);
                }
            } finally {
                setContextClassLoader(currentCl);
                DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(null);
            }
        }

        // Install the batch.
        try {
            batchBuilder.install();
            deploymentServiceListener.waitForCompletion(); // Waiting for now.  This should not block at this point long term...
        } catch(ServiceRegistryException e) {
            throw new DeploymentException(e);
        }
    }

    @Override
    public void undeploy(VirtualFile... deploymentRoots) throws DeploymentException {
        final ServiceContainer serviceContainer = this.serviceContainer;
        final ConcurrentMap<VirtualFile, Deployment> deploymentMap = this.deploymentMap;
        final List<ServiceController<?>> serviceControllers = new ArrayList<ServiceController<?>>(deploymentRoots.length);
        for(VirtualFile deploymentRoot : deploymentRoots) {
            final Deployment deployment = deploymentMap.remove(deploymentRoot);
            if(deployment == null)
                continue; // Maybe should be an exceptions
            final ServiceController<?> serviceController = serviceContainer.getService(deployment.deploymentServiceName);
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

    private ClassLoader getContextClassLoader() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return Thread.currentThread().getContextClassLoader();
            }
        });
    }

    private void setContextClassLoader(final ClassLoader classLoader) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Thread.currentThread().setContextClassLoader(classLoader);
                return null;
            }
        });
    }

    protected DeploymentChain determineDeploymentChain(final VirtualFile deploymentRoot) {
        return null; // TODO:  Determine the chain
    }

    protected DeploymentModuleLoader determineDeploymentModuleLoader(final VirtualFile deploymentRoot) {
        return null; // TODO:  Determine the loader
    }

    private static final class Deployment {
        private final String name;
        private final VirtualFile deploymentRoot;
        private final ServiceName deploymentServiceName;
        private final DeploymentUnitContextImpl deploymentUnitContext;

        private Deployment(String name, VirtualFile deploymentRoot, ServiceName deploymentServiceName, DeploymentUnitContextImpl deploymentUnitContext) {
            this.name = name;
            this.deploymentRoot = deploymentRoot;
            this.deploymentServiceName = deploymentServiceName;
            this.deploymentUnitContext = deploymentUnitContext;
        }
    }
}
