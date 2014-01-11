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
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.ServicesAttachment;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceActivatorContextImpl;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceRegistryException;

/**
 * Deployment processor responsible for executing any ServiceActivator instances for a deployment.
 *
 * @author John Bailey
 */
public class ServiceActivatorProcessor implements DeploymentUnitProcessor {

    /**
     * If the deployment has a module attached it will ask the module to load the ServiceActivator services.
     *
     * @param phaseContext the deployment unit context
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ServicesAttachment servicesAttachment = deploymentUnit.getAttachment(Attachments.SERVICES);
        if(servicesAttachment == null || servicesAttachment.getServiceImplementations(ServiceActivator.class.getName()).isEmpty())
            return; // Skip it if it has not been marked

        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        if (module == null)
            return; // Skip deployments with no module

        ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();
        if(System.getSecurityManager() != null) {
            //service registry allows you to modify internal server state across all deployments
            //if a security manager is present we use a version that has permission checks
            serviceRegistry = new SecuredServiceRegistry(serviceRegistry);
        }
        final ServiceActivatorContext serviceActivatorContext = new ServiceActivatorContextImpl(phaseContext.getServiceTarget(), serviceRegistry);
        for(ServiceActivator serviceActivator : module.loadService(ServiceActivator.class)) {
            try {
                serviceActivator.activate(serviceActivatorContext);
            } catch (ServiceRegistryException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
