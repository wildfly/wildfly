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

package org.jboss.as.web.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.web.deployment.helpers.DeploymentStructure;
import org.jboss.as.web.deployment.helpers.DeploymentStructure.ClassPathEntry;
import org.jboss.vfs.VirtualFileFilter;
import org.jboss.vfs.VisitorAttributes;
import org.jboss.vfs.util.SuffixMatchFilter;

import static org.jboss.as.web.deployment.WarDeploymentMarker.isWarDeployment;

/**
 * War {@code ModuleConfig} processor.
 *
 * @author Emanuel Muckenhuber
 */
public class WarModuleConfigProcessor implements DeploymentUnitProcessor {

    public static final String WEB_INF_LIB = "WEB-INF/lib";
    public static final String WEB_INF_CLASSES = "WEB-INF/classes";

    public static final VirtualFileFilter DEFAULT_WEB_INF_LIB_FILTER = new SuffixMatchFilter(".jar", VisitorAttributes.DEFAULT);

    private static final ResourceRoot[] NO_ROOTS = new ResourceRoot[0];

    /**
     * Create the {@code ModuleConfig} for a .war deployment
     *
     * {@inheritDoc}
     */
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if(!isWarDeployment(deploymentUnit)) {
            return; // Skip non web deployments
        }
        final ResourceRoot[] resourceRoots = createResourceRoots(deploymentUnit.getAttachment(DeploymentStructure.ATTACHMENT_KEY));
        for (ResourceRoot root : resourceRoots) {
            deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, root);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    private ResourceRoot[] createResourceRoots(final DeploymentStructure structure) {
        if(structure == null) {
            return NO_ROOTS;
        }
        final ClassPathEntry[] entries = structure.getEntries();
        if(entries == null || entries.length == 0) {
            return NO_ROOTS;
        }
        final int length = entries.length;
        final ResourceRoot[] roots = new ResourceRoot[length];
        for(int i = 0; i < length; i++) {
            final ClassPathEntry entry = entries[i];
            roots[i] = new ResourceRoot(entry.getName(), entry.getRoot(), entry.getMountHandle(), false);
        }
        return roots;
    }
}
