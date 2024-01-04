/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.iiop.openjdk.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * @author Stuart Douglas
 */
public class IIOPDeploymentMarker {

    private static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    public static void mark(final DeploymentUnit deployment) {
        deployment.putAttachment(ATTACHMENT_KEY, true);
    }

    public static boolean isIIOPDeployment(final DeploymentUnit deploymentUnit) {
        final Boolean val = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        return val != null && val;
    }
}
