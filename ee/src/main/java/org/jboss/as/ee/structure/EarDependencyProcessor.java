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

    public void undeploy(final DeploymentUnit context) {
    }
}
