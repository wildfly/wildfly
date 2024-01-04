/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * {@link org.jboss.as.server.deployment.DeploymentUnitProcessor} responsible for determining if property replacement should be
 * done on Jakarta Enterprise Beans annotations.
 */
public class AnnotationPropertyReplacementProcessor implements DeploymentUnitProcessor {

    private volatile boolean annotationPropertyReplacement = false;
    private final AttachmentKey<Boolean> attachmentKey;

    public AnnotationPropertyReplacementProcessor(final AttachmentKey<Boolean> attachmentKey) {
        this.attachmentKey = attachmentKey;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        deploymentUnit.putAttachment(attachmentKey, annotationPropertyReplacement);

    }

    public void setDescriptorPropertyReplacement(boolean annotationPropertyReplacement) {
        this.annotationPropertyReplacement = annotationPropertyReplacement;
    }

}
