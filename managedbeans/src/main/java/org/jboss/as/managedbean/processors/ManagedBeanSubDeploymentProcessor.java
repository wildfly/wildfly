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

package org.jboss.as.managedbean.processors;

import java.util.List;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import static org.jboss.as.server.deployment.SubDeploymentMarker.isSubDeployment;
import static org.jboss.as.server.deployment.SubDeploymentMarker.markRoot;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.Index;

/**
 * Deployment processor used to determine if a possible sub-deployment contains managed beans.
 *
 * @author John Bailey
 */
public class ManagedBeanSubDeploymentProcessor implements DeploymentUnitProcessor {
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Boolean earDeployment = deploymentUnit.getAttachment(Attachments.EAR_DEPLOYMENT_MARKER);
        if(earDeployment == null || !earDeployment) {
            return;
        }

        final List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        for(ResourceRoot resourceRoot : resourceRoots) {
            if(!isSubDeployment(resourceRoot) && resourceRoot.getRoot().getLowerCaseName().endsWith(".jar")) {
                final Index annotationIndex = resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX);
                if(annotationIndex == null) {
                    continue;
                }
                final List<AnnotationInstance> managedBeanAnnotations = annotationIndex.getAnnotations(ManagedBeanAnnotationProcessor.MANAGED_BEAN_ANNOTATION_NAME);
                if(managedBeanAnnotations != null && !managedBeanAnnotations.isEmpty()) {
                    markRoot(resourceRoot);
                }
            }
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
