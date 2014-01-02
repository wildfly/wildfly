/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.deployers.ra.processors;

import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.subsystems.resourceadapters.ModifiableResourceAdapter;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdaptersSubsystemService;
import org.jboss.as.connector.util.CopyOnWriteArrayListMultiMap;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;

public class RaXmlDependencyProcessor implements DeploymentUnitProcessor {


    /**
     * Add dependencies for modules required for ra deployments
     *
     * @param phaseContext the deployment unit context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (phaseContext.getDeploymentUnit().getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY) == null) {
            return;  // Skip non ra deployments
        }
        CopyOnWriteArrayListMultiMap<String,ServiceName> resourceAdaptersMap = phaseContext.getDeploymentUnit().getAttachment(ResourceAdaptersSubsystemService.ATTACHMENT_KEY);
        String deploymentUnitPrefix = "";
        if (deploymentUnit.getParent() != null) {
            deploymentUnitPrefix = deploymentUnit.getParent().getName() + "#";
        }

        final String deploymentUnitName = deploymentUnitPrefix + deploymentUnit.getName();
        if (resourceAdaptersMap != null && resourceAdaptersMap.get(deploymentUnitName) != null) {
            for (ServiceName serviceName : resourceAdaptersMap.get(deploymentUnitName)) {

                phaseContext.addDeploymentDependency(serviceName, AttachmentKey
                        .create(ModifiableResourceAdapter.class));
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
