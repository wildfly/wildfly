/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
}
