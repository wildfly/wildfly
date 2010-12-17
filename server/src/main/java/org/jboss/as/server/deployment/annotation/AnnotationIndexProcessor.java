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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

import java.io.InputStream;
import java.util.List;

/**
 * Deployment unit processor responsible for creating and attaching an annotation index for a deployment unit.
 *
 * @author John E. Bailey
 */
public class AnnotationIndexProcessor implements DeploymentUnitProcessor {

    /**
     * Process this deployment for annotations.  This will use an annotation indexer to create an index of all annotations
     * found in this deployment and attach it to the deployment unit context.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(deploymentUnit.getAttachment(Attachments.ANNOTATION_INDEX) != null)
            return;

        final ResourceRoot resourceRoot = phaseContext.getDeploymentUnit().getAttachment(Attachments.DEPLOYMENT_ROOT);
        final VirtualFile virtualFile = resourceRoot.getRoot();
        final Indexer indexer = new Indexer();
        try {
            final List<VirtualFile> classChildren = virtualFile.getChildren(new SuffixMatchFilter(".class", VisitorAttributes.RECURSE_LEAVES_ONLY));
            for(VirtualFile classFile : classChildren) {
                InputStream inputStream = null;
                try {
                    inputStream = classFile.openStream();
                    indexer.index(inputStream);
                } finally {
                    VFSUtils.safeClose(inputStream);
                }
            }
            final Index index = indexer.complete();
            deploymentUnit.putAttachment(Attachments.ANNOTATION_INDEX, index);
        } catch(Throwable t) {
            throw new DeploymentUnitProcessingException("Failed to index deployment root for annotations", t);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
