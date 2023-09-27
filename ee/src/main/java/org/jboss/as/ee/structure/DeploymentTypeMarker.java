/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;

/**
 * Helper class for dealing with the {@link Attachments#RESOURCE_ROOT_TYPE} attachment.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class DeploymentTypeMarker {

    private DeploymentTypeMarker() {
        // forbidden instantiation
    }

    public static boolean isType(final DeploymentType deploymentType, final DeploymentUnit deploymentUnit) {
        return deploymentType == deploymentUnit.getAttachment(Attachments.DEPLOYMENT_TYPE);
    }

    public static boolean isType(final DeploymentType deploymentType, final ResourceRoot resourceRoot) {
        return deploymentType == resourceRoot.getAttachment(Attachments.DEPLOYMENT_TYPE);
    }

    public static void setType(final DeploymentType deploymentType, final DeploymentUnit deploymentUnit) {
        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_TYPE, deploymentType);
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        if (resourceRoot != null) {
            resourceRoot.putAttachment(Attachments.DEPLOYMENT_TYPE, deploymentType);
        }
    }

}
