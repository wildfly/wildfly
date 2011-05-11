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

import org.jboss.as.server.deployment.module.ResourceRoot;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class with static methods related to deployment
 *
 * TODO: this should probably be somewhere else
 *
 * @author Stuart Douglas
 *
 */
public final class DeploymentUtils {

    /**
     * Get all resource roots for a {@link DeploymentUnit}
     *
     * @param deploymentUnit The deployment unit
     * @return The deployment root and any additional resource roots
     */
    public static List<ResourceRoot> allResourceRoots(DeploymentUnit deploymentUnit) {
        List<ResourceRoot> roots = new ArrayList<ResourceRoot>();
        // not all deployment units have a deployment root
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        if (deploymentRoot != null)
            roots.add(deploymentRoot);
        AttachmentList<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        if (resourceRoots != null) {
            roots.addAll(resourceRoots);
        }
        return roots;
    }

    private DeploymentUtils() {
    }
}
