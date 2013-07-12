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
package org.jboss.as.ee.weld;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;

/**
 * Marker for top level deployments that contain a beans.xml file
 *
 * @author Stuart Douglas
 *
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
