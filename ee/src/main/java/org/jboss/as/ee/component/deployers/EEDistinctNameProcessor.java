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

    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
