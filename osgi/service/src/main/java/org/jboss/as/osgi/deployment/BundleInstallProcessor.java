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

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.server.deployment.Attachments.BUNDLE_STATE_KEY;
import static org.jboss.osgi.framework.spi.IntegrationConstants.STORAGE_STATE_KEY;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.service.BundleLifecycleIntegration;
import org.jboss.as.osgi.service.InitialDeploymentTracker;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments.BundleState;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceController.Substate;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.framework.spi.IntegrationServices;
import org.jboss.osgi.framework.spi.StorageManager;
import org.jboss.osgi.framework.spi.StorageState;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XResource.State;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Processes deployments that have OSGi metadata attached.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class BundleInstallProcessor implements DeploymentUnitProcessor {

    private final InitialDeploymentTracker deploymentTracker;

    public BundleInstallProcessor(InitialDeploymentTracker deploymentTracker) {
        this.deploymentTracker = deploymentTracker;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final Deployment deployment = depUnit.getAttachment(OSGiConstants.DEPLOYMENT_KEY);
        if (deployment == null)
            return;

        try {
            BundleManager bundleManager = depUnit.getAttachment(OSGiConstants.BUNDLE_MANAGER_KEY);
            BundleContext syscontext = depUnit.getAttachment(OSGiConstants.SYSTEM_CONTEXT_KEY);
            if (deploymentTracker.hasDeploymentName(depUnit.getName())) {
                restoreStorageState(phaseContext, deployment);
            }
            XBundleRevision brev = bundleManager.createBundleRevision(syscontext, deployment, phaseContext.getServiceTarget());
            depUnit.putAttachment(OSGiConstants.BUNDLE_REVISION_KEY, brev);
            depUnit.putAttachment(BUNDLE_STATE_KEY, BundleState.INSTALLED);
        } catch (BundleException ex) {
            throw new DeploymentUnitProcessingException(ex);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
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
                Deployment dep = depUnit.getAttachment(OSGiConstants.DEPLOYMENT_KEY);
                LOGGER.errorFailedToUninstallDeployment(ex, dep);
            }
        } else {
            BundleWiring wiring = brev.getWiringSupport().getWiring(false);
            if (wiring == null || !wiring.isInUse()) {
                bundleManager.removeRevision(brev, 0);
            }
        }
    }

    private void restoreStorageState(final DeploymentPhaseContext phaseContext, final Deployment deployment) {
        ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();
        StorageManager storageProvider = (StorageManager) serviceRegistry.getRequiredService(IntegrationServices.STORAGE_MANAGER_PLUGIN).getValue();
        StorageState storageState = storageProvider.getStorageState(deployment.getLocation());
        if (storageState != null) {
            deployment.setAutoStart(storageState.isPersistentlyStarted());
            deployment.putAttachment(STORAGE_STATE_KEY, storageState);
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

        // No uninstall if the bundle is refreshing
        if (BundleLifecycleIntegration.isBundleRefreshing(bundle))
            return false;

        return true;
    }

    private int getUninstallOptions(BundleManager bundleManager) {
        ServiceContainer serviceContainer = bundleManager.getServiceContainer();
        ServiceController<?> controller = serviceContainer.getRequiredService(Services.JBOSS_SERVER_CONTROLLER);
        boolean stopRequested = controller.getSubstate() == Substate.STOP_REQUESTED;
        return stopRequested ? Bundle.STOP_TRANSIENT : 0;
    }
}
