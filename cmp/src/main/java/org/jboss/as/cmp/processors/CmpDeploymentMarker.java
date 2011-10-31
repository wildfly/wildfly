package org.jboss.as.cmp.processors;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class CmpDeploymentMarker {
    private static final AttachmentKey<Boolean> ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    public static void mark(final DeploymentUnit deployment) {
        deployment.putAttachment(ATTACHMENT_KEY, true);
    }

    public static boolean isCmpDeployment(final DeploymentUnit deploymentUnit) {
        final Boolean val = deploymentUnit.getAttachment(ATTACHMENT_KEY);
        return val != null && val;
    }
}
