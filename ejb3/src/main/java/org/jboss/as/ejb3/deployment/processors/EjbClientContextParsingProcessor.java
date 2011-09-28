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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.ejb3.remote.EjbClientContextService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Deployment unit processor that sets up the appropriate {@link org.jboss.ejb.client.EJBClientContext} for a deployment.
 *
 * @author Stuart Douglas
 */
public class EjbClientContextParsingProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        //TODO: this needs to figure out the correct service name from some kind of configuration

        deploymentUnit.putAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT_SERVICE_NAME, EjbClientContextService.DEFAULT_SERVICE_NAME);
        phaseContext.addDeploymentDependency(EjbClientContextService.DEFAULT_SERVICE_NAME, EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
