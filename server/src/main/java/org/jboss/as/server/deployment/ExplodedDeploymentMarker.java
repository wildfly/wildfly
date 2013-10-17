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
 * Marker that can be used to determine if a deployment is an exploded deployment or not
 *
 * @author Stuart Douglas
 */
public class ExplodedDeploymentMarker {

    private static final AttachmentKey<Boolean> EXPLODED_DEPLOYMENT = AttachmentKey.create(Boolean.class);

    public static void markAsExplodedDeployment(final DeploymentUnit deploymentUnit) {
        deploymentUnit.putAttachment(EXPLODED_DEPLOYMENT, true);
    }

    public static boolean isExplodedDeployment(final DeploymentUnit deploymentUnit) {
        Boolean exploded = deploymentUnit.getAttachment(EXPLODED_DEPLOYMENT);
        return exploded != null ? exploded : false;
    }
}
