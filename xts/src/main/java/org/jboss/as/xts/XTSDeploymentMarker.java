/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * @author paul.robinson@redhat.com, 2012-02-13
 */
public class XTSDeploymentMarker {

    private static final AttachmentKey<XTSDeploymentMarker> MARKER = AttachmentKey.create(XTSDeploymentMarker.class);

    private XTSDeploymentMarker() {
    }

    public static void mark(DeploymentUnit unit) {
        unit.putAttachment(MARKER, new XTSDeploymentMarker());
    }

    public static boolean isXTSAnnotationDeployment(DeploymentUnit unit) {
        return unit.hasAttachment(MARKER);
    }

}
