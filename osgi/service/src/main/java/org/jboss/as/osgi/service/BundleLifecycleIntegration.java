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

import static org.jboss.as.osgi.OSGiLogger.LOGGER;
import static org.jboss.as.osgi.OSGiMessages.MESSAGES;
import static org.jboss.as.server.Services.JBOSS_SERVER_CONTROLLER;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.osgi.management.OperationAssociation;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.client.ModelControllerServerDeploymentManager;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.FrameworkMessages;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.spi.BundleLifecycle;
import org.jboss.osgi.framework.spi.BundleLifecyclePlugin;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.framework.spi.FutureServiceValue;
import org.jboss.osgi.framework.spi.IntegrationService;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResolveContext;
import org.jboss.osgi.resolver.XResolver;
import org.jboss.vfs.VFSUtils;
import org.osgi.framework.BundleException;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.startlevel.StartLevel;

/**
 * An {@link IntegrationService} that that handles the bundle lifecycle.
 *
 * @author thomas.diesler@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @since 24-Nov-2010
 */
public final class BundleLifecycleIntegration extends BundleLifecyclePlugin {

    private static Map<String, Deployment> deploymentMap = new HashMap<String, Deployment>();

    private final InjectedValue<ModelController> injectedController = new InjectedValue<ModelController>();
    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();
    private final InjectedValue<StartLevel> injectedStartLevel = new InjectedValue<StartLevel>();
    private final InjectedValue<XResolver> injectedResolver = new InjectedValue<XResolver>();
    private ServerDeploymentManager deploymentManager;

    @Override
    protected void addServiceDependencies(ServiceBuilder<BundleLifecycle> builder) {
        super.addServiceDependencies(builder);
        builder.addDependency(JBOSS_SERVER_CONTROLLER, ModelController.class, injectedController);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, injectedBundleManager);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, injectedEnvironment);
        builder.addDependency(Services.RESOLVER, XResolver.class, injectedResolver);
        builder.addDependency(Services.START_LEVEL, StartLevel.class, injectedStartLevel);
        builder.addDependency(Services.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.ON_DEMAND);
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        super.start(startContext);
        deploymentManager = new ModelControllerServerDeploymentManager(injectedController.getValue());
    }

    @Override
    protected BundleLifecycle createServiceValue(StartContext startContext) throws StartException {
        BundleLifecycle defaultService = super.createServiceValue(startContext);
        return new BundleLifecycleImpl(defaultService);
    }

    public static Deployment getDeployment(String runtimeName) {
        synchronized (deploymentMap) {
            return deploymentMap.get(runtimeName);
        }
    }

    private void putDeployment(String runtimeName, final Deployment dep) {
        synchronized (deploymentMap) {
            deploymentMap.put(runtimeName, dep);
        }
    }

    public static Deployment removeDeployment(String runtimeName) {
        synchronized (deploymentMap) {
            return deploymentMap.remove(runtimeName);
        }
    }

    class BundleLifecycleImpl implements BundleLifecycle {

        private final BundleLifecycle defaultService;

        public BundleLifecycleImpl(BundleLifecycle defaultService) {
            this.defaultService = defaultService;
        }

        @Override
        public void install(Deployment dep) throws BundleException {
            // Do the install directly if we have a running management op
            // https://issues.jboss.org/browse/AS7-5642
            if (OperationAssociation.INSTANCE.getAssociation() != null) {
                LOGGER.warnCannotDeployBundleFromManagementOperation(dep);
                BundleManager bundleManager = injectedBundleManager.getValue();
                bundleManager.installBundle(dep, null, null);
            } else {
                LOGGER.debugf("Install deployment: %s", dep);
                String runtimeName = getRuntimeName(dep);
                putDeployment(runtimeName, dep);
                try {
                    InputStream input = dep.getRoot().openStream();
                    try {
                        ServerDeploymentHelper server = new ServerDeploymentHelper(deploymentManager);
                        server.deploy(runtimeName, input);
                    } finally {
                        VFSUtils.safeClose(input);
                    }
                } catch (RuntimeException rte) {
                    throw rte;
                } catch (Exception ex) {
                    throw MESSAGES.cannotDeployBundle(ex, dep);
                }
            }
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void resolve(XBundle bundle) throws ResolutionException {

            // Resolve the bundle
            defaultService.resolve(bundle);

            // In case there is no DeploymentUnit there is a possiblity
            // of a race between the first class load and the availability of the Module
            // Here we wait for the ModuleSpec service to come up before we are done resolving
            // https://issues.jboss.org/browse/AS7-6016
            Deployment deployment = bundle.adapt(Deployment.class);
            DeploymentUnit depUnit = deployment.getAttachment(DeploymentUnit.class);
            if (depUnit == null) {
                ModuleIdentifier identifier = bundle.getBundleRevision().getModuleIdentifier();
                ServiceName moduleServiceName = ServiceModuleLoader.moduleServiceName(identifier);
                ServiceRegistry serviceRegistry = injectedBundleManager.getValue().getServiceContainer();
                ServiceController<?> controller = serviceRegistry.getRequiredService(moduleServiceName);
                FutureServiceValue<?> future = new FutureServiceValue(controller);
                try {
                    future.get(2, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    throw FrameworkMessages.MESSAGES.illegalStateCannotLoadModule(ex, identifier);
                }
            }
        }

        @Override
        public void start(XBundle bundle, int options) throws BundleException {
            Deployment deployment = bundle.adapt(Deployment.class);
            DeploymentUnit depUnit = deployment.getAttachment(DeploymentUnit.class);

            // The DeploymentUnit would be null for initial capabilities
            // or for bundles that have been installed in nested mgmnt ops
            // https://issues.jboss.org/browse/AS7-5642
            if (depUnit == null) {
                defaultService.start(bundle, options);
                return;
            }

            // There is no deferred phase, activate using the default
            List<String> deferredModules = DeploymentUtils.getDeferredModules(depUnit);
            if (!deferredModules.contains(depUnit.getName())) {
                defaultService.start(bundle, options);
                return;
            }

            // Get the INSTALL phase service and check whether we need to activate it
            ServiceController<Phase> phaseService = getDeferredPhaseService(depUnit);
            if (phaseService.getMode() != Mode.NEVER) {
                defaultService.start(bundle, options);
                return;
            }

            activateDeferredPhase(bundle, options, depUnit, phaseService);
        }

        @Override
        public void stop(XBundle bundle, int options) throws BundleException {
            defaultService.stop(bundle, options);
        }

        @Override
        public void update(XBundle bundle, InputStream input) throws BundleException {
            defaultService.update(bundle, input);
        }

        @Override
        public void uninstall(XBundle bundle, int options) throws BundleException {
            LOGGER.tracef("Uninstall deployment: %s", bundle);
            try {
                ServerDeploymentHelper server = new ServerDeploymentHelper(deploymentManager);
                Deployment dep = bundle.adapt(Deployment.class);
                server.undeploy(getRuntimeName(dep));
            } catch (Exception ex) {
                LOGGER.warnCannotUndeployBundle(ex, bundle);
            }
        }

        private void activateDeferredPhase(XBundle bundle, int options, DeploymentUnit depUnit, ServiceController<Phase> phaseService) throws BundleException {

            // If the Framework's current start level is less than this bundle's start level
            StartLevel startLevel = injectedStartLevel.getValue();
            int bundleStartLevel = startLevel.getBundleStartLevel(bundle);
            if (bundleStartLevel > startLevel.getStartLevel()) {
                LOGGER.debugf("Start level [%d] not valid for: %s", bundleStartLevel, bundle);
                return;
            }

            LOGGER.infoActivateDeferredModulePhase(bundle);

            if (!bundle.isResolved()) {
                XEnvironment env = injectedEnvironment.getValue();
                XResolver resolver = injectedResolver.getValue();
                XResolveContext context = resolver.createResolveContext(env, Collections.singleton(bundle.getBundleRevision()), null);
                try {
                    resolver.resolveAndApply(context);
                } catch (ResolutionException ex) {
                    throw FrameworkMessages.MESSAGES.cannotResolveBundle(ex, bundle);
                }
            }

            depUnit.getAttachment(Attachments.DEFERRED_ACTIVATION_COUNT).incrementAndGet();
            phaseService.setMode(Mode.ACTIVE);
            final StabilityMonitor monitor = new StabilityMonitor();
            monitor.addController(phaseService);
            final Set<ServiceController<?>> failed = new HashSet<ServiceController<?>>();
            try {
                monitor.awaitStability(failed, null);
            } catch (final InterruptedException ex) {
                // ignore
            } finally {
                monitor.removeController(phaseService);
            }

            // In case of failure we go back to NEVER
            if (failed.size() > 0) {

                // Collect the first start exception
                StartException startex = null;
                for (ServiceController<?> aux : failed) {
                    if (aux.getStartException() != null) {
                        startex = aux.getStartException();
                        break;
                    }
                }

                // Create the BundleException that we throw later
                BundleException failure;
                if (startex != null && startex.getCause() instanceof BundleException) {
                    failure = (BundleException) startex.getCause();
                } else {
                    failure = MESSAGES.cannotActivateDeferredModulePhase(startex, bundle);
                }

                // Deactivate the deferred phase
                LOGGER.warnDeactivateDeferredModulePhase(bundle);
                phaseService.setMode(Mode.NEVER);

                // Wait for the phase service to come down
                try {
                    FutureServiceValue<Phase> future = new FutureServiceValue<Phase>(phaseService, State.DOWN);
                    future.get(30, TimeUnit.SECONDS);
                } catch (ExecutionException ex) {
                    LOGGER.errorf(failure, failure.getMessage());
                    throw MESSAGES.cannotDeactivateDeferredModulePhase(ex, bundle);
                } catch (TimeoutException ex) {
                    LOGGER.errorf(failure, failure.getMessage());
                    throw MESSAGES.cannotDeactivateDeferredModulePhase(ex, bundle);
                }

                // Throw the BundleException that caused the start failure
                throw failure;
            }
        }

        /*
         * Maps the bundle.location to a deployment runtime name
         */
        private String getRuntimeName(Deployment dep) {
            // Strip the query off the location if it is a valid URI
            String location = dep.getLocation();
            try {
                new URI(location);
                int queryIndex = location.indexOf('?');
                if (queryIndex > 0) {
                    location = location.substring(0, queryIndex);
                }
            } catch (URISyntaxException ex) {
                // ignore
            }
            return location;
        }

        @SuppressWarnings("unchecked")
        private ServiceController<Phase> getDeferredPhaseService(DeploymentUnit depUnit) {
            ServiceName serviceName = DeploymentUtils.getDeploymentUnitPhaseServiceName(depUnit, Phase.FIRST_MODULE_USE);
            BundleManager bundleManager = injectedBundleManager.getValue();
            ServiceContainer serviceContainer = bundleManager.getServiceContainer();
            return (ServiceController<Phase>) serviceContainer.getRequiredService(serviceName);
        }
    }
}
