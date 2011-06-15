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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

import java.util.List;

/**
 * Deployment unit processor that will extract module dependencies from an archive.
 *
 * @author John E. Bailey
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ModuleDependencyProcessor implements DeploymentUnitProcessor {

    /**
     * Process the deployment root for module dependency information.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);

        moduleSpecification.addUserDependencies(deploymentUnit.getAttachmentList(Attachments.MANIFEST_DEPENDENCIES));

        if (deploymentUnit.getParent() != null) {
            // propagate parent manifest dependencies
            final List<ModuleDependency> parentDependencies = deploymentUnit.getParent().getAttachmentList(Attachments.MANIFEST_DEPENDENCIES);
            moduleSpecification.addUserDependencies(parentDependencies);
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    private boolean containsParam(final String[] parts, final String expected) {
        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                if (expected.equals(parts[i])) {
                    return true;
                }
            }
        }
        return false;
    }
}
