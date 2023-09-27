/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment;

import static org.jboss.as.weld.util.Utils.getRootDeploymentUnit;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Marker for deployments that have CDI annotations present
 */
public final class CdiAnnotationMarker {
    /**
     * Boolean attachment key.
     */
    private static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    /**
     * Default constructor not visible.
     */
    private CdiAnnotationMarker() {
    }

    /**
     * Mark the deployment as a CDI one.
     *
     * @param deployment to be marked
     */
    public static void mark(final DeploymentUnit deployment) {
        if (deployment.getParent() != null) {
            deployment.getParent().putAttachment(ATTACHMENT_KEY, true);
        } else {
            deployment.putAttachment(ATTACHMENT_KEY, true);
        }
    }

    public static boolean cdiAnnotationsPresent(final DeploymentUnit deploymentUnit) {
        Boolean val = getRootDeploymentUnit(deploymentUnit).getAttachment(ATTACHMENT_KEY);
        return val != null && val;
    }
}
