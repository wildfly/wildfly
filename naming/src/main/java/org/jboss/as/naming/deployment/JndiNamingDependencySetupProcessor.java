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

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.as.naming.service.NamingService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;

/**
 * Adds the JNDI dependencies map to a deployment
 *
 * @author Stuart Douglas
 */
public class JndiNamingDependencySetupProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        deploymentUnit.putAttachment(org.jboss.as.server.deployment.Attachments.JNDI_DEPENDENCIES, Collections.newSetFromMap(new ConcurrentHashMap<ServiceName, Boolean>()));

        //this will always be up but we need to make sure the naming service is
        //not shut down before the deployment is undeployed when the container is shut down
        phaseContext.addToAttachmentList(Attachments.NEXT_PHASE_DEPS, NamingService.SERVICE_NAME);
    }

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
