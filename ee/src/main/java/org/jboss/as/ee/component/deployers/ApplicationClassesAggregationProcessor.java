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
package org.jboss.as.ee.component.deployers;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * Processor that aggregates all module descriptions visible to the deployment in an EEApplicationClasses structure.
 *
 * @author Stuart Douglas
 */
public class ApplicationClassesAggregationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<EEModuleDescription> descriptions = new ArrayList<EEModuleDescription>();
        for (final DeploymentUnit visibleDeployment : deploymentUnit.getAttachmentList(org.jboss.as.server.deployment.Attachments.ACCESSIBLE_SUB_DEPLOYMENTS)) {
            final EEModuleDescription description = visibleDeployment.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
            if (description != null) {
                descriptions.add(description);
            }
        }
        final EEApplicationClasses classes = new EEApplicationClasses(descriptions);
        deploymentUnit.putAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION, classes);
    }


    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
