/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.naming.deployment;

import java.util.Set;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Adds a service that depends on all JNDI bindings from the deployment to be up.
 * <p/>
 * As binding services are not children of the root deployment unit service this service
 * is necessary to ensure the deployment is not considered complete until add bindings are up
 *
 * @author Stuart Douglas
 */
public class JndiNamingDependencyProcessor implements DeploymentUnitProcessor {

    private static final ServiceName JNDI_DEPENDENCY_SERVICE = ServiceName.of("jndiDependencyService");


    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        Set<ServiceName> dependencies = deploymentUnit.getAttachment(Attachments.JNDI_DEPENDENCIES);
        final ServiceName serviceName = serviceName(deploymentUnit);
        final ServiceBuilder<Void> serviceBuilder = phaseContext.getServiceTarget().addService(serviceName, Service.NULL);
        serviceBuilder.addDependencies(dependencies);
        if(deploymentUnit.getParent() != null) {
            serviceBuilder.addDependencies(deploymentUnit.getParent().getAttachment(Attachments.JNDI_DEPENDENCIES));
        }
        serviceBuilder.install();

    }

    public static ServiceName serviceName(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.getServiceName().append(JNDI_DEPENDENCY_SERVICE);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
