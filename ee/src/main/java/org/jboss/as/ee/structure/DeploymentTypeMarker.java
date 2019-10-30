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
package org.jboss.as.ee.structure;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;

/**
 * Helper class for dealing with the {@link Attachments#RESOURCE_ROOT_TYPE} attachment.
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class DeploymentTypeMarker {

    private DeploymentTypeMarker() {
        // forbidden instantiation
    }

    public static boolean isType(final DeploymentType deploymentType, final DeploymentUnit deploymentUnit) {
        return deploymentType == deploymentUnit.getAttachment(Attachments.DEPLOYMENT_TYPE);
    }

    public static boolean isType(final DeploymentType deploymentType, final ResourceRoot resourceRoot) {
        return deploymentType == resourceRoot.getAttachment(Attachments.DEPLOYMENT_TYPE);
    }

    public static void setType(final DeploymentType deploymentType, final DeploymentUnit deploymentUnit) {
        deploymentUnit.putAttachment(Attachments.DEPLOYMENT_TYPE, deploymentType);
        final ResourceRoot resourceRoot = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        if (resourceRoot != null) {
            resourceRoot.putAttachment(Attachments.DEPLOYMENT_TYPE, deploymentType);
        }
    }

}
