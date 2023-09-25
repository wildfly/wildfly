/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class that is responsible for resolving name conflicts.
 *
 * //TODO: this must be able to deal with the case of module names being changed via deployment descriptor
 *
 * @author Stuart Douglas
 */
public final class EEModuleNameProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<DeploymentUnit> subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
        final Set<String> moduleNames = new HashSet<String>();
        final Set<String> moduleConflicts = new HashSet<String>();
        //look for modules with the same name
        //
        for(DeploymentUnit deployment : subDeployments) {
            final EEModuleDescription module = deployment.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if(module != null) {
                if(moduleNames.contains(module.getModuleName())) {
                    moduleConflicts.add(module.getModuleName());
                }
                moduleNames.add(module.getModuleName());
            }
        }
        for(DeploymentUnit deployment : subDeployments) {
            final EEModuleDescription module = deployment.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if (module != null
                    && moduleConflicts.contains(module.getModuleName())) {
                module.setModuleName(deployment.getName());
            }
        }

    }
}
