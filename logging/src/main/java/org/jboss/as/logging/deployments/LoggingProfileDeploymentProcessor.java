/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.deployments;

import java.util.List;
import java.util.jar.Manifest;

import org.jboss.as.logging.LoggingLogger;
import org.jboss.as.logging.LoggingProfileContextSelector;
import org.jboss.as.logging.logmanager.WildFlyLogContextSelector;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logmanager.LogContext;
import org.jboss.modules.Module;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LoggingProfileDeploymentProcessor extends AbstractLoggingDeploymentProcessor implements DeploymentUnitProcessor {

    private static final String LOGGING_PROFILE = "Logging-Profile";

    public LoggingProfileDeploymentProcessor(final WildFlyLogContextSelector logContextSelector) {
        super(logContextSelector);
    }

    @Override
    protected void processDeployment(final DeploymentPhaseContext phaseContext, final DeploymentUnit deploymentUnit, final ResourceRoot root) throws DeploymentUnitProcessingException {
        final List<DeploymentUnit> subDeployments = getSubDeployments(deploymentUnit);
        final String loggingProfile = findLoggingProfile(root);
        if (loggingProfile != null) {
            // Get the profile logging context
            final LoggingProfileContextSelector loggingProfileContext = LoggingProfileContextSelector.getInstance();
            if (loggingProfileContext.exists(loggingProfile)) {
                // Get the module
                final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
                final LogContext logContext = loggingProfileContext.get(loggingProfile);
                LoggingLogger.ROOT_LOGGER.tracef("Registering log context '%s' on '%s' for profile '%s'", logContext, root, loggingProfile);
                registerLogContext(deploymentUnit, module, logContext);
                // Process sub-deployments
                for (DeploymentUnit subDeployment : subDeployments) {
                    // A sub-deployment must have a module to process
                    if (subDeployment.hasAttachment(Attachments.MODULE)) {
                        // Set the result to true if a logging profile was found
                        if (subDeployment.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
                            processDeployment(phaseContext, subDeployment, subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT));
                        }
                        if (!hasRegisteredLogContext(subDeployment)) {
                            final Module subDeploymentModule = subDeployment.getAttachment(Attachments.MODULE);
                            LoggingLogger.ROOT_LOGGER.tracef("Registering log context '%s' on '%s' for profile '%s'", logContext, subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT), loggingProfile);
                            registerLogContext(subDeployment, subDeploymentModule, logContext);
                        }
                    }
                }
            } else {
                LoggingLogger.ROOT_LOGGER.loggingProfileNotFound(loggingProfile, root);
            }
        } else {
            // No logging profile found, but the sub-deployments should be checked for logging profiles
            for (DeploymentUnit subDeployment : subDeployments) {
                // A sub-deployment must have a root resource and a module to process
                if (subDeployment.hasAttachment(Attachments.MODULE) && subDeployment.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
                    processDeployment(phaseContext, subDeployment, subDeployment.getAttachment(Attachments.DEPLOYMENT_ROOT));
                }
            }
        }
    }

    /**
     * Find the logging profile attached to any resource.
     *
     * @param resourceRoot the root resource
     *
     * @return the logging profile name or {@code null} if one was not found
     */
    private String findLoggingProfile(final ResourceRoot resourceRoot) {
        final Manifest manifest = resourceRoot.getAttachment(Attachments.MANIFEST);
        if (manifest != null) {
            final String loggingProfile = manifest.getMainAttributes().getValue(LOGGING_PROFILE);
            if (loggingProfile != null) {
                LoggingLogger.ROOT_LOGGER.debugf("Logging profile '%s' found in '%s'.", loggingProfile, resourceRoot);
                return loggingProfile;
            }
        }
        return null;
    }
}
