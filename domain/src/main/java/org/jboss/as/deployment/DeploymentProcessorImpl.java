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
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitContextImpl;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.vfs.VirtualFile;

import java.util.Collection;

/**
 * Default implementation of DeploymentProcessor.
 * 
 * @author John E. Bailey
 */
public class DeploymentProcessorImpl implements DeploymentProcessor {

    private final ServiceContainer serviceContainer;
    private final DeploymentChain defaultChain;

    public DeploymentProcessorImpl(final DeploymentChain defaultChain, ServiceContainer serviceContainer) {
        this.defaultChain = defaultChain;
        this.serviceContainer = serviceContainer;
    }

    @Override
    public void processDeployment(String name, VirtualFile deploymentRoot) throws DeploymentUnitProcessingException {
        final DeploymentUnitContextImpl context = new DeploymentUnitContextImpl(name, deploymentRoot);
        processChain(context);
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        processDeploymentItems(context, batchBuilder);
    }

    @Override
    public void processDeployment(String name, VirtualFile deploymentRoot, BatchBuilder batchBuilder) throws DeploymentUnitProcessingException {
        final DeploymentUnitContextImpl context = new DeploymentUnitContextImpl(name, deploymentRoot);
        processChain(context);
        final BatchBuilder subBatchBuilder = batchBuilder.subBatchBuilder();
        processDeploymentItems(context, subBatchBuilder);
    }

    private void processChain(final DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final DeploymentChain deploymentChain = getChain(context);
        deploymentChain.processDeployment(context);
    }

    private void processDeploymentItems(DeploymentUnitContextImpl context, BatchBuilder batchBuilder) {
        final Collection<DeploymentItem> deploymentItems = context.getDeploymentItems();
        for(DeploymentItem deploymentItem : deploymentItems) {
            deploymentItem.install(batchBuilder);
        }
    }

    private DeploymentChain getChain(final DeploymentUnitContext context) {
        // TODO: how do we do this correctly......
        return defaultChain;
    }
}
