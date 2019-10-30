/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.javaee.spec.MessageDestinationMetaData;

/**
 * @author Stuart Douglas
 */
public class EarMessageDestinationProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            final EarMetaData metadata = deploymentUnit.getAttachment(Attachments.EAR_METADATA);
            final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

            if (metadata != null) {

                if (metadata.getEarEnvironmentRefsGroup() != null) {
                    if (metadata.getEarEnvironmentRefsGroup().getMessageDestinations() != null) {
                        for (final MessageDestinationMetaData destination : metadata.getEarEnvironmentRefsGroup().getMessageDestinations()) {
                            //TODO: should these be two separate metadata attributes?
                            if (destination.getJndiName() != null) {
                                eeModuleDescription.addMessageDestination(destination.getName(), destination.getJndiName());
                            } else if (destination.getLookupName() != null) {
                                eeModuleDescription.addMessageDestination(destination.getName(), destination.getLookupName());
                            }
                        }
                    }
                }

            }

        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
