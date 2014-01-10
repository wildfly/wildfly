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

import java.io.Closeable;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.logging.LoggingLogger;
import org.jboss.as.logging.logmanager.WildFlyLogContextSelector;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.SubDeploymentMarker;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.logmanager.LogContext;
import org.jboss.modules.Module;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractLoggingDeploymentProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<LogContext> LOG_CONTEXT_KEY = AttachmentKey.create(LogContext.class);

    protected final WildFlyLogContextSelector logContextSelector;

    protected AbstractLoggingDeploymentProcessor(final WildFlyLogContextSelector logContextSelector) {
        this.logContextSelector = logContextSelector;
    }

    @Override
    public final void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        // If the log context is already defined, skip the rest of the processing
        if (!hasRegisteredLogContext(deploymentUnit)) {
            if (deploymentUnit.hasAttachment(Attachments.MODULE) && deploymentUnit.hasAttachment(Attachments.DEPLOYMENT_ROOT)) {
                // don't process sub-deployments as they are processed by processing methods
                final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
                if (SubDeploymentMarker.isSubDeployment(root)) return;
                processDeployment(phaseContext, deploymentUnit, root);
            }
        }
    }

    @Override
    public final void undeploy(final DeploymentUnit context) {
        // OSGi bundles deployments may not have a module attached
        if (hasRegisteredLogContext(context) && context.hasAttachment(Attachments.MODULE)) {
            // don't process sub-deployments as they are processed by processing methods
            final ResourceRoot root = context.getAttachment(Attachments.DEPLOYMENT_ROOT);
            if (SubDeploymentMarker.isSubDeployment(root)) return;
            // Remove any log context selector references
            final Module module = context.getAttachment(Attachments.MODULE);
            unregisterLogContext(context, module);
            // Unregister all sub-deployments
            final List<DeploymentUnit> subDeployments = getSubDeployments(context);
            for (DeploymentUnit subDeployment : subDeployments) {
                final Module subDeploymentModule = subDeployment.getAttachment(Attachments.MODULE);
                unregisterLogContext(subDeployment, subDeploymentModule);
            }
        }
    }

    /**
     * Processes the deployment.
     *
     * @param phaseContext   the phase context
     * @param deploymentUnit the deployment unit
     * @param root           the root resource
     *
     * @throws DeploymentUnitProcessingException if an error occurs during processing
     */
    protected abstract void processDeployment(DeploymentPhaseContext phaseContext, DeploymentUnit deploymentUnit, ResourceRoot root) throws DeploymentUnitProcessingException;

    protected void registerLogContext(final DeploymentUnit deploymentUnit, final Module module, final LogContext logContext) {
        LoggingLogger.ROOT_LOGGER.tracef("Registering LogContext %s for deployment %s", logContext, deploymentUnit.getName());
        if (WildFlySecurityManager.isChecking()) {
            WildFlySecurityManager.doChecked(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    logContextSelector.registerLogContext(module.getClassLoader(), logContext);
                    return null;
                }
            });
        } else {
            logContextSelector.registerLogContext(module.getClassLoader(), logContext);
        }
        // Add the log context to the sub-deployment unit for later removal
        deploymentUnit.putAttachment(LOG_CONTEXT_KEY, logContext);
    }

    protected void unregisterLogContext(final DeploymentUnit deploymentUnit, final Module module) {
        final LogContext logContext = deploymentUnit.removeAttachment(LOG_CONTEXT_KEY);
        final boolean success;
        if (WildFlySecurityManager.isChecking()) {
            success = WildFlySecurityManager.doChecked(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return logContextSelector.unregisterLogContext(module.getClassLoader(), logContext);
                }
            });
        } else {
            success = logContextSelector.unregisterLogContext(module.getClassLoader(), logContext);
        }
        if (success) {
            LoggingLogger.ROOT_LOGGER.tracef("Removed LogContext '%s' from '%s'", logContext, module);
        } else {
            LoggingLogger.ROOT_LOGGER.logContextNotRemoved(logContext, deploymentUnit.getName());
        }
    }

    protected static List<DeploymentUnit> getSubDeployments(final DeploymentUnit deploymentUnit) {
        if (deploymentUnit.hasAttachment(Attachments.SUB_DEPLOYMENTS)) {
            final List<DeploymentUnit> result = new ArrayList<DeploymentUnit>();
            result.addAll(deploymentUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS));
            return result;
        }
        return Collections.emptyList();
    }

    protected static void safeClose(final Closeable closable) {
        if (closable != null) try {
            closable.close();
        } catch (Exception e) {
            // no-op
        }
    }

    /**
     * Checks the deployment to see if it has a registered {@link org.jboss.logmanager.LogContext log context}.
     *
     * @param deploymentUnit the deployment unit to check
     *
     * @return {@code true} if the deployment unit has a log context, otherwise {@code false}
     */
    public static boolean hasRegisteredLogContext(final DeploymentUnit deploymentUnit) {
        return deploymentUnit.hasAttachment(LOG_CONTEXT_KEY);
    }
}
