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
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.server.deployment.Services.deploymentUnitName;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.BundleManagerService;
import org.jboss.osgi.framework.Services;

/**
 * Service responsible for creating and managing the life-cycle of an OSGi deployment.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class BundleInstallService implements Service<BundleInstallService> {

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("osgi", "deployment");

    private final Deployment deployment;
    private InjectedValue<BundleManagerService> injectedBundleManager = new InjectedValue<BundleManagerService>();
    private ServiceName installedBundleName;

    private BundleInstallService(Deployment deployment) {
        this.deployment = deployment;
    }

    public static void addService(DeploymentPhaseContext phaseContext, Deployment deployment) {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final BundleInstallService service = new BundleInstallService(deployment);
        final String contextName = deploymentUnit.getName();
        final ServiceName serviceName = getServiceName(contextName);
        final ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        ServiceBuilder<BundleInstallService> builder = serviceTarget.addService(serviceName, service);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManagerService.class, service.injectedBundleManager);
        builder.addDependency(deploymentUnitName(contextName));
        builder.addDependency(Services.FRAMEWORK_ACTIVATOR);
        builder.install();
    }

    public static void removeService(DeploymentUnit depUnit) {
        final ServiceName serviceName = getServiceName(depUnit.getName());
        final ServiceController<?> serviceController = depUnit.getServiceRegistry().getService(serviceName);
        if (serviceController != null) {
            serviceController.setMode(Mode.REMOVE);
        }
    }

    public static ServiceName getServiceName(String contextName) {
        ServiceName deploymentServiceName = deploymentUnitName(contextName);
        return BundleInstallService.SERVICE_NAME_BASE.append(deploymentServiceName.getSimpleName());
    }

    public synchronized void start(StartContext context) throws StartException {
        ServiceController<?> controller = context.getController();
        LOGGER.debugf("Starting: %s in mode %s", controller.getName(), controller.getMode());
        try {
            ServiceTarget serviceTarget = context.getChildTarget();
            BundleManagerService bundleManager = injectedBundleManager.getValue();
            installedBundleName = bundleManager.installBundle(serviceTarget, deployment);
        } catch (Throwable t) {
            throw new StartException(MESSAGES.failedToInstallDeployment(deployment), t);
        }
    }

    public synchronized void stop(StopContext context) {
        ServiceController<?> controller = context.getController();
        LOGGER.debugf("Stopping: %s in mode %s", controller.getName(), controller.getMode());
        try {
            BundleManagerService bundleManager = injectedBundleManager.getValue();
            bundleManager.uninstallBundle(deployment);
        } catch (Throwable t) {
            LOGGER.errorFailedToUninstallDeployment(t, deployment);
        }

        // [JBAS-8801] Undeployment leaks root deployment service
        // [TODO] remove this workaround
        ServiceName serviceName = deploymentUnitName(context.getController().getName().getSimpleName());
        ServiceController<?> deploymentController = context.getController().getServiceContainer().getService(serviceName);
        if (deploymentController != null) {
            deploymentController.setMode(Mode.REMOVE);
        }
    }

    @Override
    public synchronized BundleInstallService getValue() throws IllegalStateException {
        return this;
    }

    public ServiceName getInstalledBundleName() {
        return installedBundleName;
    }
}
