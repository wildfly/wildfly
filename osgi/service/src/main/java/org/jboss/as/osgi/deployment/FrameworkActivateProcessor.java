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

import static org.jboss.osgi.framework.IntegrationServices.BOOTSTRAP_BUNDLES_COMPLETE;
import static org.jboss.osgi.framework.Services.FRAMEWORK_ACTIVE;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.service.FrameworkActivationService;
import org.jboss.as.osgi.service.PersistentBundlesIntegration.InitialDeploymentTracker;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.framework.Services;

/**
 * Activates the OSGi subsystem if an OSGi deployment is detected.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Jun-2012
 */
public class FrameworkActivateProcessor implements DeploymentUnitProcessor {

    private final InitialDeploymentTracker deploymentTracker;

    public FrameworkActivateProcessor(InitialDeploymentTracker deploymentTracker) {
        this.deploymentTracker = deploymentTracker;
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // Always make the system context & the environment available
        phaseContext.addDeploymentDependency(Services.SYSTEM_CONTEXT, OSGiConstants.SYSTEM_CONTEXT_KEY);
        phaseContext.addDeploymentDependency(Services.ENVIRONMENT, OSGiConstants.ENVIRONMENT_KEY);

        // Not a bundle deployment - do nothing
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        if (depUnit.hasAttachment(OSGiConstants.DEPLOYMENT_KEY)) {

            // Install the {@link FrameworkActivationService} if not done so already
            if (FrameworkActivationService.activated() == false) {
                ServiceVerificationHandler verificationHandler = depUnit.getAttachment(Attachments.SERVICE_VERIFICATION_HANDLER);
                FrameworkActivationService.addService(deploymentTracker.getServiceTarget(), verificationHandler);
            }

            // Setup a dependency on the the next phase. Persistent bundles have a dependency on the bootstrap bundles
            ServiceName phaseDependency = deploymentTracker.isComplete() ? FRAMEWORK_ACTIVE : BOOTSTRAP_BUNDLES_COMPLETE;
            phaseContext.addDeploymentDependency(phaseDependency, AttachmentKey.create(Object.class));

            // Make these services available for a bundle deployment only
            phaseContext.addDeploymentDependency(Services.BUNDLE_MANAGER, OSGiConstants.BUNDLE_MANAGER_KEY);
            phaseContext.addDeploymentDependency(Services.RESOLVER, OSGiConstants.RESOLVER_KEY);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
