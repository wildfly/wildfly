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
import org.jboss.msc.service.ServiceUtils;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
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
    private static Logger logger = Logger.getLogger("org.jboss.as.deployment");

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

        // Run through each deployment
        try {
            for(VirtualFile deploymentRoot : deploymentRoots) {
                if(!deploymentRoot.exists())
                    throw new DeploymentException("Deployment root does not exist." + deploymentRoot);
                deploy(deploymentRoot, batchBuilder);
            }

            // Install the batch.
            batchBuilder.install();
            deploymentServiceListener.waitForCompletion(); // Waiting for now.  This should not block at this point long term...
        } catch(DeploymentException e) {
            throw e;
        } catch(Throwable t) {
            throw new DeploymentException(t);
        }
    }

    private void deploy(final VirtualFile deploymentRoot, final BatchBuilder batchBuilder) throws DeploymentException {
        final String deploymentPath = deploymentRoot.getPathName();
        final ServiceName deploymentServiceName = DeploymentService.SERVICE_NAME.append(deploymentPath);
        if(deploymentMap.putIfAbsent(deploymentRoot, deploymentServiceName) != null)
            throw new DeploymentException("Deployment already exists for deployment root: " + deploymentRoot);

        // Create a sub-batch for this deployment
        final BatchBuilder subBatchBuilder = batchBuilder.subBatchBuilder();

        // TODO: Mount properly........

        // Determine which deployment chain to use for this deployment
        final DeploymentChain deploymentChain = determineDeploymentChain(deploymentRoot);

        // Determine which deployment module loader to use for this deployment
        final DeploymentModuleLoader deploymentModuleLoader = determineDeploymentModuleLoader(deploymentRoot);
        DeploymentModuleLoaderSelector.CURRENT_MODULE_LOADER.set(deploymentModuleLoader);

        // Setup deployment service
        subBatchBuilder.addService(deploymentServiceName, new DeploymentService(deploymentPath));
        // Add batch level dependency on the deployment service
        subBatchBuilder.addDependency(deploymentServiceName);

        // Create the deployment unit context
        final DeploymentUnitContextImpl deploymentUnitContext = new DeploymentUnitContextImpl(deploymentPath);
        attachVirtualFile(deploymentUnitContext, deploymentRoot);

        // Execute the deployment chain
        logger.debugf("Deployment processor starting with chain: %s", deploymentChain);
        try {
            deploymentChain.processDeployment(deploymentUnitContext);
        } catch(DeploymentUnitProcessingException e) {
            throw new DeploymentException("Failed to process deployment chain.", e);
        }

        // Setup deployment module
        Module module = null;
        final ModuleConfig moduleConfig = deploymentUnitContext.getAttachment(ModuleConfig.ATTACHMENT_KEY);
        if(moduleConfig != null) {
            try {
                module = deploymentModuleLoader.loadModule(moduleConfig.getIdentifier());
            } catch(ModuleLoadException e) {
                throw new DeploymentException("Faild to load deployment module.  The module spec was likely not added to the deployment module loader", e);
            }
        }

        final ClassLoader currentCl = getContextClassLoader();
        if(module != null) {
            setContextClassLoader(module.getClassLoader());
        }
        try {
            // Construct an item context
            final DeploymentItemContext deploymentItemContext = new DeploymentItemContextImpl(module, subBatchBuilder);

            // Process all the deployment items with the item context
            final Collection<DeploymentItem> deploymentItems = deploymentUnitContext.getDeploymentItems();
            for(DeploymentItem deploymentItem : deploymentItems) {
                deploymentItem.install(deploymentItemContext);
            }
        } finally {
            setContextClassLoader(currentCl);
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
}
