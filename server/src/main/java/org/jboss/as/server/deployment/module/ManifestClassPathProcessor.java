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

import org.jboss.as.server.deployment.Attachable;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.moduleservice.ExternalModuleService;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VirtualFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * A processor which adds class path entries for each manifest entry.
 * <p>
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

    /** {@inheritDoc} */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        //if this has already been handled by the ear class path processor
        if(deploymentUnit.getAttachment(Attachments.CLASS_PATH_ENTRIES) != null) {
            return;
        }

        final List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);

        final DeploymentUnit parent = deploymentUnit.getParent();
        final DeploymentUnit topLevelDeployment = parent == null ? deploymentUnit : parent;
        final VirtualFile topLevelRoot = topLevelDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        final ExternalModuleService externalModuleService = topLevelDeployment.getAttachment(Attachments.EXTERNAL_MODULE_SERVICE);
        final List<AdditionalModuleSpecification> additionalModuleList = topLevelDeployment.getAttachment(Attachments.ADDITIONAL_MODULES);
        final List<ResourceRoot> topLevelResourceRoots = topLevelDeployment.getAttachment(Attachments.RESOURCE_ROOTS);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final List<DeploymentUnit> subDeployments;
        if(deploymentUnit.getParent() == null) {
            subDeployments = deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);
        } else {
            subDeployments = deploymentUnit.getParent().getAttachmentList(Attachments.SUB_DEPLOYMENTS);
        }
        final Map<VirtualFile,ModuleIdentifier> subDeploymentModules = new HashMap<VirtualFile,ModuleIdentifier>();
        for(DeploymentUnit deployment : subDeployments) {
            final ResourceRoot root = deployment.getAttachment(Attachments.DEPLOYMENT_ROOT);
            final ModuleIdentifier identifier = deployment.getAttachment(Attachments.MODULE_IDENTIFIER);
            if(root == null || identifier == null) {
                continue;
            }
            subDeploymentModules.put(root.getRoot(),identifier);
        }

        // build a map of the additional module locations
        final Map<VirtualFile, AdditionalModuleSpecification> additionalModules;
        if (additionalModuleList == null) {
            additionalModules = Collections.emptyMap();
        } else {
            additionalModules = new HashMap<VirtualFile, AdditionalModuleSpecification>();
            for (AdditionalModuleSpecification module : additionalModuleList) {
                for (ResourceRoot additionalModuleResourceRoot : module.getResourceRoots()) {
                    additionalModules.put(additionalModuleResourceRoot.getRoot(), module);
                }
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

        for (ResourceRoot resourceRoot : resourceRoots) {
            if(SubDeploymentMarker.isSubDeployment(resourceRoot) && resourceRoot != deploymentRoot) {
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
                final VirtualFile classPathFile = resourceRoot.getRoot().getParent().getChild(item);
                if (isInside(classPathFile, topLevelRoot)) {
                    if (earLibJars.contains(classPathFile)) {
                        log.debugf("Class-Path entry %s in %s ignored, as target is in or referenced by /lib", classPathFile,
                                resourceRoot.getRoot());
                        continue; // we already have access to ear/lib
                    } else if (additionalModules.containsKey(classPathFile)) {
                        target.addToAttachmentList(Attachments.CLASS_PATH_ENTRIES, additionalModules.get(classPathFile)
                                .getModuleIdentifier());
                    } else if (subDeploymentModules.containsKey(classPathFile)) {
                        target.addToAttachmentList(Attachments.CLASS_PATH_ENTRIES, subDeploymentModules.get(classPathFile));
                    } else {
                        log.warn("Class Path entry " + item + " in "
                                + resourceRoot.getRoot() + "  does not point to a valid jar for a Class-Path reference.");
                    }
                }  else if(item.startsWith("/")) {
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
        final Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
        if (manifest == null) {
            // no class path to process!
            return EMPTY_STRING_ARRAY;
        }
        final String classPathString = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
        if (classPathString == null) {
            // no entry
            return EMPTY_STRING_ARRAY;
        }
        if(classPathString.trim().isEmpty()) {
            return EMPTY_STRING_ARRAY;
        }
        return classPathString.trim().split("\\s+");
    }

    /** {@inheritDoc} */
    public void undeploy(final DeploymentUnit context) {
    }
}
