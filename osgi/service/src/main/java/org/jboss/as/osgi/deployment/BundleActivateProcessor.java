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

import static org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION;
import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.osgi.framework.spi.IntegrationConstants.BUNDLE_ACTIVATOR_KEY;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.Attachments.BundleState;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;

/**
 * Attempt to activate the OSGi deployment.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Jun-2012
 */
public class BundleActivateProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        XBundleRevision brev = depUnit.getAttachment(OSGiConstants.BUNDLE_REVISION_KEY);
        if (brev == null || brev.isFragment())
            return;

        ServiceController<XBundle> controller = BundleActivateService.addService(phaseContext.getServiceTarget(), depUnit, brev.getBundle());
        phaseContext.addDependency(controller.getName(), AttachmentKey.create(XBundle.class));
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }

    static class BundleActivateService implements Service<XBundle> {

        private final InjectedValue<Component> injectedComponent = new InjectedValue<Component>();
        private final DeploymentUnit depUnit;
        private final XBundle bundle;

        static ServiceController<XBundle> addService(ServiceTarget serviceTarget, DeploymentUnit depUnit, XBundle bundle) {
            ServiceName serviceName = depUnit.getServiceName().append("Activate");
            BundleActivateService service = new BundleActivateService(depUnit, bundle);
            ServiceBuilder<XBundle> builder = serviceTarget.addService(serviceName, service);
            // Add a dependency on the BundleActivator component
            OSGiMetaData metadata = depUnit.getAttachment(OSGiConstants.OSGI_METADATA_KEY);
            if (metadata != null && metadata.getBundleActivator() != null) {
                String activatorClass = metadata.getBundleActivator();
                EEModuleDescription moduleDescription = depUnit.getAttachment(EE_MODULE_DESCRIPTION);
                if (moduleDescription != null) {
                    for (ComponentDescription componentDescription : moduleDescription.getComponentDescriptions()) {
                        if (activatorClass.equals(componentDescription.getComponentClassName())) {
                            ServiceName startServiceName = componentDescription.getStartServiceName();
                            builder.addDependency(startServiceName, Component.class, service.injectedComponent);
                        }
                    }
                }
            }
            // Add other named dependencies
            builder.addDependencies(depUnit.getAttachmentList(Attachments.BUNDLE_ACTIVE_DEPENDENCIES));
            return builder.install();
        }

        private BundleActivateService(DeploymentUnit depUnit, XBundle bundle) {
            this.depUnit = depUnit;
            this.bundle = bundle;
        }

        @Override
        public void start(StartContext context) throws StartException {
            Deployment deployment = depUnit.getAttachment(OSGiConstants.DEPLOYMENT_KEY);
            BundleManager bundleManager = depUnit.getAttachment(OSGiConstants.BUNDLE_MANAGER_KEY);
            Component activatorComponent = injectedComponent.getOptionalValue();
            if (activatorComponent != null && deployment.getAttachment(BUNDLE_ACTIVATOR_KEY) == null) {
                ComponentInstance componentInstance = activatorComponent.createInstance();
                BundleActivator instance = (BundleActivator) componentInstance.getInstance();
                deployment.putAttachment(BUNDLE_ACTIVATOR_KEY, instance);
            }
            if (bundle.getState() != Bundle.ACTIVE) {
                try {
                    bundleManager.startBundle(bundle, Bundle.START_ACTIVATION_POLICY);
                    depUnit.putAttachment(Attachments.BUNDLE_STATE_KEY, BundleState.ACTIVE);
                } catch (BundleException ex) {
                    throw MESSAGES.cannotStartBundle(ex, bundle);
                }
            }
        }

        @Override
        public void stop(StopContext context) {
            if (bundle.getState() == Bundle.STARTING || bundle.getState() == Bundle.ACTIVE) {
                try {
                    BundleManager bundleManager = depUnit.getAttachment(OSGiConstants.BUNDLE_MANAGER_KEY);
                    bundleManager.stopBundle(bundle, Bundle.STOP_TRANSIENT);
                    depUnit.putAttachment(Attachments.BUNDLE_STATE_KEY, BundleState.RESOLVED);
                } catch (BundleException ex) {
                    LOGGER.debugf(ex, "Cannot stop bundle: %s", bundle);
                }
            }
        }

        @Override
        public XBundle getValue() {
            return bundle;
        }
    }
}
