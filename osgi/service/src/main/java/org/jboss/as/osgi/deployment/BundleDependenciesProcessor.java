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

package org.jboss.as.osgi.deployment;

import static org.jboss.as.osgi.service.InitialDeploymentTracker.INITIAL_DEPLOYMENTS_COMPLETE;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.osgi.deployment.deployer.Deployment;

/**
 * Add a dependency on the resolve phase for all persisten bundles to be installed.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 16-Nov-2012
 */
public class BundleDependenciesProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final Deployment deployment = depUnit.getAttachment(OSGiConstants.DEPLOYMENT_KEY);
        if (deployment == null)
            return;

        // Add a dependency on the next phase for all persisten bundles to be installed
        phaseContext.addDeploymentDependency(INITIAL_DEPLOYMENTS_COMPLETE, AttachmentKey.create(Object.class));
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
