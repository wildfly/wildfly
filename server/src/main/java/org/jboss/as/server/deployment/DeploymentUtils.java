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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.dmr.ModelNode;

/**
 * Helper class with static methods related to deployment
 *
 * TODO: this should probably be somewhere else
 *
 * @author Stuart Douglas
 * @author Ales Justin
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

    /**
     * Get top deployment unit.
     *
     * @param unit the current deployment unit
     * @return top deployment unit
     */
    public static DeploymentUnit getTopDeploymentUnit(DeploymentUnit unit) {
        if (unit == null)
            throw new IllegalArgumentException("Null initial unit");

        DeploymentUnit parent = unit.getParent();
        while (parent != null) {
            unit = parent;
            parent = unit.getParent();
        }
        return unit;
    }

    public static List<byte[]> getDeploymentHash(Resource deployment) {
        return getDeploymentHash(deployment.getModel());
    }

    public static List<byte[]> getDeploymentHash(ModelNode deployment){
        List<byte[]> hashes = new ArrayList<byte[]>();
        if (deployment.hasDefined(CONTENT)) {
            for (ModelNode contentElement : deployment.get(CONTENT).asList()) {
                if (contentElement.hasDefined(HASH)) {
                    final byte[] hash = contentElement.get(HASH).asBytes();
                    hashes.add(hash);
                }
            }
        }
        return hashes;
    }

    private DeploymentUtils() {
    }
}
