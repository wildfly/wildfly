/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.deployment.EjbDeploymentAttachmentKeys;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * @author Stuart Douglas
 */
public class EjbCleanUpProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        deploymentUnit.removeAttachment(EjbDeploymentAttachmentKeys.EJB_INJECTIONS);
        deploymentUnit.removeAttachment(EjbDeploymentAttachmentKeys.APPLICATION_EXCEPTION_DETAILS);
        deploymentUnit.removeAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_DESCRIPTION);
        deploymentUnit.removeAttachment(EjbDeploymentAttachmentKeys.EJB_JAR_METADATA);
    }
}
