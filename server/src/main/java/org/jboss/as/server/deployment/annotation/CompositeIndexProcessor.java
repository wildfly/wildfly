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
import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.Index;

/**
 * Processor responsible for creating and attaching a {@link CompositeIndex} for a deployment.
 *
 * @author John Bailey
 */
public class CompositeIndexProcessor implements DeploymentUnitProcessor {
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final Boolean computeCompositeIndex = deploymentUnit.getAttachment(Attachments.COMPUTE_COMPOSITE_ANNOTATION_INDEX);
        if(computeCompositeIndex != null && !computeCompositeIndex) {
            return;
        }

        final List<ResourceRoot> allResourceRoots = new ArrayList<ResourceRoot>();
        final Boolean processChildren = deploymentUnit.getAttachment(Attachments.PROCESS_CHILD_ANNOTATION_INDEX);
        if(processChildren == null || processChildren) {
            final List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
            if (resourceRoots != null) {
                for (ResourceRoot resourceRoot : resourceRoots) {
                    // do not add child sub deployments to the composite index
                    if (!SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                        allResourceRoots.add(resourceRoot);
                    }
                }
            }
        }
        allResourceRoots.add(deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT));
        List<Index> indexes = new ArrayList<Index>(allResourceRoots.size());
        for (ResourceRoot resourceRoot : allResourceRoots) {
            Index index = resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX);
            if (index != null) {
                indexes.add(index);
            }
        }
        deploymentUnit.putAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX, new CompositeIndex(indexes));
    }

    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
    }
}
