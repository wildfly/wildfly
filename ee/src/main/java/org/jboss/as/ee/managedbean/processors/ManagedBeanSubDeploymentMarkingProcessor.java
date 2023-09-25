/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.managedbean.processors;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;

import jakarta.annotation.ManagedBean;
import java.util.List;

/**
 * Processor that only runs for ear deployments where no application.xml is provided. It examines jars in the ear to determine
 * sub-deployments containing {@link ManagedBean managed beans}.
 *
 * @author Jaikiran Pai
 */
public class ManagedBeanSubDeploymentMarkingProcessor implements DeploymentUnitProcessor {


    private static final DotName MANAGED_BEAN = DotName.createSimple(ManagedBean.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        List<ResourceRoot> potentialSubDeployments = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : potentialSubDeployments) {
            if (ModuleRootMarker.isModuleRoot(resourceRoot)) {
                // module roots cannot be managed bean jars
                continue;
            }
            final Index index = resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX);
            if (index != null
                    && !index.getAnnotations(MANAGED_BEAN).isEmpty()) {
                // this is a managed bean deployment
                SubDeploymentMarker.mark(resourceRoot);
                ModuleRootMarker.mark(resourceRoot);
            }
        }
    }
}
