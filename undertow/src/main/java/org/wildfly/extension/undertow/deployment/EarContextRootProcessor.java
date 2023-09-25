/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import org.jboss.as.ee.structure.Attachments;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.metadata.ear.spec.ModulesMetaData;
import org.jboss.metadata.ear.spec.WebModuleMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;

import static org.jboss.metadata.ear.spec.ModuleMetaData.ModuleType.Web;

/**
 * Deployment unit processor responsible for detecting web deployments and determining if they have a parent EAR file and
 * if so applying the EAR defined context root to web metadata.
 *
 * @author John Bailey
 */
public class EarContextRootProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if(warMetaData == null) {
            return; // Nothing we can do without WarMetaData
        }
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        if(deploymentRoot == null) {
            return; // We don't have a root to work with
        }

        final DeploymentUnit parent = deploymentUnit.getParent();
        if(parent == null || !DeploymentTypeMarker.isType(DeploymentType.EAR, parent)) {
            return;  // Only care if this war is nested in an EAR
        }

        final EarMetaData earMetaData = parent.getAttachment(Attachments.EAR_METADATA);
        if(earMetaData == null) {
            return; // Nothing to see here
        }

        final ModulesMetaData modulesMetaData = earMetaData.getModules();
        if(modulesMetaData != null) for(ModuleMetaData moduleMetaData : modulesMetaData) {
            if(Web.equals(moduleMetaData.getType()) && moduleMetaData.getFileName().equals(deploymentRoot.getRootName())) {
                String contextRoot = WebModuleMetaData.class.cast(moduleMetaData.getValue()).getContextRoot();

                if(contextRoot == null && (warMetaData.getJBossWebMetaData() == null || warMetaData.getJBossWebMetaData().getContextRoot() == null)) {
                    contextRoot = "/" + parent.getName().substring(0, parent.getName().length() - 4) + "/"
                            + deploymentUnit.getName().substring(0, deploymentUnit.getName().length() - 4);
                }

                if(contextRoot != null) {
                    JBossWebMetaData jBossWebMetaData = warMetaData.getJBossWebMetaData();
                        if(jBossWebMetaData == null) {
                            jBossWebMetaData = new JBossWebMetaData();
                            warMetaData.setJBossWebMetaData(jBossWebMetaData);
                        }
                    jBossWebMetaData.setContextRoot(contextRoot);
                }
                return;
            }
        }
    }
}
