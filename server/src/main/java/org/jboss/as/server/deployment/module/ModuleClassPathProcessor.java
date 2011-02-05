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

package org.jboss.as.server.deployment.module;

import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceName;

/**
 * The processor which adds {@code MANIFEST.MF} {@code Class-Path} entries to the module configuration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ModuleClassPathProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final AttachmentList<ClassPathEntry> entries = deploymentUnit.getAttachment(Attachments.CLASS_PATH_ENTRIES);
        if (entries == null || entries.isEmpty()) {
            return;
        }
        final AttachmentList<ResourceRoot> resourceRoots = deploymentUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        final DeploymentUnit parent = deploymentUnit.getParent();
        if (parent == null) {
            // we depend on sibling deployments
            AttachmentList<ServiceName> deps = phaseContext.getAttachment(Attachments.NEXT_PHASE_DEPS);
            if (deps == null) phaseContext.putAttachment(Attachments.NEXT_PHASE_DEPS, deps = new AttachmentList<ServiceName>(ServiceName.class));
            for (ClassPathEntry entry : entries) {
                final String classPath = entry.getRelativeUrl();
                final String deploymentName;
                int firstSlash = classPath.indexOf('/');
                if (firstSlash != -1) {
                    // TODO: multi-part relative deployment; find sub-JARs in the referenced sibling
                    deploymentName = classPath.substring(0, firstSlash);
                } else {
                    deploymentName = classPath;
                }
                if (deploymentName.equals(".") || deploymentName.equals("..")) {
                    throw new DeploymentUnitProcessingException("Invalid relative path '" + classPath + "' in class path entry");
                }
                deps.add(Services.deploymentUnitName(deploymentName, phaseContext.getPhase()));
                // TODO: Need to add a resource root with a "future" virtual file root from an injected dependency
                // resourceRoots.add(new ResourceRoot(classPath, null, null, false));
            }
        } else {
            // TODO: add support for subdeployment dependencies
            throw new DeploymentUnitProcessingException("Class-Path on subdeployments not yet supported");
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }
}
