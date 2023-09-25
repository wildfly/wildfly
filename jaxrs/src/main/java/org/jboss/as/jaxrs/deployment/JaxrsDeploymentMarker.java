/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs.deployment;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Marker for Jakarta RESTful Web Services deployments
 *
 * @author Stuart Douglas
 */
public class JaxrsDeploymentMarker {
    private static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    public static void mark(DeploymentUnit deployment) {
        if (deployment.getParent() != null) {
            deployment.getParent().putAttachment(ATTACHMENT_KEY, true);
        } else {
            deployment.putAttachment(ATTACHMENT_KEY, true);
        }
    }

    //This actually tells whether the deployment unit is potentially part of a Jakarta RESTful Web Services deployment;
    //in practice, we might not be dealing with a Jakarta RESTful Web Services deployment (it depends on which/where
    //Jakarta RESTful Web Services annotations are found in the deployment, especially if it's an EAR one)
    public static boolean isJaxrsDeployment(DeploymentUnit deploymentUnit) {
        DeploymentUnit deployment = deploymentUnit.getParent() == null ? deploymentUnit : deploymentUnit.getParent();
        Boolean val = deployment.getAttachment(ATTACHMENT_KEY);
        return val != null && val;
    }
}
