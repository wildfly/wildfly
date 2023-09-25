/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.wildfly.extension.microprofile.faulttolerance.MicroProfileFaultToleranceLogger;

/**
 * Utility class which marks MP FT deployments.
 *
 * @author Radoslav Husar
 */
public class MicroProfileFaultToleranceMarker {

    private static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    static void mark(DeploymentUnit deployment) {
        deployment.putAttachment(ATTACHMENT_KEY, true);
    }

    /**
     * @return whether the deployment was marked as MP FT deployment
     */
    public static boolean isMarked(DeploymentUnit deploymentUnit) {
        Boolean b = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        return (b != null) && b;
    }

    static void clearMark(DeploymentUnit deployment) {
        deployment.removeAttachment(ATTACHMENT_KEY);
    }

    public static boolean hasMicroProfileFaultToleranceAnnotations(DeploymentUnit deploymentUnit) {
        CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        for (MicroProfileFaultToleranceAnnotation ftAnnotation : MicroProfileFaultToleranceAnnotation.values()) {
            if (!index.getAnnotations(ftAnnotation.getDotName()).isEmpty()) {
                MicroProfileFaultToleranceLogger.ROOT_LOGGER.debugf("Deployment '%s' is a MicroProfile Fault Tolerance deployment – @%s annotation found.", deploymentUnit.getName(), ftAnnotation.getDotName());
                return true;
            }
        }

        MicroProfileFaultToleranceLogger.ROOT_LOGGER.debugf("No MicroProfile Fault Tolerance annotations found in deployment '%s'.", deploymentUnit.getName());

        return false;
    }

    private MicroProfileFaultToleranceMarker() {
        // Utility class.
    }
}
