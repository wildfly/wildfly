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
package org.jboss.as.server.deployment;

/**
 * Marker for sub deployments that are not globally accessible, such as war's. If a sub deployment does not have this marker set
 * then all other deployments will be given access to it, through a module dependency on sub deployments module.
 *
 * @author Stuart Douglas
 *
 */
public class PrivateSubDeploymentMarker {
    private static final AttachmentKey<Boolean> PRIVATE_SUB_DEPLOYMENT = AttachmentKey.create(Boolean.class);

    public static void mark(DeploymentUnit attachable) {
        attachable.putAttachment(PRIVATE_SUB_DEPLOYMENT, true);
    }

    public static boolean isPrivate(DeploymentUnit resourceRoot) {
        Boolean res = resourceRoot.getAttachment(PRIVATE_SUB_DEPLOYMENT);
        return res != null && res;
    }

    private PrivateSubDeploymentMarker() {

    }
}
