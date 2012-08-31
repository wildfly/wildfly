/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.server.deployment;


/**
 * JPA Deployment marker
 *
 * @author Scott Marlow (copied from WeldDeploymentMarker)
 */
public class JPADeploymentMarker {

    private static final AttachmentKey<Boolean> MARKER = AttachmentKey.create(Boolean.class);

    /**
     * Mark the top level deployment as being a JPA deployment. If the deployment is not a top level deployment the parent is
     * marked instead
     */
    public static void mark(DeploymentUnit unit) {
        unit = DeploymentUtils.getTopDeploymentUnit(unit);
        unit.putAttachment(MARKER, Boolean.TRUE);
    }

    /**
     * return true if the {@link DeploymentUnit} is part of a JPA deployment
     */
    public static boolean isJPADeployment(DeploymentUnit unit) {
        unit = DeploymentUtils.getTopDeploymentUnit(unit);
        return unit.getAttachment(MARKER) != null;
    }

    private JPADeploymentMarker() {

    }
}
