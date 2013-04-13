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

import static org.jboss.osgi.framework.FrameworkLogger.LOGGER;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Substate;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XResource.State;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Uninstalls OSGi deployments from the Framework.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 12-Apr-2013
 */
public class BundleUninstallProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        // do nothing
    }

    @Override
    public void undeploy(DeploymentUnit depUnit) {

        XBundleRevision brev = depUnit.getAttachment(OSGiConstants.BUNDLE_REVISION_KEY);
        if (brev == null)
            return;

        XBundle bundle = brev.getBundle();
        BundleManager bundleManager = depUnit.getAttachment(OSGiConstants.BUNDLE_MANAGER_KEY);
        if (uninstallRequired(depUnit, brev)) {
            try {
                int options = getUninstallOptions(bundleManager);
                bundleManager.uninstallBundle(bundle, options);
            } catch (BundleException ex) {
                LOGGER.debugf(ex, "Cannot uninstall bundle: %s", bundle);
            }
        }
    }

    private boolean uninstallRequired(DeploymentUnit depUnit, XBundleRevision brev) {

        // No uninstall when activation failed
        Boolean startFailed = depUnit.removeAttachment(OSGiConstants.DEFERRED_ACTIVATION_FAILED);
        if (Boolean.TRUE.equals(startFailed))
            return false;

        // No uninstall if the bundle is already uninstalled
        XBundle bundle = brev.getBundle();
        if (bundle.getState() == Bundle.UNINSTALLED || brev.getState() == State.UNINSTALLED)
            return false;

        // No uninstall if this is not the current revision
        if (bundle.getBundleRevision() != brev)
            return false;

        // No uninstall if the revision service goes down because of a bundle refresh
        //Method method = bundle.getAttachment(InternalConstants.LOCK_METHOD_KEY);
        //if (method == Method.REFRESH)
        //    return false;

        return true;
    }

    private int getUninstallOptions(BundleManager bundleManager) {
        ServiceContainer serviceContainer = bundleManager.getServiceContainer();
        ServiceController<?> controller = serviceContainer.getRequiredService(Services.JBOSS_SERVER_CONTROLLER);
        boolean stopRequested = controller.getSubstate() == Substate.STOP_REQUESTED;
        return stopRequested ? Bundle.STOP_TRANSIENT : 0;
    }
}
