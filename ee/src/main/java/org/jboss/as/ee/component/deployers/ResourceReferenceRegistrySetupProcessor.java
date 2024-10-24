/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.Attachments;
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
            deploymentUnit.putAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY, new EEResourceReferenceProcessorRegistry());
        } else{
            deploymentUnit.putAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY, deploymentUnit.getParent().getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY));
        }
    }
}
