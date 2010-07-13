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

import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.as.deployment.module.DeploymentModuleLoader;
import org.jboss.as.deployment.module.DeploymentModuleService;
import org.jboss.as.deployment.unit.DeploymentChain;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VirtualFile;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.attachVirtualFile;

/**
 * Service that represents a deployment.  Should be used as a dependency for all services registered for the deployment.
 * When started this service will initialize the deployment process and will install additional services to support the
 * specific deployment type.
 *
 * @author John E. Bailey
 */
public class DeploymentService implements Service<DeploymentService> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("deployment");
    private static Logger logger = Logger.getLogger("org.jboss.as.deployment");

    static final Method DEPLOYMENT_ROOT_SETTER;
    static final Method DEPLOYMENT_CHAIN_SETTER;
    static final Method DEPLOYMENT_MODULE_LOADER_SETTER;
    static {
        try {
            DEPLOYMENT_ROOT_SETTER = DeploymentService.class.getMethod("setDeploymentRoot", VirtualFile.class);
            DEPLOYMENT_CHAIN_SETTER = DeploymentService.class.getMethod("setDeploymentChain", DeploymentChain.class);
            DEPLOYMENT_MODULE_LOADER_SETTER = DeploymentService.class.getMethod("setDeploymentModuleLoader", DeploymentModuleLoader.class);
        } catch(NoSuchMethodException e) {
            throw new RuntimeException(e);  // Gross....
        }
    }
    private final String deploymentName;
    private VirtualFile deploymentRoot;
    private DeploymentChain deploymentChain;
    private DeploymentModuleLoader deploymentModuleLoader;

    public DeploymentService(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        if(deploymentRoot == null) throw new StartException("DeploymentService requires a deployment root to start");
        if(deploymentChain == null) throw new StartException("DeploymentService requires a deployment chain to start");
        if(deploymentModuleLoader == null) throw new StartException("DeploymentService requires a deployment deployment ModuleLoader to start");

        final ServiceController<?> serviceController = context.getController();
        final ServiceName deploymentServiceName = serviceController.getName();
        final String deploymentPath = deploymentRoot.getPathName();
        
        // Create the deployment unit context
        final DeploymentUnitContextImpl deploymentUnitContext = new DeploymentUnitContextImpl(deploymentName);
        attachVirtualFile(deploymentUnitContext, deploymentRoot);

        // Execute the deployment chain
        final DeploymentChain deploymentChain = this.deploymentChain;
        logger.debugf("Deployment processor starting with chain: %s", deploymentChain);
        try {
            deploymentChain.processDeployment(deploymentUnitContext);
        } catch(DeploymentUnitProcessingException e) {
            throw new StartException("Failed to process deployment chain.", e);
        }
        
        // Setup batch for the next phases of deployment
        final ServiceContainer serviceContainer = serviceController.getServiceContainer();
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();

        // Add batch level dependency on this service
        batchBuilder.addDependency(deploymentServiceName);

        // Setup deployment module service
        final ServiceName moduleServiceName = DeploymentModuleService.SERVICE_NAME.append(deploymentPath);
        DeploymentModuleService deploymentModuleService = null;
        final ModuleConfig moduleConfig = deploymentUnitContext.getAttachment(ModuleConfig.ATTACHMENT_KEY);
        if(moduleConfig != null) {
            deploymentModuleService = new DeploymentModuleService(deploymentModuleLoader, moduleConfig);
            final BatchServiceBuilder<?> moduleServiceBuilder = batchBuilder.addService(moduleServiceName, deploymentModuleService);
            for(ModuleConfig.Dependency dependency : moduleConfig.getDependencies()) {
                // TODO determine whether the dependency comes from the deployment module loader and if so add a service dep
            }
        }
        
        // Setup deployment item processor service
        final ServiceName deploymentItemProcessorName = DeploymentItemProcessor.SERVICE_NAME.append(deploymentPath);
        final DeploymentItemProcessor deploymentItemProcessor = new DeploymentItemProcessor(deploymentUnitContext);
        final BatchServiceBuilder<?> itemProcessorServiceBuilder = batchBuilder.addService(deploymentItemProcessorName, deploymentItemProcessor);
        if(deploymentModuleService != null) {
            itemProcessorServiceBuilder.addDependency(moduleServiceName).toMethod(DeploymentItemProcessor.DEPLOYMENT_MODULE_SETTER, Collections.singletonList(deploymentModuleService));
        }

        // Install the batch
        try {
            batchBuilder.install();
        } catch(ServiceRegistryException e) {
            throw new StartException("Failed to install deployment phase batch for " + deploymentName, e);
        }
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public DeploymentService getValue() throws IllegalStateException {
        return this;
    }

    public void setDeploymentRoot(final VirtualFile deploymentRoot) {
        this.deploymentRoot = deploymentRoot;
    }

    public void setDeploymentChain(DeploymentChain deploymentChain) {
        this.deploymentChain = deploymentChain;
    }

    public VirtualFile getDeploymentRoot() {
        return deploymentRoot;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentModuleLoader(DeploymentModuleLoader deploymentModuleLoader) {
        this.deploymentModuleLoader = deploymentModuleLoader;
    }
}
