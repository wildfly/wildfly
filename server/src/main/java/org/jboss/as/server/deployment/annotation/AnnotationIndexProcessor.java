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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.moduleservice.ModuleIndexBuilder;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

/**
 * Deployment unit processor responsible for creating and attaching an annotation index for a resource root
 *
 * @author John E. Bailey
 * @author Stuart Douglas
 */
public class AnnotationIndexProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(AnnotationIndexProcessor.class);

    /**
     * Process this deployment for annotations.  This will use an annotation indexer to create an index of all annotations
     * found in this deployment and attach it to the deployment unit context.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final List<ResourceRoot> allResourceRoots = new ArrayList<ResourceRoot>();
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        if (resourceRoots != null) {
            allResourceRoots.addAll(resourceRoots);
        }

        allResourceRoots.add(deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT));
        for (ResourceRoot resourceRoot : allResourceRoots) {
            if (resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX) != null) {
                continue;
            }

            VirtualFile indexFile = resourceRoot.getRoot().getChild(ModuleIndexBuilder.INDEX_LOCATION);
            if (indexFile.exists()) {
                try {
                    IndexReader reader = new IndexReader(indexFile.openStream());
                    resourceRoot.putAttachment(Attachments.ANNOTATION_INDEX, reader.read());
                    logger.tracef("Found and read index at: %s", indexFile);
                    continue;
                } catch (Exception e) {
                    logger.debugf("Could not read provided index: %s", indexFile, e);
                }
            }

            // if this flag is present and set to false then do not index the resource
            Boolean shouldIndexResource = resourceRoot.getAttachment(Attachments.INDEX_RESOURCE_ROOT);
            if (shouldIndexResource != null && !shouldIndexResource) {
                continue;
            }

            final List<String> indexIgnorePathList = resourceRoot.getAttachment(Attachments.INDEX_IGNORE_PATHS);
            final Set<String> indexIgnorePaths;
            if (indexIgnorePathList != null && !indexIgnorePathList.isEmpty()) {
                indexIgnorePaths = new HashSet<String>(indexIgnorePathList);
            } else {
                indexIgnorePaths = null;
            }

            final VirtualFile virtualFile = resourceRoot.getRoot();
            final Indexer indexer = new Indexer();
            try {
                final VisitorAttributes visitorAttributes = new VisitorAttributes();
                visitorAttributes.setLeavesOnly(true);
                visitorAttributes.setRecurseFilter(new VirtualFileFilter() {
                    public boolean accepts(VirtualFile file) {
                        return indexIgnorePaths == null || !indexIgnorePaths.contains(file.getPathNameRelativeTo(virtualFile));
                    }
                });

                final List<VirtualFile> classChildren = virtualFile.getChildren(new SuffixMatchFilter(".class", visitorAttributes));
                for (VirtualFile classFile : classChildren) {
                    InputStream inputStream = null;
                    try {
                        inputStream = classFile.openStream();
                        indexer.index(inputStream);
                    } catch (Exception e) {
                        logger.warn("Could not index class " + classFile.getPathNameRelativeTo(virtualFile) + " in archive '" + virtualFile + "'", e);
                    } finally {
                        VFSUtils.safeClose(inputStream);
                    }
                }
                final Index index = indexer.complete();
                resourceRoot.putAttachment(Attachments.ANNOTATION_INDEX, index);
                logger.tracef("Generated index for archive %s", virtualFile);
            } catch (Throwable t) {
                throw new DeploymentUnitProcessingException("Failed to index deployment root for annotations", t);
            }
        }


    }

    public void undeploy(final DeploymentUnit context) {
    }
}
