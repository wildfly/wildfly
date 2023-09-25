/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant.deployment;

import io.narayana.lra.client.internal.proxy.nonjaxrs.LRAParticipantResource;
import io.narayana.lra.filter.ClientLRARequestFilter;
import io.narayana.lra.filter.ClientLRAResponseFilter;
import io.narayana.lra.filter.ServerLRAFilter;
import org.jboss.as.jaxrs.deployment.JaxrsAttachments;
import org.jboss.as.jaxrs.deployment.ResteasyDeploymentData;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

public class LRAParticipantJaxrsDeploymentUnitProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (LRAAnnotationsUtil.isNotLRADeployment(deploymentUnit)) {
            return;
        }

        final ResteasyDeploymentData resteasyDeploymentData = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);

        if (resteasyDeploymentData != null) {
            resteasyDeploymentData.getScannedResourceClasses().add(LRAParticipantResource.class.getName());
            resteasyDeploymentData.getScannedProviderClasses().add(ServerLRAFilter.class.getName());
            resteasyDeploymentData.getScannedProviderClasses().add(ClientLRARequestFilter.class.getName());
            resteasyDeploymentData.getScannedProviderClasses().add(ClientLRAResponseFilter.class.getName());
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}