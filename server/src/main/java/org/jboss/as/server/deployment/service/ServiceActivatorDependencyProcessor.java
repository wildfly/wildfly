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

package org.jboss.as.server.deployment.service;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.ServicesAttachment;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceActivator;

/**
 * Deployment processor that adds required dependencies for executing service activators.
 *
 * @author John Bailey
 */
public class ServiceActivatorDependencyProcessor implements DeploymentUnitProcessor {

    private static final ModuleDependency MSC_DEP = new ModuleDependency(Module.getBootModuleLoader(), ModuleIdentifier.create("org.jboss.msc"), false, false, false, false);

    /**
     * Add the dependencies if the deployment contains a service activator loader entry.
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final ResourceRoot deploymentRoot = phaseContext.getDeploymentUnit().getAttachment(Attachments.DEPLOYMENT_ROOT);
        final ModuleSpecification moduleSpecification = phaseContext.getDeploymentUnit().getAttachment(
                Attachments.MODULE_SPECIFICATION);
        if(deploymentRoot == null)
            return;
        final ServicesAttachment servicesAttachments = phaseContext.getDeploymentUnit().getAttachment(Attachments.SERVICES);
        if(servicesAttachments != null && !servicesAttachments.getServiceImplementations(ServiceActivator.class.getName()).isEmpty()) {
            moduleSpecification.addSystemDependency(MSC_DEP);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
