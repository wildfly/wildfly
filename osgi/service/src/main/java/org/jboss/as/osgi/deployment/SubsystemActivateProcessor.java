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

import static org.jboss.osgi.framework.IntegrationServices.BOOTSTRAP_BUNDLES_ACTIVE;
import static org.jboss.osgi.framework.IntegrationServices.BOOTSTRAP_BUNDLES_INSTALLED;
import static org.jboss.osgi.framework.IntegrationServices.BOOTSTRAP_BUNDLES_RESOLVED;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.internal.FrameworkActive;

/**
 * Activates the OSGi subsystem if an OSGi deployment is detected.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Jun-2012
 */
public class SubsystemActivateProcessor implements DeploymentUnitProcessor {

    private AtomicBoolean subsystemActivated = new AtomicBoolean();

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        // Always make the system context & the environment available
        phaseContext.addDeploymentDependency(Services.SYSTEM_CONTEXT, OSGiConstants.SYSTEM_CONTEXT_KEY);
        phaseContext.addDeploymentDependency(Services.ENVIRONMENT, OSGiConstants.ENVIRONMENT_KEY);

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        if (depUnit.hasAttachment(OSGiConstants.DEPLOYMENT_KEY)) {
            if (subsystemActivated.compareAndSet(false, true)) {
                SubsystemActivationService.addService(phaseContext.getServiceTarget());
            }
            phaseContext.addDependency(SubsystemActivationService.SUBSYTEM_ACTIVATOR_NAME, AttachmentKey.create(Void.class));
            phaseContext.addDeploymentDependency(Services.BUNDLE_MANAGER, OSGiConstants.BUNDLE_MANAGER_KEY);
            phaseContext.addDeploymentDependency(Services.RESOLVER, OSGiConstants.RESOLVER_KEY);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }

    /**
     * {@link FrameworkActive} has a dependency on the bootstrap & persistent bundles being activated.
     * The persistent bundles pass through this DUP on server startup - so we cannot simply create
     * a phase dependency on {@link FrameworkActive}.
     *
     * Instead we create a dependency on this service, which has dependencies the boostrap bundle services,
     * which allow the persistent bundle services to get started. Additionally we explicitly set the mode
     * for {@link FrameworkActive} to ACTIVE so it does not go down again when it has no more dependees.
     */
    static class SubsystemActivationService extends AbstractService<Void> {

        static ServiceName SUBSYTEM_ACTIVATOR_NAME = OSGiConstants.SERVICE_BASE_NAME.append("subsystem", "activator");

        static void addService(ServiceTarget serviceTarget) {
            SubsystemActivationService service = new SubsystemActivationService();
            ServiceBuilder<Void> builder = serviceTarget.addService(SUBSYTEM_ACTIVATOR_NAME, service);
            builder.addDependencies(BOOTSTRAP_BUNDLES_INSTALLED, BOOTSTRAP_BUNDLES_RESOLVED, BOOTSTRAP_BUNDLES_ACTIVE);
            builder.install();
        }

        @Override
        public void start(StartContext context) throws StartException {
            ServiceRegistry serviceRegistry = context.getController().getServiceContainer();
            serviceRegistry.getRequiredService(Services.FRAMEWORK_ACTIVE).setMode(Mode.ACTIVE);
        }
    }
}
