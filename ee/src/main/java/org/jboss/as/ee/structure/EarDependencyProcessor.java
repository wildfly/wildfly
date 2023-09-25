/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Services;

import static org.jboss.as.server.deployment.Attachments.NEXT_PHASE_DEPS;
import static org.jboss.as.server.deployment.Attachments.SUB_DEPLOYMENTS;

/**
 * Processor which ensures that subdeployments of an EAR all synchronize before the next phase.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EarDependencyProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        if (phaseContext.getAttachment(Attachments.DEPLOYMENT_TYPE) == DeploymentType.EAR) {
            final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
            // Make sure the next phase of this EAR depends on this phase of not just the EAR but also all subdeployments
            for (DeploymentUnit subdeployment : deploymentUnit.getAttachmentList(SUB_DEPLOYMENTS)) {
                phaseContext.addToAttachmentList(NEXT_PHASE_DEPS, Services.deploymentUnitName(deploymentUnit.getName(), subdeployment.getName(), phaseContext.getPhase()));
            }
        }
    }
}
