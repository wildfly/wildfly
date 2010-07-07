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

import org.jboss.as.deployment.item.DeploymentItem;
import org.jboss.as.deployment.unit.DeploymentChain;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
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
import java.util.Collection;

import static org.jboss.as.deployment.attachment.VirtualFileAttachment.attachVirtualFile;

/**
 * Service that processes a deployment using a deployment chain.   
 *
 * @author John E. Bailey
 */
public class DeploymentProcessorService implements Service<Void> {
    public static final ServiceName DEPLOYMENT_PROCESSOR_SERVICE_NAME = DeploymentService.DEPLOYMENT_SERVICE_NAME.append("processor");
    private static Logger logger = Logger.getLogger("org.jboss.as.deployment");
    public static final Method DEPLOYMENT_SERVICE_SETTER;

    static {
        try {
            DEPLOYMENT_SERVICE_SETTER = DeploymentService.class.getMethod("setDeploymentService", DeploymentService.class);
        } catch(NoSuchMethodException e) {
            throw new RuntimeException(e);  // Gross....
        }
    }

    private DeploymentService deploymentService;

    @Override
    public void start(final StartContext context) throws StartException {
        final DeploymentService deploymentService = this.deploymentService;
        final ServiceController<?> controller = context.getController();
        final ServiceContainer serviceContainer = controller.getServiceContainer();

        final String deploymentName = deploymentService.getDeploymentName();
        final VirtualFile deploymentRoot = deploymentService.getDeploymentRoot();

        // Create the context
        final DeploymentUnitContextImpl deploymentContext = new DeploymentUnitContextImpl(deploymentName);
        attachVirtualFile(deploymentContext, deploymentRoot);

        // Execute the deployment chain
        final DeploymentChain deploymentChain = deploymentService.getDeploymentChain();
        logger.debugf("Deployment processor starting with chain: %s", deploymentChain);
        try {
            deploymentChain.processDeployment(deploymentContext);
        } catch(DeploymentUnitProcessingException e) {
            throw new StartException("Failed to process deployment chain.", e);
        }

        // Create batch for these items
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        //  Add batch level dependency for this deployment
        batchBuilder.addDependency(controller.getName());

        // Process all the deployment items with the batch
        final Collection<DeploymentItem> deploymentItems = deploymentContext.getDeploymentItems();
        for(DeploymentItem deploymentItem : deploymentItems) {
            deploymentItem.install(batchBuilder);
        }

        // Install the batch
        try {
            batchBuilder.install();
        } catch(ServiceRegistryException e) {
            throw new StartException("Failed to install deployment batch for " + deploymentName, e);
        }
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    public void setDeploymentService(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }
}
