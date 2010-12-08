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

import org.jboss.as.deployment.DeploymentService;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.as.osgi.service.BundleManagerService;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.bundle.AbstractBundleContext;
import org.jboss.osgi.framework.bundle.BundleManager;

/**
 * A service that marks a bundle deployment through the BundleContext.installBundle(...) API.
 *
 * The {@link Deployment} returned by the service value is the one constructed in {@link AbstractBundleContext} It is not the
 * one constructed by the OSGi {@link DeploymentUnitProcessor}s
 *
 * @author Thomas.Diesler@jboss.com
 * @since 24-Nov-2010
 */
public class InstallBundleInitiatorService extends AbstractService<Deployment> {

    private static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("osgi", "bundle", "install");
    private final Deployment deployment;

    private InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();

    private InstallBundleInitiatorService(Deployment deployment) {
        this.deployment = deployment;
    }

    public static void addService(BatchBuilder batchBuilder, String contextName, Deployment dep) {
        InstallBundleInitiatorService service = new InstallBundleInitiatorService(dep);
        ServiceBuilder<Deployment> serviceBuilder = batchBuilder.addService(getServiceName(contextName), service);
        serviceBuilder.addDependency(BundleManagerService.SERVICE_NAME, BundleManager.class, service.injectedBundleManager);
        serviceBuilder.setInitialMode(Mode.ACTIVE);
        serviceBuilder.install();
    }

    /**
     * Get the {@link DeploymentUnitContext} name for the given deployment
     */
    public static String getContextName(Deployment dep) {
        String name = dep.getLocation();

        // The location prefix for non-osgi modules
        String prefix = "module:deployment.";
        if (name.startsWith(prefix))
            name = name.substring(prefix.length());

        if (name.endsWith("/"))
            name = name.substring(0, name.length() - 1);
        int idx = name.lastIndexOf("/");
        if (idx > 0)
            name = name.substring(idx + 1);

        return name;
    }

    public static ServiceName getServiceName(String contextName) {
        ServiceName deploymentServiceName = DeploymentService.getServiceName(contextName);
        return SERVICE_NAME_BASE.append(deploymentServiceName.getSimpleName());
    }

    public static String getLocation(ServiceRegistry registry, String contextName) {
        // If this service is registered use the location from the original deployemnt
        ServiceController<?> controller = registry.getService(getServiceName(contextName));
        return controller != null ? ((Deployment) controller.getValue()).getLocation() : contextName;
    }

    @Override
    public Deployment getValue() throws IllegalStateException {
        return deployment;
    }
}
