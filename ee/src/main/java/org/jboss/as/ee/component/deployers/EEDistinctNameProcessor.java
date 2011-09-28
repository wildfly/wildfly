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

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;

import java.util.jar.Manifest;

/**
 * Examines a top level manifest to see if a deployment has been given a distinct name
 *
 * @author Stuart Douglas
 */
public final class EEDistinctNameProcessor implements DeploymentUnitProcessor {

    public static final String DISTINCT_NAME = "Distinct-Name";

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription module = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final ResourceRoot deploymentRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        if (module == null) {
            return;
        }
        //if this is a sub deployment we share the DN with the parent
        if (deploymentUnit.getParent() != null) {
            final EEModuleDescription parentDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            module.setDistinctName(parentDescription.getDistinctName());
            return;
        }

        final Manifest manifest = deploymentRoot.getAttachment(Attachments.MANIFEST);
        if (manifest == null) {
            return;
        }

        final String name = manifest.getMainAttributes().getValue(DISTINCT_NAME);
        if (name == null) {
            return;
        }
        if (!name.trim().isEmpty()) {
            module.setDistinctName(name.trim());
        }

    }

    public void undeploy(final DeploymentUnit context) {
    }
}
