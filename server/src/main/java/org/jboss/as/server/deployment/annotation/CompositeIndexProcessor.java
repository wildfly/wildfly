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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ModuleRootMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.moduleservice.ModuleIndexBuilder;
import org.jboss.jandex.Index;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * Processor responsible for creating and attaching a {@link CompositeIndex} for a deployment.
 * <p/>
 * This must run after the {@link org.jboss.as.server.deployment.module.ManifestDependencyProcessor}
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public class CompositeIndexProcessor implements DeploymentUnitProcessor {

    private static final Logger log = Logger.getLogger(CompositeIndexProcessor.class);

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final Boolean computeCompositeIndex = deploymentUnit.getAttachment(Attachments.COMPUTE_COMPOSITE_ANNOTATION_INDEX);
        if (computeCompositeIndex != null && !computeCompositeIndex) {
            return;
        }

        final List<ModuleIdentifier> additionalModuleIndexes = deploymentUnit.getAttachmentList(Attachments.ADDITIONAL_ANNOTATION_INDEXES);
        final List<Index> indexes = new ArrayList<Index>();
        for (final ModuleIdentifier moduleIdentifier : additionalModuleIndexes) {
            try {
                Module module = Module.getBootModuleLoader().loadModule(moduleIdentifier);
                final CompositeIndex additionalIndex = ModuleIndexBuilder.buildCompositeIndex(module);
                if (additionalIndex != null) {
                    indexes.addAll(additionalIndex.indexes);
                } else {
                    log.errorf("Module %s will not have it's annotations processed as no %s file was found in the deployment. Please generate this file using the Jandex ant task.", module.getIdentifier(), ModuleIndexBuilder.INDEX_LOCATION);
                }
            } catch (ModuleLoadException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }

        final List<ResourceRoot> allResourceRoots = new ArrayList<ResourceRoot>();
        final List<ResourceRoot> resourceRoots = deploymentUnit.getAttachmentList(Attachments.RESOURCE_ROOTS);
        for (ResourceRoot resourceRoot : resourceRoots) {
            // do not add child sub deployments to the composite index
            if (!SubDeploymentMarker.isSubDeployment(resourceRoot) && ModuleRootMarker.isModuleRoot(resourceRoot)) {
                allResourceRoots.add(resourceRoot);
            }
        }


        //we merge all Class-Path annotation indexes into the deployments composite index
        //this means that if component defining annotations (e.g. @Stateless) are specified in a Class-Path
        //entry references by two sub deployments this component will be created twice.
        //the spec expects this behaviour, and explicitly warns not to put component defining annotations
        //in Class-Path items
        allResourceRoots.addAll(handleClassPathItems(deploymentUnit));

        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        if (ModuleRootMarker.isModuleRoot(deploymentRoot)) {
            allResourceRoots.add(deploymentRoot);
        }
        for (ResourceRoot resourceRoot : allResourceRoots) {
            Index index = resourceRoot.getAttachment(Attachments.ANNOTATION_INDEX);
            if (index != null) {
                indexes.add(index);
            }
        }
        deploymentUnit.putAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX, new CompositeIndex(indexes));
    }

    /**
     * Loops through all resource roots that have been made available transitively via Class-Path entries, and
     * adds them to the list of roots to be processed.
     */
    private Collection<? extends ResourceRoot> handleClassPathItems(final DeploymentUnit deploymentUnit) {
        final Set<ResourceRoot> additionalRoots = new HashSet<ResourceRoot>();
        final ArrayDeque<ResourceRoot> toProcess = new ArrayDeque<ResourceRoot>();
        final List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        toProcess.addAll(resourceRoots);
        final Set<ResourceRoot> processed = new HashSet<ResourceRoot>(resourceRoots);

        while (!toProcess.isEmpty()) {
            final ResourceRoot root = toProcess.pop();
            final List<ResourceRoot> classPathRoots = root.getAttachmentList(Attachments.CLASS_PATH_RESOURCE_ROOTS);
            for(ResourceRoot cpRoot : classPathRoots) {
                if(!processed.contains(cpRoot)) {
                    additionalRoots.add(cpRoot);
                    toProcess.add(cpRoot);
                    processed.add(cpRoot);
                }
            }
        }
        return additionalRoots;
    }

    public void undeploy(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
    }
}
