/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
