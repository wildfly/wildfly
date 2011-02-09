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

import org.jboss.as.osgi.service.BundleManagerService;
import org.jboss.as.osgi.service.PackageAdminService;
import org.jboss.as.server.deployment.Services;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.bundle.BundleManager;

/**
 * Service responsible for creating and managing the life-cycle of an OSGi deployment.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Nov-2010
 */
public class ModuleRegistrationService extends AbstractService<Deployment> {

    private static final Logger log = Logger.getLogger("org.jboss.as.osgi");

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("osgi", "module", "registration");

    private final Deployment deployment;
    private InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();

    private ModuleRegistrationService(Deployment deployment) {
        this.deployment = deployment;
    }

    public static void addService(ServiceTarget serviceTarget, Deployment deployment, String contextName) {
        ModuleRegistrationService service = new ModuleRegistrationService(deployment);
        ServiceBuilder<Deployment> serviceBuilder = serviceTarget.addService(getServiceName(contextName), service);
        serviceBuilder.addDependency(BundleManagerService.SERVICE_NAME, BundleManager.class, service.injectedBundleManager);
        serviceBuilder.addDependency(PackageAdminService.SERVICE_NAME);
        serviceBuilder.addDependency(Services.deploymentUnitName(contextName));
        serviceBuilder.setInitialMode(Mode.ACTIVE);
        serviceBuilder.install();
    }

    /**
     * Get the service name for a given context
     */
    public static ServiceName getServiceName(String contextName) {
        ServiceName deploymentServiceName = Services.deploymentUnitName(contextName);
        return ModuleRegistrationService.SERVICE_NAME_BASE.append(deploymentServiceName.getSimpleName());
    }

    /**
     * Uninstall the Bundle associated with this deployment.
     *
     * @param context The stop context.
     */
    public synchronized void stop(StopContext context) {
        log.tracef("Uninstalling deployment: %s", deployment);
        try {
            BundleManager bundleManager = injectedBundleManager.getValue();
            bundleManager.uninstallBundle(deployment);

            ServiceController<?> controller = context.getController();
            ServiceContainer serviceContainer = controller.getServiceContainer();
            controller.setMode(Mode.REMOVE);

            // [JBAS-8801] Undeployment leaks root deployment service
            // [TODO] remove this workaround
            ServiceName serviceName = Services.deploymentUnitName(controller.getName().getSimpleName());
            ServiceController<?> deploymentController = serviceContainer.getService(serviceName);
            if (deploymentController != null) {
                deploymentController.setMode(Mode.REMOVE);
            }
        } catch (Throwable t) {
            log.errorf(t, "Failed to uninstall deployment: %s", deployment);
        }
    }

    @Override
    public Deployment getValue() throws IllegalStateException {
        return deployment;
    }
}
