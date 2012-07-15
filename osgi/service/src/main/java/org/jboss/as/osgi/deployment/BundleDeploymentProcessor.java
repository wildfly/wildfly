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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.service.BundleInstallIntegration;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.deployment.deployer.DeploymentFactory;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.internal.FrameworkActive;
import org.jboss.osgi.spi.BundleInfo;

/**
 * Processes deployments that have OSGi metadata attached.
 *
 * If so, it creates an {@link Deployment}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class BundleDeploymentProcessor implements DeploymentUnitProcessor {

    private AtomicBoolean subsystemActivated = new AtomicBoolean();

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final String contextName = depUnit.getName();

        // Check if {@link BundleInstallIntegration} provided the {@link Deployment}
        Deployment deployment = BundleInstallIntegration.removeDeployment(contextName);
        if (deployment != null) {
            deployment.setAutoStart(false);
        }

        // Check for attached BundleInfo
        BundleInfo info = depUnit.getAttachment(OSGiConstants.BUNDLE_INFO_KEY);
        if (deployment == null && info != null) {
            deployment = DeploymentFactory.createDeployment(info);
            deployment.addAttachment(BundleInfo.class, info);

            // Prevent autostart of ARQ deployments
            DotName runWithName = DotName.createSimple("org.junit.runner.RunWith");
            CompositeIndex compositeIndex = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
            List<AnnotationInstance> runWithList = compositeIndex.getAnnotations(runWithName);
            deployment.setAutoStart(runWithList.isEmpty());
        }

        // Attach the deployment and activate the framework
        if (deployment != null) {
            if (subsystemActivated.compareAndSet(false, true)) {
                SubsystemActivationService.addService(phaseContext.getServiceTarget());
            }
            phaseContext.addDependency(SubsystemActivationService.SUBSYTEM_ACTIVATOR_NAME, AttachmentKey.create(Void.class));
            phaseContext.addDeploymentDependency(Services.BUNDLE_MANAGER, OSGiConstants.BUNDLE_MANAGER_KEY);
            depUnit.putAttachment(OSGiConstants.DEPLOYMENT_KEY, deployment);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        depUnit.removeAttachment(OSGiConstants.DEPLOYMENT_KEY);
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
