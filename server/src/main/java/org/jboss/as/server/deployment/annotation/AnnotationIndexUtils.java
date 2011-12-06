/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.deployment.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.Index;

/**
 * Utility class that allows easy access to all annotation Indexes in a deployment.
 *
 * Note that this will not resolve indexes from Class-Path entries
 *
 * @author Stuart Douglas
 * @author Ales Justin
 */
public class AnnotationIndexUtils {

    public static Map<ResourceRoot, Index> getAnnotationIndexes(final DeploymentUnit deploymentUnit) {
        final List<ResourceRoot> allResourceRoots = new ArrayList<ResourceRoot>();
        final List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        if (resourceRoots != null) {
            allResourceRoots.addAll(resourceRoots);
        }
        allResourceRoots.add(deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT));
        Map<ResourceRoot, Index> indexes = new HashMap<ResourceRoot, Index>();
        for (ResourceRoot resourceRoot : allResourceRoots) {
            if (resourceRoot == null)
                continue;

            Index index = resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX);
            if (index != null) {
                indexes.put(resourceRoot, index);
            }
        }
        return indexes;
    }

}
