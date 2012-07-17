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

import static org.jboss.as.osgi.OSGiConstants.SERVICE_BASE_NAME;
import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;
import static org.jboss.osgi.framework.IntegrationServices.BOOTSTRAP_BUNDLES_COMPLETE;
import static org.jboss.osgi.framework.IntegrationServices.BOOTSTRAP_BUNDLES_INSTALL;
import static org.jboss.osgi.framework.IntegrationServices.PERSISTENT_BUNDLES;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.osgi.framework.BootstrapBundlesResolve;
import org.jboss.osgi.framework.IntegrationServices;
import org.jboss.osgi.framework.IntegrationServices.BootstrapPhase;
import org.jboss.osgi.framework.util.ServiceTracker;
import org.jboss.osgi.resolver.XBundle;

/**
 * A service that provides persistent bundles on framework startup.
 *
 * @author thomas.diesler@jboss.com
 * @since 12-Apr-2012
 */
public class PersistentBundlesIntegration extends AbstractService<Void> {

    static final ServiceName INITIAL_DEPLOYMENTS = SERVICE_BASE_NAME.append("initial", "deployments");
    static final ServiceName INITIAL_DEPLOYMENTS_COMPLETE = BootstrapPhase.serviceName(INITIAL_DEPLOYMENTS, BootstrapPhase.COMPLETE);

    public static ServiceController<Void> addService(ServiceTarget serviceTarget) {
        PersistentBundlesIntegration service = new PersistentBundlesIntegration();
        ServiceName serviceName = BootstrapPhase.serviceName(IntegrationServices.PERSISTENT_BUNDLES, BootstrapPhase.INSTALL);
        ServiceBuilder<Void> builder = serviceTarget.addService(serviceName, service);
        builder.addDependencies(BOOTSTRAP_BUNDLES_INSTALL, BOOTSTRAP_BUNDLES_COMPLETE);
        builder.addDependencies(INITIAL_DEPLOYMENTS_COMPLETE);
        builder.setInitialMode(Mode.ON_DEMAND);
        return builder.install();
    }

    private PersistentBundlesIntegration() {
    }

    public static class InitialDeploymentTracker extends ServiceTracker<Object> {

        private final AtomicBoolean deploymentInstallComplete = new AtomicBoolean(false);
        private final Set<ServiceName> deploymentPhaseServices = new HashSet<ServiceName>();
        private final Set<ServiceName> installedServices = new HashSet<ServiceName>();
        private final ServiceTarget serviceTarget;
        private final Set<String> deploymentNames;

        private ServiceTracker<XBundle> bundleInstallListener;
        private ServiceTarget listenerTarget;

        public InitialDeploymentTracker(OperationContext context, ServiceVerificationHandler verificationHandler) {

            serviceTarget = context.getServiceTarget();
            deploymentNames = getDeploymentNames(context);

            // Get the INSTALL phase service names
            for (String deploymentName : deploymentNames) {
                ServiceName serviceName = Services.deploymentUnitName(deploymentName);
                deploymentPhaseServices.add(serviceName.append(Phase.INSTALL.toString()));
            }

            // Register this tracker with the server controller
            if (!deploymentNames.isEmpty()) {
                ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
                listenerTarget = serviceRegistry.getService(JBOSS_SERVER_CONTROLLER).getServiceContainer();
                listenerTarget.addListener(Inheritance.ALL, this);
            }

            // Setup the bundle install listener
            bundleInstallListener = new ServiceTracker<XBundle>() {

                @Override
                protected boolean allServicesAdded(Set<ServiceName> trackedServices) {
                    return deploymentInstallComplete.get();
                }

                @Override
                protected void complete() {
                    installResolveService(serviceTarget, installedServices);
                }
            };

            // Check the tracker for completeness
            checkAndComplete();
        }

        @Override
        protected boolean trackService(ServiceController<? extends Object> controller) {
            return deploymentPhaseServices.contains(controller.getName());
        }

        @Override
        protected boolean allServicesAdded(Set<ServiceName> trackedServices) {
            return deploymentPhaseServices.size() == trackedServices.size();
        }

        @Override
        protected void complete() {
            LOGGER.tracef("Initial deployments complete");
            if (listenerTarget != null) {
                listenerTarget.removeListener(this);
            }
            deploymentInstallComplete.set(true);
            initialDeploymentsComplete(serviceTarget);
            bundleInstallListener.checkAndComplete();
        }

        public ServiceListener<XBundle> getBundleInstallListener() {
            return bundleInstallListener;
        }

        public boolean isComplete() {
            return deploymentInstallComplete.get();
        }

        public boolean hasDeploymentName(String depname) {
            return deploymentNames.contains(depname);
        }

        public void registerBundleInstallService(ServiceName serviceName) {
            synchronized (installedServices) {
                LOGGER.tracef("Register bundle install service: %s", serviceName);
                installedServices.add(serviceName);
            }
        }

        private Set<String> getDeploymentNames(OperationContext context) {
            final Set<String> result = new HashSet<String>();
            final ModelNode model = Resource.Tools.readModel(context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, true));
            final ModelNode depmodel = model.get(ModelDescriptionConstants.DEPLOYMENT);
            if (depmodel.isDefined()) {
                final List<ModelNode> deploymentNodes = depmodel.asList();
                for (ModelNode node : deploymentNodes) {
                    Property property = node.asProperty();
                    ModelNode enabled = property.getValue().get(ModelDescriptionConstants.ENABLED);
                    if (enabled.isDefined() && enabled.asBoolean()) {
                        result.add(property.getName());
                    }
                }
                LOGGER.tracef("Expecting initial deployments: %s", result);
            }
            return result;
        }

        private ServiceController<Void> installResolveService(ServiceTarget serviceTarget, Set<ServiceName> installedServices) {
            return new BootstrapBundlesResolve<Void>(PERSISTENT_BUNDLES, installedServices).install(serviceTarget);
        }

        private ServiceController<Void> initialDeploymentsComplete(ServiceTarget serviceTarget) {
            return serviceTarget.addService(INITIAL_DEPLOYMENTS_COMPLETE, new AbstractService<Void>() {
            }).install();
        }
    }
}
