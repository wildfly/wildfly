/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Utility class for marking a deployment as an OIDC deployment.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class OidcDeploymentMarker {

    private static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    public static void mark(DeploymentUnit deployment) {
        toRoot(deployment).putAttachment(ATTACHMENT_KEY, true);
    }

    public static boolean isOidcDeployment(DeploymentUnit deployment) {
        Boolean val = toRoot(deployment).getAttachment(ATTACHMENT_KEY);
        return val != null && val;
    }

    private static DeploymentUnit toRoot(final DeploymentUnit deployment) {
        DeploymentUnit result = deployment;
        DeploymentUnit parent = result.getParent();
        while (parent != null) {
            result = parent;
            parent = result.getParent();
        }

        return result;
    }

}