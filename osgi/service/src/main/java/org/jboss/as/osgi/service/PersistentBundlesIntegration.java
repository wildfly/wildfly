/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.osgi.service;

import java.util.HashSet;
import java.util.Set;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.spi.BootstrapBundlesInstall;
import org.jboss.osgi.framework.spi.IntegrationService;
import org.jboss.osgi.framework.spi.IntegrationServices;

/**
 * An {@link IntegrationService} that installs persistent bundles on framework startup.
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Apr-2012
 */
class PersistentBundlesIntegration extends BootstrapBundlesInstall<Void> {

    private final InitialDeploymentTracker deploymentTracker;

    PersistentBundlesIntegration(InitialDeploymentTracker deploymentTracker) {
        super(IntegrationServices.PERSISTENT_BUNDLES);
        this.deploymentTracker = deploymentTracker;
    }

    @Override
    protected void addServiceDependencies(ServiceBuilder<Void> builder) {
        super.addServiceDependencies(builder);
        builder.addDependency(InitialDeploymentTracker.INITIAL_DEPLOYMENTS_COMPLETE);
        builder.addDependency(ModuleRegistrationTracker.MODULE_REGISTRATION_COMPLETE);
        builder.addDependency(IntegrationServices.BOOTSTRAP_BUNDLES_COMPLETE);
    }

    @Override
    public void start(StartContext context) throws StartException {

        // Get the set of persistent bundle deployments from {@link InitialDeploymentTracker}
        Set<Deployment> deployments = deploymentTracker.getDeployments();

        // Collect the Bundle.INSTALLED services
        Set<ServiceName> installedServices = new HashSet<ServiceName>();
        for (Deployment dep : deployments) {
            ServiceName serviceName = dep.getAttachment(ServiceName.class);
            installedServices.add(serviceName);
        }

        // Install the persistent bundle resolve service
        installResolveService(context.getChildTarget(), installedServices);
    }
}
