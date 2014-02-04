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
package org.jboss.as.appclient.deployment;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VirtualFile;

/**
 * Processor that marks a sub-deployment as an application client based on the parameters passed on the command line
 *
 * @author Stuart Douglas
 */
public class ApplicationClientStructureProcessor implements DeploymentUnitProcessor {

    private final String deployment;

    public ApplicationClientStructureProcessor(final String deployment) {
        this.deployment = deployment;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (deploymentUnit.getName().toLowerCase(Locale.ENGLISH).endsWith(".ear")) {
            final Map<VirtualFile, ResourceRoot> existing = new HashMap<VirtualFile, ResourceRoot>();
            for (final ResourceRoot additional : deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS)) {
                existing.put(additional.getRoot(), additional);
            }

            final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            final VirtualFile appClientRoot = root.getRoot().getChild(deployment);
            if (appClientRoot.exists()) {
                if (existing.containsKey(appClientRoot)) {
                    final ResourceRoot existingRoot = existing.get(appClientRoot);
                    SubDeploymentMarker.mark(existingRoot);
                    ModuleRootMarker.mark(existingRoot);
                } else {
                    final Closeable closable = appClientRoot.isFile() ? mount(appClientRoot, false) : null;
                    final MountHandle mountHandle = new MountHandle(closable);
                    final ResourceRoot childResource = new ResourceRoot(appClientRoot, mountHandle);
                    ModuleRootMarker.mark(childResource);
                    SubDeploymentMarker.mark(childResource);
                    deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, childResource);
                }

            } else {
                throw AppClientLogger.ROOT_LOGGER.cannotFindAppClient(deployment);
            }
        } else if (deploymentUnit.getParent() != null && deploymentUnit.getName().toLowerCase(Locale.ENGLISH).endsWith(".jar")) {
            final ResourceRoot parentRoot = deploymentUnit.getParent().getAttachment(Attachments.DEPLOYMENT_ROOT);
            final VirtualFile appClientRoot = parentRoot.getRoot().getChild(deployment);
            final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
            if (appClientRoot.equals(root.getRoot())) {
                DeploymentTypeMarker.setType(DeploymentType.APPLICATION_CLIENT, deploymentUnit);
            }

        }
    }

    private static Closeable mount(VirtualFile moduleFile, boolean explode) throws DeploymentUnitProcessingException {
        try {
            return explode ? VFS.mountZipExpanded(moduleFile, moduleFile, TempFileProviderService.provider())
                    : VFS.mountZip(moduleFile, moduleFile, TempFileProviderService.provider());
        } catch (IOException e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
