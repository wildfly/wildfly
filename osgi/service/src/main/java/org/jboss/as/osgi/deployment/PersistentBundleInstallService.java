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

import static org.jboss.as.osgi.OSGiConstants.SERVICE_BASE_NAME;
import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.server.deployment.Services.deploymentUnitName;

import org.jboss.as.osgi.service.PersistentBundlesIntegration.InitialDeploymentTracker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.framework.IntegrationServices;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.StorageState;
import org.jboss.osgi.framework.StorageStateProvider;

/**
 * Service responsible for creating and managing the life-cycle of an OSGi deployment.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class PersistentBundleInstallService extends AbstractService<Void> {

    static final ServiceName SERVICE_NAME_BASE = SERVICE_BASE_NAME.append("persistent", "bundle", "install");

    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private final InjectedValue<StorageStateProvider> injectedStorageProvider = new InjectedValue<StorageStateProvider>();
    private final InitialDeploymentTracker deploymentTracker;
    private final Deployment deployment;

    private PersistentBundleInstallService(InitialDeploymentTracker deploymentTracker, Deployment deployment) {
        this.deploymentTracker = deploymentTracker;
        this.deployment = deployment;
    }

    public static ServiceName addService(InitialDeploymentTracker deploymentTracker, DeploymentPhaseContext phaseContext, Deployment deployment) {
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final PersistentBundleInstallService service = new PersistentBundleInstallService(deploymentTracker, deployment);
        final String contextName = depUnit.getName();
        final ServiceName serviceName = getServiceName(depUnit);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        ServiceBuilder<Void> builder = serviceTarget.addService(serviceName, service);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, service.injectedBundleManager);
        builder.addDependency(Services.STORAGE_STATE_PROVIDER, StorageStateProvider.class, service.injectedStorageProvider);
        builder.addDependency(deploymentUnitName(contextName));
        builder.addDependency(IntegrationServices.AUTOINSTALL_HANDLER_COMPLETE);
        builder.install();
        return serviceName;
    }

    private static ServiceName getServiceName(DeploymentUnit depUnit) {
        ServiceName deploymentServiceName = deploymentUnitName(depUnit.getName());
        return SERVICE_NAME_BASE.append(deploymentServiceName.getSimpleName());
    }

    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        try {
            ServiceTarget serviceTarget = context.getChildTarget();
            BundleManager bundleManager = injectedBundleManager.getValue();
            StorageStateProvider storageStateProvider = injectedStorageProvider.getValue();
            StorageState storageState = storageStateProvider.getByLocation(deployment.getLocation());
            if (storageState != null) {
                deployment.addAttachment(StorageState.class, storageState);
            }
            ServiceName serviceName = bundleManager.installBundle(serviceTarget, deployment);
            deploymentTracker.addInstalledBundle(serviceName, deployment);
        } catch (Throwable th) {
            throw MESSAGES.startFailedToInstallDeployment(th, deployment);
        }
    }

    public synchronized void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
        try {
            BundleManager bundleManager = injectedBundleManager.getValue();
            bundleManager.uninstallBundle(deployment);
        } catch (Throwable t) {
            LOGGER.errorFailedToUninstallDeployment(t, deployment);
        }
    }
}
