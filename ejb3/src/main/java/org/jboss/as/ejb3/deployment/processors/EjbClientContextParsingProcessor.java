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
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;

/**
 * Deployment unit processor that sets up a dependency for the {@link DeploymentPhaseContext} on the appropriate
 * {@link org.jboss.ejb.client.EJBClientContext} so that the subsequent phase will have the {@link org.jboss.ejb.client.EJBClientContext}
 * available as an attachment in the deployment unit.
 *
 * @author Stuart Douglas
 */
public class EjbClientContextParsingProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(EjbClientContextParsingProcessor.class);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parentDeploymentUnit = deploymentUnit.getParent();
        final ServiceName ejbClientContextServiceName;
        // The top level parent deployment unit will have the attachment containing the EJB client context
        // service name
        if (parentDeploymentUnit != null) {
            ejbClientContextServiceName = parentDeploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT_SERVICE_NAME);
        } else {
            ejbClientContextServiceName = deploymentUnit.getAttachment(EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT_SERVICE_NAME);
        }
        if (ejbClientContextServiceName == null) {
            throw new IllegalStateException("Deployment unit " + deploymentUnit + " doesn't contain the service name for EJB client context");
        }
        logger.debug("Using " + ejbClientContextServiceName + " as the EJB client context service for deployment unit " + deploymentUnit);
        // add the attachments/dependencies in the DU
        phaseContext.addDeploymentDependency(ejbClientContextServiceName, EjbDeploymentAttachmentKeys.EJB_CLIENT_CONTEXT);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
