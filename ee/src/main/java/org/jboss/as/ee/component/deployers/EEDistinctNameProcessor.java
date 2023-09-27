/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Examines a deployment unit and its top level parent to check for any distinct-name that's configured in the
 * deployment descriptor(s) of the deployment units. If a top level deployment unit has a distinct-name configured
 * then it will be applied to all sub-deployments in that unit (unless the sub-deployment has overridden the distinct-name)
 *
 * @author Stuart Douglas
 * @author Jaikiran Pai
 */
public final class EEDistinctNameProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription module = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        if (module == null) {
            return;
        }
        // see if the deployment unit has an explicit distinct-name
        final String distinctName = deploymentUnit.getAttachment(org.jboss.as.ee.structure.Attachments.DISTINCT_NAME);
        if (distinctName != null) {
            module.setDistinctName(distinctName);
            return;
        }
        // check the parent DU for any explicit distinct-name
        if (deploymentUnit.getParent() != null) {
            final DeploymentUnit parentDU = deploymentUnit.getParent();
            final String distinctNameInParentDeployment = parentDU.getAttachment(org.jboss.as.ee.structure.Attachments.DISTINCT_NAME);
            if (distinctNameInParentDeployment != null) {
                module.setDistinctName(distinctNameInParentDeployment);
            }
            return;
        }
    }
}
