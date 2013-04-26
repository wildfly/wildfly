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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentManager;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.deployment.BundleDeploymentProcessor;
import org.jboss.as.osgi.management.OperationAssociation;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.Phase;
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
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.FrameworkMessages;
import org.jboss.osgi.framework.Services;
import org.jboss.osgi.framework.spi.BundleLifecycle;
import org.jboss.osgi.framework.spi.BundleLifecyclePlugin;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.framework.spi.DeploymentProvider;
import org.jboss.osgi.framework.spi.FutureServiceValue;
import org.jboss.osgi.framework.spi.IntegrationConstants;
import org.jboss.osgi.framework.spi.IntegrationServices;
import org.jboss.osgi.framework.spi.LockManager;
import org.jboss.osgi.framework.spi.StorageManager;
import org.jboss.osgi.resolver.XBundle;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResolveContext;
import org.jboss.osgi.resolver.XResolver;
import org.jboss.osgi.resolver.XResource;
import org.jboss.osgi.spi.AttachmentKey;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.vfs.VFSUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.service.resolver.ResolutionException;

/**
 * An {@link org.jboss.osgi.framework.spi.IntegrationService} that that handles the bundle lifecycle.
 *
 * @author thomas.diesler@jboss.com
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @since 24-Nov-2010
 */
public final class BundleLifecycleIntegration extends BundleLifecyclePlugin {

    private static final AttachmentKey<String> RUNTIME_NAME_KEY = AttachmentKey.create(String.class);
    private static final AttachmentKey<Boolean> BUNDLE_REFRESHING_KEY = AttachmentKey.create(Boolean.class);

    private static final Map<String, Deployment> deploymentMap = new HashMap<String, Deployment>();

    private final InjectedValue<ModelController> injectedController = new InjectedValue<ModelController>();
    private final InjectedValue<BundleManager> injectedBundleManager = new InjectedValue<BundleManager>();
    private final InjectedValue<XEnvironment> injectedEnvironment = new InjectedValue<XEnvironment>();
    private final InjectedValue<XResolver> injectedResolver = new InjectedValue<XResolver>();
    private final InjectedValue<StorageManager> injectedStorageManager = new InjectedValue<StorageManager>();
    private final InjectedValue<DeploymentProvider> injectedDeploymentManager = new InjectedValue<DeploymentProvider>();
    private final InjectedValue<LockManager> injectedLockManager = new InjectedValue<LockManager>();
    private ServerDeploymentManager serverDeploymentManager;
    private ModelControllerClient modelControllerClient;

    @Override
    protected void addServiceDependencies(ServiceBuilder<BundleLifecycle> builder) {
        super.addServiceDependencies(builder);
        builder.addDependency(JBOSS_SERVER_CONTROLLER, ModelController.class, injectedController);
        builder.addDependency(IntegrationServices.STORAGE_MANAGER_PLUGIN, StorageManager.class, injectedStorageManager);
        builder.addDependency(IntegrationServices.DEPLOYMENT_PROVIDER_PLUGIN, DeploymentProvider.class, injectedDeploymentManager);
        builder.addDependency(IntegrationServices.LOCK_MANAGER_PLUGIN, LockManager.class, injectedLockManager);
        builder.addDependency(Services.BUNDLE_MANAGER, BundleManager.class, injectedBundleManager);
        builder.addDependency(Services.ENVIRONMENT, XEnvironment.class, injectedEnvironment);
        builder.addDependency(Services.RESOLVER, XResolver.class, injectedResolver);
        builder.addDependency(Services.FRAMEWORK_CREATE);
        builder.setInitialMode(Mode.ON_DEMAND);
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        super.start(startContext);
        ModelController modelController = injectedController.getValue();
        modelControllerClient = modelController.createClient(Executors.newCachedThreadPool());
        serverDeploymentManager =  ServerDeploymentManager.Factory.create(modelControllerClient);
    }

    @Override
    public void stop(StopContext context) {
        try {
            modelControllerClient.close();
        } catch (IOException ex) {
            // ignore
        }
        super.stop(context);
    }

    @Override
    protected BundleLifecycle createServiceValue(StartContext startContext) throws StartException {
        return new BundleLifecycleImpl();
    }

    public static Deployment getDeployment(String runtimeName) {
        synchronized (deploymentMap) {
            return deploymentMap.get(runtimeName);
        }
    }

    public static Deployment removeDeployment(String runtimeName) {
        synchronized (deploymentMap) {
            return deploymentMap.remove(runtimeName);
        }
    }

    private void putDeployment(String runtimeName, final Deployment dep) {
        synchronized (deploymentMap) {
            deploymentMap.put(runtimeName, dep);
        }
    }

    public static boolean isBundleRefreshing(XBundle bundle) {
        return Boolean.TRUE.equals(bundle.getAttachment(BUNDLE_REFRESHING_KEY));
    }

    class BundleLifecycleImpl implements BundleLifecycle {

        private final BundleManager bundleManager;
        private final XEnvironment environment;
        private final XResolver resolver;

        BundleLifecycleImpl() {
            this.bundleManager = injectedBundleManager.getValue();
            this.environment = injectedEnvironment.getValue();
            this.resolver = injectedResolver.getValue();
        }

        @Override
        public XBundleRevision createBundleRevision(BundleContext context, Deployment deployment) throws BundleException {

            // Do the install directly if we have a running management op
            // https://issues.jboss.org/browse/AS7-5642
            if (OperationAssociation.INSTANCE.getAssociation() != null) {
                LOGGER.warnCannotDeployBundleFromManagementOperation(deployment);
                return bundleManager.createBundleRevision(context, deployment, null);
            }

            // Check if a bundle with that location already exists
            if (!deployment.isBundleUpdate() && bundleManager.getBundleByLocation(deployment.getLocation()) != null) {
                return bundleManager.createBundleRevision(context, deployment, null);
            }

            LOGGER.debugf("Install deployment: %s", deployment);
            String runtimeName = getRuntimeName(deployment);
            putDeployment(runtimeName, deployment);
            try {
                InputStream input = deployment.getRoot().openStream();
                try {
                    ServerDeploymentHelper server = new ServerDeploymentHelper(serverDeploymentManager);
                    server.deploy(runtimeName, input);
                } finally {
                    VFSUtils.safeClose(input);
                }
            } catch (RuntimeException rte) {
                throw rte;
            } catch (Exception ex) {
                throw MESSAGES.cannotDeployBundleRevision(ex, deployment);
            }

            // For an already existing bundle (i.e. same location) the
            // {@link Deployment} may not have the bundle revision attached
            DeploymentUnit depUnit = deployment.getAttachment(BundleDeploymentProcessor.DEPLOYMENT_UNIT_KEY);
            XBundleRevision brev = depUnit.getAttachment(OSGiConstants.BUNDLE_REVISION_KEY);
            return brev;
        }

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public void resolve(XBundle bundle) throws ResolutionException {

            // Resolve the bundle
            bundleManager.resolveBundle(bundle);

            // In case there is no DeploymentUnit there is a possiblity
            // of a race between the first class load and the availability of the Module
            // Here we wait for the ModuleSpec service to come up before we are done resolving
            // https://issues.jboss.org/browse/AS7-6016
            Deployment deployment = bundle.adapt(Deployment.class);
            DeploymentUnit depUnit = deployment.getAttachment(BundleDeploymentProcessor.DEPLOYMENT_UNIT_KEY);
            if (depUnit == null) {
                ModuleIdentifier identifier = bundle.getBundleRevision().getModuleIdentifier();
                ServiceName moduleServiceName = ServiceModuleLoader.moduleServiceName(identifier);
                ServiceRegistry serviceRegistry = bundleManager.getServiceContainer();
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
        @SuppressWarnings("unchecked")
        public void start(XBundle bundle, int options) throws BundleException {
            Deployment deployment = bundle.adapt(Deployment.class);
            DeploymentUnit depUnit = deployment.getAttachment(BundleDeploymentProcessor.DEPLOYMENT_UNIT_KEY);

            // The DeploymentUnit would be null for initial capabilities
            // or for bundles that have been installed in nested mgmnt ops
            // https://issues.jboss.org/browse/AS7-5642
            if (depUnit == null) {
                bundleManager.startBundle(bundle, options);
                return;
            }

            // There is no deferred phase, activate using the default
            List<String> deferredModules = DeploymentUtils.getDeferredModules(depUnit);
            if (!deferredModules.contains(depUnit.getName())) {
                bundleManager.startBundle(bundle, options);
                return;
            }

            // Get the INSTALL phase service and check whether we need to activate it
            ServiceController<Phase> phaseService = getDeferredPhaseService(depUnit);
            if (phaseService.getMode() != Mode.NEVER) {
                bundleManager.startBundle(bundle, options);
                return;
            }

            final ServiceName deploymentServiceName;
            if (depUnit.getParent() == null) {
                deploymentServiceName = depUnit.getServiceName();
            } else {
                deploymentServiceName = depUnit.getParent().getServiceName();
            }

            ServiceContainer serviceContainer = bundleManager.getServiceContainer();
            ServiceController<DeploymentUnit> deploymentService = (ServiceController<DeploymentUnit>) serviceContainer.getRequiredService(deploymentServiceName);
            activateDeferredPhase(bundle, options, depUnit, phaseService, deploymentService);
        }

        @Override
        public void stop(XBundle bundle, int options) throws BundleException {
            bundleManager.stopBundle(bundle, options);
        }

        @Override
        public void removeRevision(XBundleRevision brev, int options) {

            undeployRevision(brev);

            // [AS7-6855] Bundle revision not removed when not in use any more
            if (brev.getState() != XResource.State.UNINSTALLED) {
                bundleManager.removeRevision(brev, options);
            }
        }

        private void undeployRevision(XBundleRevision brev) {
            ServerDeploymentHelper server = new ServerDeploymentHelper(serverDeploymentManager);
            try {
                Deployment deployment = brev.getAttachment(IntegrationConstants.DEPLOYMENT_KEY);
                server.undeploy(getRuntimeName(deployment));
            } catch (Exception ex) {
                LOGGER.warnCannotUndeployBundleRevision(ex, brev);
            }
        }

        @Override
        public BundleRefreshPolicy getBundleRefreshPolicy() {
            return new RecreateCurrentRevisionPolicy();
        }

        private void activateDeferredPhase(XBundle bundle, int options, DeploymentUnit depUnit, ServiceController<Phase> phaseService,
                ServiceController<DeploymentUnit> parentDeploymentService) throws BundleException {

            // If the Framework's current start level is less than this bundle's start level
            FrameworkStartLevel frameworkStartLevel = bundleManager.getSystemBundle().adapt(FrameworkStartLevel.class);
            BundleStartLevel bundleStartLevel = bundle.adapt(BundleStartLevel.class);
            int startlevel = bundleStartLevel.getStartLevel();
            if (startlevel > frameworkStartLevel.getStartLevel()) {
                LOGGER.debugf("Start level [%d] not valid for: %s", startlevel, bundle);
                return;
            }

            LOGGER.infoActivateDeferredModulePhase(bundle);

            if (!bundle.isResolved()) {
                XResolveContext context = resolver.createResolveContext(environment, Collections.singleton(bundle.getBundleRevision()), null);
                try {
                    resolver.resolveAndApply(context);
                } catch (ResolutionException ex) {
                    throw new BundleException(FrameworkMessages.MESSAGES.cannotResolveBundle(bundle), BundleException.RESOLVE_ERROR, ex);
                }
            }

            depUnit.getAttachment(Attachments.DEFERRED_ACTIVATION_COUNT).incrementAndGet();

            StabilityMonitor monitor = new StabilityMonitor();
            monitor.addController(parentDeploymentService);
            monitor.addController(phaseService);
            Set<ServiceController<?>> failed = new HashSet<ServiceController<?>>();
            Set<ServiceController<?>> problems = new HashSet<ServiceController<?>>();
            try {
                phaseService.setMode(Mode.ACTIVE);
                monitor.awaitStability(failed, problems);
            } catch (final InterruptedException ex) {
                // ignore
            } finally {
                monitor.clear();
            }

            // In case of failure we go back to NEVER

            if (failed.size() > 0 || problems.size() > 0) {
                List<ServiceController<?>> combined = new ArrayList<ServiceController<?>>();
                combined.addAll(failed);
                combined.addAll(problems);

                // Collect the first start exception
                StartException startex = null;
                for (ServiceController<?> aux : combined) {
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
                depUnit.putAttachment(OSGiConstants.DEFERRED_ACTIVATION_FAILED, Boolean.TRUE);
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

        // Maps the bundle.location to a deployment runtime name
        private String getRuntimeName(Deployment deployment) {
            String runtimeName = deployment.getAttachment(RUNTIME_NAME_KEY);
            if (runtimeName == null) {
                runtimeName = deployment.getLocation();
                try {
                    // Strip the query off the location if it is a valid URI
                    new URI(runtimeName);
                    int queryIndex = runtimeName.indexOf('?');
                    if (queryIndex > 0) {
                        runtimeName = runtimeName.substring(0, queryIndex);
                    }
                } catch (URISyntaxException ex) {
                    // ignore
                }
                if (deployment.isBundleUpdate()) {
                    String suffix = "";
                    XBundle bundle = deployment.getAttachment(IntegrationConstants.BUNDLE_KEY);
                    BundleRevisions brevs = bundle.adapt(BundleRevisions.class);
                    int revid = brevs.getRevisions().size();
                    int dotindex = runtimeName.length() - 4;
                    if (dotindex > 0 && runtimeName.charAt(dotindex) == '.') {
                        suffix = runtimeName.substring(runtimeName.length() - 4);
                        runtimeName = runtimeName.substring(0, dotindex);
                    }
                    runtimeName += "-rev" + revid + suffix;
                }
                deployment.putAttachment(RUNTIME_NAME_KEY, runtimeName);
            }
            return runtimeName;
        }

        @SuppressWarnings("unchecked")
        private ServiceController<Phase> getDeferredPhaseService(DeploymentUnit depUnit) {
            ServiceName serviceName = DeploymentUtils.getDeploymentUnitPhaseServiceName(depUnit, Phase.FIRST_MODULE_USE);
            ServiceContainer serviceContainer = bundleManager.getServiceContainer();
            return (ServiceController<Phase>) serviceContainer.getRequiredService(serviceName);
        }

        private final class RecreateCurrentRevisionPolicy implements BundleRefreshPolicy {
            private XBundle bundle;
            private VirtualFile rootFile;

            @Override
            public void startBundleRefresh(XBundle bundle) throws BundleException {
                this.bundle = bundle;

                XBundleRevision brev = bundle.getBundleRevision();
                Deployment deployment = bundle.adapt(Deployment.class);

                try {
                    InputStream inputStream = deployment.getRoot().getStreamURL().openStream();
                    rootFile = AbstractVFS.toVirtualFile(inputStream);
                } catch (IOException ex) {
                    throw FrameworkMessages.MESSAGES.cannotObtainVirtualFile(ex);
                }

                bundle.putAttachment(BUNDLE_REFRESHING_KEY, Boolean.TRUE);
                undeployRevision(brev);
            }

            @Override
            public void refreshCurrentRevision(XBundleRevision brev) throws BundleException {

                // Create the revision {@link Deployment}
                DeploymentProvider deploymentManager = injectedDeploymentManager.getValue();
                Deployment deployment = deploymentManager.createDeployment(bundle.getLocation(), rootFile);
                deployment.putAttachment(IntegrationConstants.BUNDLE_KEY, bundle);
                deployment.setAutoStart(false);

                String runtimeName = getRuntimeName(deployment);
                putDeployment(runtimeName, deployment);

                try {
                    InputStream input = deployment.getRoot().openStream();
                    try {
                        ServerDeploymentHelper server = new ServerDeploymentHelper(serverDeploymentManager);
                        server.deploy(runtimeName, input);
                    } finally {
                        VFSUtils.safeClose(input);
                    }
                } catch (RuntimeException rte) {
                    throw rte;
                } catch (Exception ex) {
                    throw MESSAGES.cannotDeployBundleRevision(ex, deployment);
                }
            }

            @Override
            public void endBundleRefresh(XBundle bundle) {
                bundle.removeAttachment(BUNDLE_REFRESHING_KEY);
            }
        }
    }
}
