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

package org.jboss.as.server.deployment.module;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.as.server.deployment.Attachable;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

/**
 * A processor which adds class path entries for each manifest entry.
 * <p/>
 * This processor examines all class path entries found.
 * <ul>
 * <li>
 * If the Class-Path entry points to a jar in ear/lib then it is ignored.
 * </li>
 * <li>
 * If the Class-Path entry is external to the deployment then it is handled by the external jar service.</li>
 * <li>
 * If the entry refers to a sibling deployment then a dependency is added on that deployment. If this deployment is
 * not present then this deployment will block until it is.</li>
 * <li>
 * If the Class-Path entry points to a jar inside the ear that is not a deployment and not a /lib jar then a reference is added
 * to this jars {@link AdditionalModuleSpecification}</li>
 * </ul>
 *
 * @author Stuart Douglas
 */
public final class ManifestClassPathProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger("org.jboss.as.server.deployment");

    private static final String[] EMPTY_STRING_ARRAY = {};

    /**
     * We only allow a single deployment at a time to be run through the class path processor.
     * <p/>
     * This is because if multiple sibling deployments reference the same item we need to make sure that they end up
     * with the same external module, and do not both create an external module with the same name.
     */
    public synchronized void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        //if this has already been handled by the ear class path processor
        if (deploymentUnit.getAttachment(Attachments.CLASS_PATH_ENTRIES) != null) {
            return;
        }

        final ArrayDeque<ResourceRoot> resourceRoots = new ArrayDeque<ResourceRoot>(DeploymentUtils.allResourceRoots(deploymentUnit));

        final DeploymentUnit parent = deploymentUnit.getParent();
        final DeploymentUnit topLevelDeployment = parent == null ? deploymentUnit : parent;
        final VirtualFile topLevelRoot = topLevelDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final ExternalModuleService externalModuleService = topLevelDeployment.getAttachment(Attachments.EXTERNAL_MODULE_SERVICE);
        final List<ResourceRoot> topLevelResourceRoots = topLevelDeployment.getAttachment(Attachments.RESOURCE_ROOTS);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        final Map<VirtualFile, ResourceRoot> subDeployments = new HashMap<VirtualFile, ResourceRoot>();
        for (ResourceRoot root : DeploymentUtils.allResourceRoots(topLevelDeployment)) {
            if (SubDeploymentMarker.isSubDeployment(root)) {
                subDeployments.put(root.getRoot(), root);
            }
        }

        // build a map of the additional module locations
        final Map<VirtualFile, AdditionalModuleSpecification> additionalModules = new HashMap<VirtualFile, AdditionalModuleSpecification>();
        for (AdditionalModuleSpecification module : topLevelDeployment.getAttachmentList(Attachments.ADDITIONAL_MODULES)) {
            for (ResourceRoot additionalModuleResourceRoot : module.getResourceRoots()) {
                additionalModules.put(additionalModuleResourceRoot.getRoot(), module);
            }
        }
        // build a set of ear/lib jars. references to these classes can be ignored as they are already on the class-path
        final Set<VirtualFile> earLibJars = new HashSet<VirtualFile>();
        if (deploymentUnit.getParent() != null && topLevelResourceRoots != null) {
            for (ResourceRoot resourceRoot : topLevelResourceRoots) {
                if (ModuleRootMarker.isModuleRoot(resourceRoot) && !SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                    earLibJars.add(resourceRoot.getRoot());
                }
            }
        }

        while (!resourceRoots.isEmpty()) {
            final ResourceRoot resourceRoot = resourceRoots.pop();
            if (SubDeploymentMarker.isSubDeployment(resourceRoot) && resourceRoot != deploymentRoot) {
                continue;
            }
            // if this resource root represents an additional module then we need
            // to add the class path entry to the additional module
            final Attachable target;
            if (additionalModules.containsKey(resourceRoot.getRoot())) {
                target = additionalModules.get(resourceRoot.getRoot());
            } else {
                target = deploymentUnit;
            }
            final String[] items = getClassPathEntries(resourceRoot);
            for (String item : items) {
                //first try and resolve relative to the manifest resource root
                final VirtualFile classPathFile = resourceRoot.getRoot().getParent().getChild(item);
                final VirtualFile topLevelClassPathFile = deploymentRoot.getRoot().getParent().getChild(item);

                if (isInside(classPathFile, topLevelRoot) || isInside(topLevelClassPathFile, topLevelRoot)) {
                    if (classPathFile.exists()) {
                        handlingExistingClassPathEntry(deploymentUnit, resourceRoots, topLevelDeployment, topLevelRoot, subDeployments, additionalModules, earLibJars, resourceRoot, target, classPathFile);
                    } else if (topLevelClassPathFile.exists()) {
                        handlingExistingClassPathEntry(deploymentUnit, resourceRoots, topLevelDeployment, topLevelRoot, subDeployments, additionalModules, earLibJars, resourceRoot, target, topLevelClassPathFile);
                    } else {
                        log.warn("Class Path entry " + item + " in " + resourceRoot.getRoot() + "  does not point to a valid jar for a Class-Path reference.");
                    }
                } else if (item.startsWith("/")) {
                    ModuleIdentifier moduleIdentifier = externalModuleService.addExternalModule(item);
                    target.addToAttachmentList(Attachments.CLASS_PATH_ENTRIES, moduleIdentifier);
                    log.debugf("Resource %s added as external jar %s", classPathFile, resourceRoot.getRoot());
                } else {
                    //ignore
                    log.debugf("Ignoring missing Class-Path entry %s", classPathFile);
                }
            }
        }
    }

    private void handlingExistingClassPathEntry(final DeploymentUnit deploymentUnit, final ArrayDeque<ResourceRoot> resourceRoots, final DeploymentUnit topLevelDeployment, final VirtualFile topLevelRoot, final Map<VirtualFile, ResourceRoot> subDeployments, final Map<VirtualFile, AdditionalModuleSpecification> additionalModules, final Set<VirtualFile> earLibJars, final ResourceRoot resourceRoot, final Attachable target, final VirtualFile topLevelClassPathFile) {
        if (earLibJars.contains(topLevelClassPathFile)) {
            log.debugf("Class-Path entry %s in %s ignored, as target is in or referenced by /lib", topLevelClassPathFile, resourceRoot.getRoot());
        } else if (additionalModules.containsKey(topLevelClassPathFile)) {
            target.addToAttachmentList(Attachments.CLASS_PATH_ENTRIES, additionalModules.get(topLevelClassPathFile).getModuleIdentifier());
        } else if (subDeployments.containsKey(topLevelClassPathFile)) {
            //now we need to calculate the sub deployment module identifer
            //unfortunately the sub deployment has not been setup yet, so we cannot just
            //get it from the sub deployment directly
            final ResourceRoot otherRoot = subDeployments.get(topLevelClassPathFile);
            target.addToAttachmentList(Attachments.CLASS_PATH_ENTRIES, ModuleIdentifierProcessor.createModuleIdentifier(otherRoot.getRootName(), otherRoot, topLevelDeployment, topLevelRoot, false));
        } else {
            createAdditionalModule(deploymentUnit, topLevelDeployment, topLevelRoot, additionalModules, resourceRoot, topLevelClassPathFile, resourceRoots);
        }
    }

    private void createAdditionalModule(final DeploymentUnit deploymentUnit, final DeploymentUnit topLevelDeployment, final VirtualFile topLevelRoot, final Map<VirtualFile, AdditionalModuleSpecification> additionalModules, final ResourceRoot resourceRoot, final VirtualFile classPathFile, final ArrayDeque<ResourceRoot> resourceRoots) {
        final ResourceRoot root = createResourceRoot(deploymentUnit, classPathFile);

        //add this to the list of roots to be processed, so transitive class path entries will be respected
        resourceRoots.add(root);
        String pathName = root.getRoot().getPathNameRelativeTo(topLevelRoot);
        ModuleIdentifier identifier = ModuleIdentifier.create(ServiceModuleLoader.MODULE_PREFIX + topLevelDeployment.getName() + "." + pathName);
        AdditionalModuleSpecification module = new AdditionalModuleSpecification(identifier, root);
        topLevelDeployment.addToAttachmentList(Attachments.ADDITIONAL_MODULES, module);
        additionalModules.put(classPathFile, module);
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
    private synchronized ResourceRoot createResourceRoot(final DeploymentUnit deploymentUnit, final VirtualFile file) {
        try {
            final Closeable closable = file.isFile() ? VFS.mountZip(file, file, TempFileProviderService.provider()) : null;
            final MountHandle mountHandle = new MountHandle(closable);
            final ResourceRoot resourceRoot = new ResourceRoot(file, mountHandle);
            deploymentUnit.addToAttachmentList(Attachments.RESOURCE_ROOTS, resourceRoot);
            ModuleRootMarker.mark(resourceRoot);
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
