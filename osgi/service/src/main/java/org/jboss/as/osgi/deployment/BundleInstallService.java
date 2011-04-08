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

import org.jboss.as.osgi.service.BundleContextService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Services;
import org.jboss.logging.Logger;
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
import org.jboss.osgi.framework.BundleManagement;
import org.jboss.osgi.framework.ServiceNames;
import org.osgi.framework.BundleContext;

/**
 * Service responsible for creating and managing the life-cycle of an OSGi deployment.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class BundleInstallService implements Service<BundleInstallService> {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("osgi", "deployment");

    private final Deployment deployment;
    private InjectedValue<BundleContext> injectedBundleContext = new InjectedValue<BundleContext>();
    private InjectedValue<BundleManagement> injectedBundleManager = new InjectedValue<BundleManagement>();
    private InjectedValue<BundleStartupProcessor> injectedStartTracker = new InjectedValue<BundleStartupProcessor>();
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
        builder.addDependency(ServiceNames.SYSTEM_CONTEXT, BundleContext.class, service.injectedBundleContext);
        builder.addDependency(ServiceNames.BUNDLE_MANAGER, BundleManagement.class, service.injectedBundleManager);
        builder.addDependency(BundleStartupProcessor.SERVICE_NAME, BundleStartupProcessor.class, service.injectedStartTracker);
        builder.addDependency(org.jboss.as.server.deployment.Services.deploymentUnitName(contextName));
        builder.install();
    }

    public static void removeService(DeploymentUnit context) {
        final ServiceName serviceName = getServiceName(context.getName());
        final ServiceController<?> serviceController = context.getServiceRegistry().getService(serviceName);
        if (serviceController != null) {
            serviceController.setMode(Mode.REMOVE);
        }
    }

    public static ServiceName getServiceName(String contextName) {
        ServiceName deploymentServiceName = Services.deploymentUnitName(contextName);
        return BundleInstallService.SERVICE_NAME_BASE.append(deploymentServiceName.getSimpleName());
    }

    /**
     * Install the Bundle associated with this deployment.
     */
    public synchronized void start(StartContext context) throws StartException {
        log.infof("Installing deployment: %s", deployment);
        try {
            ServiceTarget serviceTarget = context.getChildTarget();
            BundleManagement bundleManager = injectedBundleManager.getValue();
            installedBundleName = bundleManager.installBundle(serviceTarget, deployment);
            injectedStartTracker.getValue().addInstalledBundle(installedBundleName, deployment);
        } catch (Throwable t) {
            throw new StartException("Failed to install deployment: " + deployment, t);
        }
    }

    /**
     * Uninstall the Bundle associated with this deployment.
     *
     * @param context The stop context.
     */
    public synchronized void stop(StopContext context) {
        log.infof("Uninstalling deployment: %s", deployment);
        try {
            BundleManagement bundleManager = injectedBundleManager.getValue();
            bundleManager.uninstallBundle(deployment);
        } catch (Throwable t) {
            log.errorf(t, "Failed to uninstall deployment: %s", deployment);
        }

        // [JBAS-8801] Undeployment leaks root deployment service
        // [TODO] remove this workaround
        ServiceName serviceName = Services.deploymentUnitName(context.getController().getName().getSimpleName());
        ServiceController<?> deploymentController = context.getController().getServiceContainer().getService(serviceName);
        if (deploymentController != null) {
            deploymentController.setMode(Mode.REMOVE);
        }
    }

    @Override
    public BundleInstallService getValue() throws IllegalStateException {
        return this;
    }

    public ServiceName getInstalledBundleName() {
        return installedBundleName;
    }
}
