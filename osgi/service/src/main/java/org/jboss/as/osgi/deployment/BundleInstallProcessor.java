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

import static org.jboss.as.osgi.OSGiConstants.FRAMEWORK_BASE_NAME;
import static org.jboss.osgi.framework.IntegrationServices.AUTOINSTALL_PROVIDER_COMPLETE;
import static org.jboss.osgi.framework.Services.FRAMEWORK_ACTIVE;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.osgi.service.PersistentBundlesIntegration.InitialDeploymentTracker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.Services;

/**
 * Processes deployments that have OSGi metadata attached.
 *
 * If so, it creates an {@link BundleInstallService}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class BundleInstallProcessor implements DeploymentUnitProcessor {

    private final InitialDeploymentTracker deploymentTracker;
    private final AtomicBoolean frameworkActivated = new AtomicBoolean();

    public BundleInstallProcessor(InitialDeploymentTracker deploymentListener) {
        this.deploymentTracker = deploymentListener;
    }

    @Override
    public void deploy(final DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        final DeploymentUnit depUnit = context.getDeploymentUnit();
        final Deployment deployment = OSGiDeploymentAttachment.getDeployment(depUnit);
        if (deployment != null) {
            if (frameworkActivated.compareAndSet(false, true)) {
                activateFramework(context);
            }
            ServiceName dependency = deploymentTracker.isClosed() ? FRAMEWORK_ACTIVE : AUTOINSTALL_PROVIDER_COMPLETE;
            ServiceName serviceName = BundleInstallService.addService(deploymentTracker, context, deployment, dependency);
            deploymentTracker.registerBundleInstallService(serviceName);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        final Deployment deployment = OSGiDeploymentAttachment.getDeployment(depUnit);
        if (deployment != null) {
            BundleInstallService.removeService(depUnit);
        }
    }

    private void activateFramework(final DeploymentPhaseContext context) {
        Service<Void> service = new AbstractService<Void>() {
            public void start(StartContext context) throws StartException {
                context.getController().setMode(Mode.REMOVE);
            }
        };
        ServiceTarget serviceTarget = context.getServiceTarget();
        ServiceBuilder<Void> builder = serviceTarget.addService(FRAMEWORK_BASE_NAME.append("ACTIVATE"), service);
        builder.addDependency(Services.FRAMEWORK_ACTIVATOR);
        builder.install();
    }
}
