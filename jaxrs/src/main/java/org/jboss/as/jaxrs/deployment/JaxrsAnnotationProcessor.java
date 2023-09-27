/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs.deployment;

import org.jboss.as.jaxrs.JaxrsAnnotations;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;

import static org.jboss.as.ee.weld.InjectionTargetDefiningAnnotations.INJECTION_TARGET_DEFINING_ANNOTATIONS;

/**
 * Looks for jaxrs annotations in war deployments
 *
 * @author Stuart Douglas
 */
public class JaxrsAnnotationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (deploymentUnit.getParent() == null) {
            //register resource, provider and application as Jakarta Contexts and Dependency Injection annotation defining types
            deploymentUnit.addToAttachmentList(INJECTION_TARGET_DEFINING_ANNOTATIONS, JaxrsAnnotations.PROVIDER.getDotName());
            deploymentUnit.addToAttachmentList(INJECTION_TARGET_DEFINING_ANNOTATIONS, JaxrsAnnotations.PATH.getDotName());
        }

        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (final JaxrsAnnotations annotation : JaxrsAnnotations.values()) {
            if (!index.getAnnotations(annotation.getDotName()).isEmpty()) {
                JaxrsDeploymentMarker.mark(deploymentUnit);
                return;
            }
        }

    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        if (deploymentUnit.getParent() == null) {
            deploymentUnit.getAttachmentList(INJECTION_TARGET_DEFINING_ANNOTATIONS).remove(JaxrsAnnotations.PROVIDER.getDotName());
            deploymentUnit.getAttachmentList(INJECTION_TARGET_DEFINING_ANNOTATIONS).remove(JaxrsAnnotations.PATH.getDotName());
        }
    }

}
