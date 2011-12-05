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

package org.jboss.as.ee.structure;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.MountHandle;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.module.TempFileProviderService;
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

import static org.jboss.as.ee.EeLogger.SERVER_DEPLOYMENT_LOGGER;

/**
 * A processor which adds class path entries for an ears lib directory. Any jars referenced by these class-path entries are
 * merged into the ear's module.
 *
 * @author Stuart Douglas
 */
public final class EarLibManifestClassPathProcessor implements DeploymentUnitProcessor {

    private static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * {@inheritDoc}
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);

        if (!DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }

        final DeploymentUnit parent = deploymentUnit.getParent();
        final DeploymentUnit topLevelDeployment = parent == null ? deploymentUnit : parent;
        final VirtualFile toplevelRoot = topLevelDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final ExternalModuleService externalModuleService = topLevelDeployment.getAttachment(Attachments.EXTERNAL_MODULE_SERVICE);

        final Map<VirtualFile, ResourceRoot> files = new HashMap<VirtualFile, ResourceRoot>();
        for (ResourceRoot resourceRoot : resourceRoots) {
            files.put(resourceRoot.getRoot(), resourceRoot);
        }
        final Deque<ResourceRoot> libResourceRoots = new ArrayDeque<ResourceRoot>();
        // scan /lib entries for class-path items
        for (ResourceRoot resourceRoot : resourceRoots) {
            if (ModuleRootMarker.isModuleRoot(resourceRoot) && !SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                libResourceRoots.add(resourceRoot);
            }
        }
        while (!libResourceRoots.isEmpty()) {
            final ResourceRoot resourceRoot = libResourceRoots.pop();
            final String[] items = getClassPathEntries(resourceRoot);
            for (String item : items) {
                final VirtualFile classPathFile = resourceRoot.getRoot().getParent().getChild(item);
                if (!classPathFile.exists()) {
                    SERVER_DEPLOYMENT_LOGGER.classPathEntryNotFound(item, resourceRoot.getRoot());
                } else if (isInside(classPathFile, toplevelRoot)) {
                    if (!files.containsKey(classPathFile)) {
                        final ResourceRoot root = createResourceRoot(deploymentUnit, classPathFile);
                        ModuleRootMarker.mark(root);
                        libResourceRoots.add(root);
                    } else {
                        final ResourceRoot target = files.get(classPathFile);
                        if (SubDeploymentMarker.isSubDeployment(target)) {
                            // for now we do not allow ear Class-Path references to subdeployments
                            SERVER_DEPLOYMENT_LOGGER.classPathEntryASubDeployment(resourceRoot.getRoot());
                        } else if (!ModuleRootMarker.isModuleRoot(target)) {
                            // otherwise just add it to the lib dir
                            ModuleRootMarker.mark(target);
                            libResourceRoots.push(target);
                            SERVER_DEPLOYMENT_LOGGER.debugf("Resource %s added to logical lib directory due to Class-Path entry in %s",
                                    classPathFile, target.getRoot());
                        }
                        // otherwise it is already part of lib, so we leave it alone for now
                    }
                } else if (item.startsWith("/")) {
                    ModuleIdentifier moduleIdentifier = externalModuleService.addExternalModule(item);
                    deploymentUnit.addToAttachmentList(Attachments.CLASS_PATH_ENTRIES, moduleIdentifier);
                    SERVER_DEPLOYMENT_LOGGER.debugf("Resource %s added as external jar %s", classPathFile, resourceRoot.getRoot());
                } else {
                    SERVER_DEPLOYMENT_LOGGER.classPathEntryNotAJar(item, resourceRoot.getRoot());
                }
            }
        }
    }

    private static boolean isInside(VirtualFile classPathFile, VirtualFile toplevelRoot) {
        VirtualFile[] parentPaths = classPathFile.getParentFiles();
        for (VirtualFile path : parentPaths) {
            if (path == toplevelRoot) {
                // inside the deployment
                return true;
            }
        }
        return false;
    }

    private static String[] getClassPathEntries(final ResourceRoot resourceRoot) {

        final Manifest manifest;
        try {
            manifest = VFSUtils.getManifest(resourceRoot.getRoot());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (manifest == null) {
            // no class path to process!
            return EMPTY_STRING_ARRAY;
        }
        final String classPathString = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
        if (classPathString == null) {
            // no entry
            return EMPTY_STRING_ARRAY;
        }
        return classPathString.split("\\s+");
    }


    /**
     * Creates a {@link ResourceRoot} for the passed {@link VirtualFile file} and adds it to the list of {@link ResourceRoot}s
     * in the {@link DeploymentUnit deploymentUnit}
     *
     * @param deploymentUnit The deployment unit
     * @param file           The file for which the resource root will be created
     * @return Returns the created {@link ResourceRoot}
     * @throws java.io.IOException
     */
    private ResourceRoot createResourceRoot(final DeploymentUnit deploymentUnit, final VirtualFile file) {
        try {
            final Closeable closable = file.isFile() ? VFS.mountZip(file, file, TempFileProviderService.provider()) : null;
            final MountHandle mountHandle = new MountHandle(closable);
            final ResourceRoot resourceRoot = new ResourceRoot(file, mountHandle);
            deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, resourceRoot);
            return resourceRoot;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * {@inheritDoc}
     */
    public void undeploy(final DeploymentUnit context) {
    }
}
