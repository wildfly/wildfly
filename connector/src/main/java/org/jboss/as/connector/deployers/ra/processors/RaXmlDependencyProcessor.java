/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.deployers.ra.processors;

import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.subsystems.resourceadapters.ModifiableResourceAdapter;
import org.jboss.as.connector.subsystems.resourceadapters.ConfiguredAdaptersService;
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
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (phaseContext.getDeploymentUnit().getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY) == null) {
            return;  // Skip non ra deployments
        }
        CopyOnWriteArrayListMultiMap<String,ServiceName> resourceAdaptersMap = phaseContext.getDeploymentUnit().getAttachment(ConfiguredAdaptersService.ATTACHMENT_KEY);
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
}
