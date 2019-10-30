/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.concurrent.deployers.injection.ContextServiceResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.injection.ManagedExecutorServiceResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.injection.ManagedScheduledExecutorServiceResourceReferenceProcessor;
import org.jboss.as.ee.concurrent.deployers.injection.ManagedThreadFactoryResourceReferenceProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * DUP that adds the {@Link EEResourceReferenceProcessorRegistry} to the deployment, and adds the bean validation resolvers.
 *
 * @author Stuart Douglas
 */
public class ResourceReferenceRegistrySetupProcessor implements DeploymentUnitProcessor {


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.getParent() == null) {
            final EEResourceReferenceProcessorRegistry registry = new EEResourceReferenceProcessorRegistry();
            registry.registerResourceReferenceProcessor(ContextServiceResourceReferenceProcessor.INSTANCE);
            registry.registerResourceReferenceProcessor(ManagedExecutorServiceResourceReferenceProcessor.INSTANCE);
            registry.registerResourceReferenceProcessor(ManagedScheduledExecutorServiceResourceReferenceProcessor.INSTANCE);
            registry.registerResourceReferenceProcessor(ManagedThreadFactoryResourceReferenceProcessor.INSTANCE);
            deploymentUnit.putAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY, registry);
        } else{
            deploymentUnit.putAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY, deploymentUnit.getParent().getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY));
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
