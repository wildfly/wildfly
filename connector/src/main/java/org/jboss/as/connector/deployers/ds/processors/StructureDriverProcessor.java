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

package org.jboss.as.connector.deployers.ds.processors;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;

import java.util.List;

/**
 * Detects a JDBC driver at an early stage, and sets flags critical to the deployment.
 * Currently this is just disabling OSGi, so that it does not skip the driver processor
 *
 * @author Jason T. Greene
 */
public final class StructureDriverProcessor implements DeploymentUnitProcessor {

    /** {@inheritDoc} */
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        Boolean ignore = deploymentUnit.getAttachment(Attachments.IGNORE_OSGI);
        if (ignore != null && ignore.booleanValue()) {
            return;
        }

        List<ResourceRoot> resourceRoots = DeploymentUtils.allResourceRoots(deploymentUnit);
        for (ResourceRoot resourceRoot : resourceRoots) {
            final VirtualFile deploymentRoot = resourceRoot.getRoot();

            if (deploymentRoot.getChild("META-INF/services/java.sql.Driver").exists())  {
                deploymentUnit.putAttachment(Attachments.IGNORE_OSGI, Boolean.TRUE);
                break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void undeploy(final DeploymentUnit context) {
    }
}
