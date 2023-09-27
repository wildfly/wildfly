/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.structure;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.server.deployment.DeploymentCompleteServiceProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.msc.service.ServiceName;

/**
 * Sets up dependencies for the next phase if initialize in order is set.
 *
 * @author Stuart Douglas
 */
public class InitializeInOrderProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final DeploymentUnit parent = deploymentUnit.getParent();
        if (parent == null) {
            return;
        }
        final EarMetaData earConfig = parent.getAttachment(Attachments.EAR_METADATA);
        if (earConfig != null) {
            final boolean inOrder = earConfig.getInitializeInOrder();
            if (inOrder && earConfig.getModules().size() > 1) {

                final Map<String, DeploymentUnit> deploymentUnitMap = new HashMap<String, DeploymentUnit>();
                for (final DeploymentUnit subDeployment : parent.getAttachment(org.jboss.as.server.deployment.Attachments.SUB_DEPLOYMENTS)) {

                    final ResourceRoot deploymentRoot = subDeployment.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
                    final ModuleMetaData moduleMetaData = deploymentRoot.getAttachment(Attachments.MODULE_META_DATA);
                    if (moduleMetaData != null) {
                        deploymentUnitMap.put(moduleMetaData.getFileName(), subDeployment);
                    }
                }


                final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
                final ModuleMetaData thisModulesMetadata = deploymentRoot.getAttachment(Attachments.MODULE_META_DATA);
                if (thisModulesMetadata != null && thisModulesMetadata.getType() != ModuleMetaData.ModuleType.Client) {
                    ModuleMetaData previous = null;
                    boolean found = false;
                    for (ModuleMetaData module : earConfig.getModules()) {
                        if (module.getType() != ModuleMetaData.ModuleType.Client) {
                            if (module.getFileName().equals(thisModulesMetadata.getFileName())) {
                                found = true;
                                break;
                            }
                            previous = module;
                        }
                    }
                    if (previous != null && found) {
                        //now we know the previous module we can setup the service dependencies
                        //we setup one on the deployment service, and also one on every component
                        final ServiceName serviceName = Services.deploymentUnitName(parent.getName(), previous.getFileName());
                        phaseContext.addToAttachmentList(org.jboss.as.server.deployment.Attachments.NEXT_PHASE_DEPS, serviceName.append(Phase.INSTALL.name()));
                        final DeploymentUnit prevDeployment = deploymentUnitMap.get(previous.getFileName());

                        phaseContext.addToAttachmentList(org.jboss.as.server.deployment.Attachments.NEXT_PHASE_DEPS, DeploymentCompleteServiceProcessor.serviceName(prevDeployment.getServiceName()));
                    }
                }
            }
        }
    }
}
