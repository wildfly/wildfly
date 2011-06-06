/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.vfs.VirtualFile;

import java.util.List;

/**
 * User: Jaikiran Pai
 */
public class SubDeploymentClassPathAdditionProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(SubDeploymentClassPathAdditionProcessor.class);

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // we only process sub deployment of a .ear and *not* top level .ear file
        if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
            return;
        }
        // get the parent .ear file
        final DeploymentUnit parentEar = deploymentUnit.getParent();
        // if it's standalone deployment, then just return
        if (parentEar == null) {
            return;
        }

        final List<ResourceRoot> allResourceRootsOfEar = DeploymentUtils.allResourceRoots(parentEar);
        if (allResourceRootsOfEar == null || allResourceRootsOfEar.isEmpty()) {
            return;
        }
        // root of the current deployment unit being processed
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        // root of the parent .ear
        final ResourceRoot parentEarResourceRoot = parentEar.getAttachment(Attachments.DEPLOYMENT_ROOT);
        for (ResourceRoot resourceRoot : allResourceRootsOfEar) {
            // if it's not a sub deployment, then skip
            if (!SubDeploymentMarker.isSubDeployment(resourceRoot)) {
                continue;
            }
            // if it's the same as the deployment unit being processed then skip
            if (resourceRoot.equals(deploymentRoot)) {
                continue;
            }
            // if it's a .war then don't add it to the classpath of the subdeployment, since .war shouldn't be available
            // to the other subdeployments within the .ear
            if (DeploymentTypeMarker.isType(DeploymentType.WAR, resourceRoot)) {
                continue;
            }
            // get the resource root of the subdeployment
            final VirtualFile subDeploymentResourceRoot = resourceRoot.getRoot();
            // get the path of the sub deployment, relative to the parent .ear
            final String relativePath = subDeploymentResourceRoot.getPathNameRelativeTo(parentEarResourceRoot.getRoot());
            // get the module identifier of the sub deployment
            final ModuleIdentifier moduleIdentifier = ModuleIdentifier.create(ServiceModuleLoader.MODULE_PREFIX + parentEar.getName() + '.' + relativePath.replace('/', '.'));
            logger.debug("Subdeployment " + subDeploymentResourceRoot + " with module identifier " + moduleIdentifier +
                    " will be added to the classpath entries of deployment unit " + deploymentUnit);
            // add it to the class path entries list of the deployment unit being processed, so that the sub deployment
            // is now available in the classpath of the deployment unit
            deploymentUnit.addToAttachmentList(Attachments.CLASS_PATH_ENTRIES, moduleIdentifier);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
