/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld._private;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Marker for top level deployments that contain a beans.xml file
 *
 * @author Stuart Douglas
 */
public class WeldDeploymentMarker {

    private static final AttachmentKey<Boolean> MARKER = AttachmentKey.create(Boolean.class);

    /**
     * Mark this deployment and the top level deployment as being a weld deployment.
     *
     */
    public static void mark(DeploymentUnit unit) {
        unit.putAttachment(MARKER, Boolean.TRUE);
        if (unit.getParent() != null) {
            mark(unit.getParent());
        }
    }

    /**
     * returns true if the {@link DeploymentUnit} is part of a weld deployment
     */
    public static boolean isPartOfWeldDeployment(DeploymentUnit unit) {
        if (unit.getParent() == null) {
            return unit.getAttachment(MARKER) != null;
        } else {
            return unit.getParent().getAttachment(MARKER) != null;
        }
    }

    /**
     * returns true if the {@link DeploymentUnit} has a beans.xml in any of it's resource roots,
     * or is a top level deployment that contains sub-deployments that are weld deployments.
     */
    public static boolean isWeldDeployment(DeploymentUnit unit) {
        return unit.getAttachment(MARKER) != null;
    }

    private WeldDeploymentMarker() {

    }
}

