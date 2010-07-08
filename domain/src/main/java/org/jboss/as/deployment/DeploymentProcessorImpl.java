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
import org.jboss.as.deployment.unit.DeploymentChain;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.logging.Logger;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.VirtualFile;

import java.util.Collection;

/**
 * Default implementation of DeploymentProcessor.
 * 
 * @author John E. Bailey
 */
public class DeploymentProcessorImpl implements DeploymentProcessor, Service<DeploymentProcessor> {
    public static final ServiceName DEPLOYMENT_PROCESSOR_NAME = ServiceName.JBOSS.append("deployment", "processor");

    private static Logger logger = Logger.getLogger("org.jboss.as.deployment");

    private ServiceContainer serviceContainer;
    private DeploymentChain deploymentChain;

    @Override
    public void start(StartContext context) throws StartException {
        logger.debugf("Deployment processor starting with chain: %s", deploymentChain);
    }

    @Override
    public void stop(StopContext context) {
    }

    @Override
    public DeploymentProcessor getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public void processDeployment(String name, VirtualFile deploymentRoot) throws DeploymentUnitProcessingException {
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        process(name, deploymentRoot, batchBuilder);
    }

    @Override
    public void processDeployment(String name, VirtualFile deploymentRoot, BatchBuilder batchBuilder) throws DeploymentUnitProcessingException {
        final BatchBuilder subBatchBuilder = batchBuilder.subBatchBuilder();
        process(name, deploymentRoot, subBatchBuilder);
    }

    private void process(final String name, final VirtualFile deploymentRoot, final BatchBuilder batchBuilder) throws DeploymentUnitProcessingException {
        // Create the context
        final DeploymentUnitContextImpl context = new DeploymentUnitContextImpl(name, deploymentRoot);

        // Add service for this deployment unit
        final ServiceName deploymentUnitServiceName = DeploymentUnitContext.JBOSS_DEPLOYMENT_UNIT.append(name); 
        try {
            batchBuilder.addService(deploymentUnitServiceName, Service.NULL);
        } catch(DuplicateServiceException e) {
            throw new DeploymentUnitProcessingException(e, new Location(e.getStackTrace()[0].getFileName(), e.getStackTrace()[0].getLineNumber(), -1, null));
        }

        // Execute the deployment chain
        final DeploymentChain deploymentChain = this.deploymentChain;
        deploymentChain.processDeployment(context);

        //  Add batch level dependency for this deployment
        batchBuilder.addDependency(deploymentUnitServiceName);

        // Process all the deployment items with the batch
        final Collection<DeploymentItem> deploymentItems = context.getDeploymentItems();
        for(DeploymentItem deploymentItem : deploymentItems) {
            deploymentItem.install(batchBuilder);
        }
    }

    public void setServiceContainer(ServiceContainer serviceContainer) {
        this.serviceContainer = serviceContainer;
    }

    public void setDeploymentChain(DeploymentChain deploymentChain) {
        this.deploymentChain = deploymentChain;
    }
}
