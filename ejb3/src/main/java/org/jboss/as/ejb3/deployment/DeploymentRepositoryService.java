/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.ejb3.remote.ModuleAvailabilityRegistrar;
import org.jboss.as.ejb3.remote.ModuleAvailabilityRegistrarListener;
import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.as.server.suspend.SuspensionStateProvider;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationEvent;
import org.wildfly.clustering.server.provider.ServiceProviderRegistryListener;
import org.wildfly.clustering.server.service.Service;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Repository for information about deployed modules. This includes information on all the deployed Jakarta Enterprise Beans's in the module
 *
 * @author Stuart Douglas
 * @author Richard Achmatowicz
 */
public class DeploymentRepositoryService implements DeploymentRepository, ModuleAvailabilityRegistrar, Service {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ee", "deploymentRepository");

    private final ServiceDependency<SuspendableActivityRegistry> activityRegistryDependency;
    private final ServiceDependency<ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember>> serviceRegistrarDependency;

    private SuspendableActivityRegistry activityRegistry;
    private SuspendableActivity activity;
    private ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember> serviceRegistrar;
    private AtomicReference<Registration> globalServiceProviderRegistration = new AtomicReference<>(null);

    private final List<ModuleAvailabilityRegistrarListener> listeners = new ArrayList<ModuleAvailabilityRegistrarListener>();

    /**
     * All deployed modules. This is a copy on write map that is updated infrequently and read often.
     */
    protected volatile Map<EJBModuleIdentifier, DeploymentHolder> modules;
    private boolean started;
    // servers start out suspended; the actual value is established from the activity registry on start()
    private volatile boolean suspended = true;

    public DeploymentRepositoryService(ServiceDependency<SuspendableActivityRegistry> activityRegistryDependency, ServiceDependency<ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember>> serviceRegistrarDependency) {
        this.activityRegistryDependency = activityRegistryDependency;
        this.serviceRegistrarDependency = serviceRegistrarDependency;
        this.activity = new ModuleAvailabilityRegistrarSuspendableActivity();
    }

    @Override
    public void start()  {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("Starting DeploymentRepositoryService");

        // inject the dependencies
        this.activityRegistry = this.activityRegistryDependency.get();
        this.serviceRegistrar = this.serviceRegistrarDependency.get();

        // register as a ServerActivity
        activityRegistry.registerActivity(this.activity);

        // This service is on-demand, so it may be (re)started at any point in the server lifecycle, including after the
        // server has already resumed. In that case our activity will never see a resume() callback, so seed the
        // suspension state from the registry rather than assuming the server is still suspended.
        this.suspended = activityRegistry.getState() != SuspensionStateProvider.State.RUNNING;
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.tracef("DeploymentRepositoryService starting with suspended = %s", this.suspended);

        // initialize the map of module identifiers to modules
        modules = Collections.emptyMap();

        // register a global ServiceProviderRegistryListener to receive ServiceProviderRegistry updates
        ModuleAvailabilityRegistrarServiceProviderRegistryListener listener = new ModuleAvailabilityRegistrarServiceProviderRegistryListener(listeners);
        Registration globalRegistration = serviceRegistrar.register(listener);
        globalServiceProviderRegistration.compareAndSet(null, globalRegistration);

        // mark this service as started
        started = true;
    }

    @Override
    public void stop() {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.trace("Stopping DeploymentRepositoryService");

        // deregister the global ServiceProviderRegistryListener
        Registration globalRegistration = globalServiceProviderRegistration.get();
        if (globalRegistration != null) {
            globalRegistration.close();
        }

        // unregister as a server activity
        activityRegistry.unregisterActivity(this.activity);

        modules = null;

        this.activityRegistry = null;
        this.serviceRegistrar = null;

        // mark this service as stopped
        started = false;
    }

    // check started status
    public boolean isStarted() {
        return started;
    }

    // check suspended status
    public boolean isSuspended() {
        return suspended;
    }

    // DeploymentRepository interface

    /**
     * Adds a deployment and its metadata to the deployment repository holding information on deployed modules.
     * Information on deployments is held locally and globally:
     * - a server-local map maps moduleId to DeploymentHolder
     * - a cluster-wide service provider registry holds information on which modules are deployed on which servers in the cluster
     *
     * @param moduleId the moduleId of the newly deployed module
     * @param deployment the metadata for the newly deployed module
     */
    @Override
    public void add(EJBModuleIdentifier moduleId, ModuleDeployment deployment) {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.tracef("Adding moduleId %s to DeploymentRepository(suspended = %s)", moduleId, isSuspended());

        synchronized (this) {
            final Map<EJBModuleIdentifier, DeploymentHolder> modules = new HashMap<EJBModuleIdentifier, DeploymentHolder>(this.modules);
            AtomicReference<ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>> registrationReference = new AtomicReference<>();

            // do *not* register modules which are not yet confirmed as started

            // update the local map of deployments
            modules.put(moduleId, new DeploymentHolder(deployment, registrationReference));
            this.modules = Collections.unmodifiableMap(modules);
        }
    }

    /**
     * Updates the metadata in the deployment repository holding information on deployed modules to reflect the fact that
     * they have started.
     * Information on deployments is held locally and globally:
     * - a server-local map maps moduleId to DeploymentHolder
     * - a cluster-wide service provider registry holds information on which modules are deployed on which servers in the cluster
     *
     * This method will always mark the deployment locally as started.
     * It will only register the module's availability if the local server is not suspended.
     *
     * @param moduleId the moduleId of the newly started module
     * @return true of the deployment has started
     */
    @Override
    public boolean startDeployment(EJBModuleIdentifier moduleId) {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.tracef("Starting moduleId %s in DeploymentRepository (suspended = %s)", moduleId, isSuspended());

        DeploymentHolder deployment;
        synchronized (this) {
            deployment = modules.get(moduleId);
            if (deployment == null) return false;

            // if the server is not suspended, we need to register this started module
            AtomicReference<ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>> registrationReference =
                    deployment.registrationReference;

            // validate that the module is not currently registered
            ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> currentRegistration = registrationReference.get();
            assert currentRegistration == null : "a non-started module has been registered - not supposed to happen!";

            if (!isSuspended()) {
                if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                    EjbLogger.DEPLOYMENT_LOGGER.tracef("Adding registration for moduleId %s to ServiceProviderRegistrar", moduleId);

                // register the moduleId with the ServiceProviderRegistrar
                ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = serviceRegistrar.register(moduleId);
                registrationReference.set(registration);
            } else {
                if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                    EjbLogger.DEPLOYMENT_LOGGER.tracef("Skipping registration of moduleId %s to ServiceProviderRegistrar: server is suspended", moduleId);
            }
            deployment.started = true;
        }
        return true;
    }

    /**
     * Removes a deployment and its metadata from the deployment repository holding information on deployed modules.
     * Information on deployments is held locally and globally:
     * - a server-local map maps moduleId to DeploymentHolder
     * - a cluster-wide service provider registry holds information on which modules are deployed on which servers in the cluster
     *
     * @param moduleId the moduleId of the newly deployed module
     */
    @Override
    public void remove(EJBModuleIdentifier moduleId) {
        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
            EjbLogger.DEPLOYMENT_LOGGER.tracef("Removing moduleId %s from DeploymentRepository (suspended= %s)", moduleId, isSuspended());

        synchronized (this) {
            final Map<EJBModuleIdentifier, DeploymentHolder> modules = new HashMap<EJBModuleIdentifier, DeploymentHolder>(this.modules);

            // remove the DeploymentHolder of the undeployed module from the map
            DeploymentHolder deploymentHolder = modules.remove(moduleId);
            this.modules = Collections.unmodifiableMap(modules);

            if (!isSuspended()) {
                // close the registration of the undeployed module in the service provider registry
                // the registration will already be closed if the server is currently suspended
                ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = deploymentHolder.registrationReference.getAndSet(null);
                if (registration != null) {
                    if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                        EjbLogger.DEPLOYMENT_LOGGER.tracef("Removing registration for moduleId %s from ServiceProviderRegistrar", moduleId);
                    registration.close();
                }
            } else {
                if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                    EjbLogger.DEPLOYMENT_LOGGER.tracef("Skipping de-registration of moduleId %s from ServiceProviderRegistrar: server is suspended", moduleId);
            }
        }
    }

    // ModuleAvailabilityRegistrar interface

    /**
     * Return the set of modules currently deployed in the cluster.
     *
     * @return a set of EJBModuleIdentifier instances representing each module
     */
    @Override
    public Set<EJBModuleIdentifier> getServices() {
       Set<EJBModuleIdentifier> services = new HashSet<>();
        for (EJBModuleIdentifier service : serviceRegistrar.getServices()) {
            services.add(service);
        }
        return services;
    }

    /**
     * Return the set providers (nodes) on which this module is deployed.
     * @param service the deployment identifier
     * @return the ser of providers
     */
    @Override
    public Set<GroupMember> getProviders(EJBModuleIdentifier service) {
        return serviceRegistrar.getProviders(service);
    }

    // ModuleAvailabilityRegistrarListener interface

    @Override
    public void addListener(final ModuleAvailabilityRegistrarListener listener) {
        synchronized (this) {
            listeners.add(listener);
        }
        listener.listenerAdded(this);
    }

    @Override
    public synchronized void removeListener(final ModuleAvailabilityRegistrarListener listener) {
        listeners.remove(listener);
    }

    // DeploymentRepository interface

    @Override
    public Map<EJBModuleIdentifier, ModuleDeployment> getModules() {
        Map<EJBModuleIdentifier, ModuleDeployment> modules = new HashMap<EJBModuleIdentifier, ModuleDeployment>();
        for (Map.Entry<EJBModuleIdentifier, DeploymentHolder> entry : this.modules.entrySet()) {
            modules.put(entry.getKey(), entry.getValue().deployment);
        }
        return modules;
    }

    @Override
    public Map<EJBModuleIdentifier, ModuleDeployment> getStartedModules() {
        Map<EJBModuleIdentifier, ModuleDeployment> modules = new HashMap<EJBModuleIdentifier, ModuleDeployment>();
        for (Map.Entry<EJBModuleIdentifier, DeploymentHolder> entry : this.modules.entrySet()) {
            if (entry.getValue().started) {
                modules.put(entry.getKey(), entry.getValue().deployment);
            }
        }
        return modules;
    }

    /*
     * This listener is notified of changes for a particular service that has been registered.
     */
    class ModuleAvailabilityRegistrarServiceProviderRegistryListener implements ServiceProviderRegistryListener<EJBModuleIdentifier, GroupMember> {

        private final List<ModuleAvailabilityRegistrarListener> listeners;

        public ModuleAvailabilityRegistrarServiceProviderRegistryListener(List<ModuleAvailabilityRegistrarListener> listeners) {
            this.listeners = listeners;
        }

        /**
         * Use the information on changes in providers to notify listeners that modules have been added or removed.
         * NOTE: called even when the change in providers is locally initiated.
         *
         * @param event a registration event describing members providing the given service
         */
        @Override
        public void providersChanged(EJBModuleIdentifier moduleId, ServiceProviderRegistrationEvent<GroupMember> event) {
            if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarServiceProviderRegistrationListener: Calling providersChanged() for module %s : previous = %s, current = %s", moduleId, event.getPreviousProviders(), event.getCurrentProviders());

            Set<GroupMember> added = event.getNewProviders();
            Set<GroupMember> removed = event.getObsoleteProviders();

            if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarServiceProviderRegistrationListener: Calling providersChanged() for module %s : added = %s, removed = %s", moduleId, added, removed);

            if (!added.isEmpty()) {
                // some providers were added - create the map
                List<GroupMember> list = new ArrayList<GroupMember>(added);
                Map<EJBModuleIdentifier, List<GroupMember>> map = Map.of(moduleId, list);

                if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                    EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarServiceProviderRegistrationListener: Calling modulesAvailable with map = %s", map);

                // copy the listeners under lock (addListener/removeListener concurrent with providersChanged)
                final List<ModuleAvailabilityRegistrarListener> listeners;
                synchronized (DeploymentRepositoryService.this) {
                    listeners = new ArrayList<>(this.listeners);
                }
                // call the listeners
                for (ModuleAvailabilityRegistrarListener listener : listeners) {
                    listener.modulesAvailable(map);
                }
            }

            if (!removed.isEmpty()) {
                // some providers were removed - create the map
                List<GroupMember> list = new ArrayList<GroupMember>(removed);
                Map<EJBModuleIdentifier, List<GroupMember>> map = Map.of(moduleId, list);

                if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                    EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarServiceProviderRegistrationListener: Calling modulesUnavailable with map = %s", map);

                // copy the listeners under lock (addListener/removeListener concurrent with providersChanged)
                final List<ModuleAvailabilityRegistrarListener> listeners;
                synchronized (DeploymentRepositoryService.this) {
                    listeners = new ArrayList<>(this.listeners);
                }
                // call the listeners
                for (ModuleAvailabilityRegistrarListener listener : listeners) {
                    listener.modulesUnavailable(map);
                }
            }
        }
    }

    /*
     * A SuspendableActivity that controls what happens to deployment registrations when the server is suspended and resumed.
     */
    class ModuleAvailabilityRegistrarSuspendableActivity implements SuspendableActivity {

        /**
         * Prepare the ServiceProviderRegistry for suspension of the server.
         * When the server is suspended:
         * - unregister all registered service providers
         * - for each service provider unregistered, callback clients will be notified that the module is no longer available
         * This also includes the case where the server is being suspended as part of clean shutdown.
         *
         * IMPORTANT NOTE: This activity must happen in the prepare phase, so that moduleunavailability updates are sent to
         * connected EJB clients before the suspend phase, when the EjbSuspendHandlerService will block the completion of suspend
         * to allow active transactions to complete. This prevents the creation of new transactions on a server which is in
         * the process of shutting down and allows the EjbSuspendHandlerService to permit clean transaction shutdown.
         *
         * @param context the server suspend context
         * @return a completion stage for the ServerSuspendController
         */
        @Override
        public CompletionStage<Void> prepare(ServerSuspendContext context) {
            if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarSuspendableActivity: Preparing for suspend(suspended = %s): server suspend context: isStarting = %s, isStopping = %s", isSuspended(), context.isStarting(), context.isStopping());

            if (modules.size() != 0) {
                // unregister the service providers we have registered
                Map<EJBModuleIdentifier, DeploymentHolder> deployedModules = modules;

                if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                    EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarSuspendableActivity: Processing modules");

                for (EJBModuleIdentifier moduleId : deployedModules.keySet()) {
                    if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                        EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarSuspendableActivity: Processing module %s", moduleId);

                    DeploymentHolder holder = deployedModules.get(moduleId);
                    // the registration will be null if the module was undeployed while the server was suspended
                    ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = holder.registrationReference.getAndSet(null);
                    if (registration != null) {
                        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                            EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarSuspendableActivity: Closing registration for module %s", moduleId);
                        registration.close();
                    }
                    if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                        EjbLogger.DEPLOYMENT_LOGGER.trace("ModuleAvailabilityRegistrarSuspendableActivity: Prepared for suspend - with modules");
                }
            }
            if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                EjbLogger.DEPLOYMENT_LOGGER.trace("ModuleAvailabilityRegistrarSuspendableActivity: Prepared for suspend");
            return SuspendableActivity.COMPLETED;
        }

        /**
         * Adjust the ServiceProviderRegistry once suspension of the server cas completed.
         *
         * @param context the server suspend context
         * @return a completion stage for the ServerSuspendController
         */
        @Override
        public CompletionStage<Void> suspend(ServerSuspendContext context) {
            if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarSuspendableActivity: Suspending (suspended = %s): server suspend context: isStarting = %s, isStopping = %s", isSuspended(), context.isStarting(), context.isStopping());
            // available if necessary

            if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                EjbLogger.DEPLOYMENT_LOGGER.trace("ModuleAvailabilityRegistrarSuspendableActivity: Suspended");
            suspended = true;
            return SuspendableActivity.COMPLETED;
         }

        /**
         * Prepare the ServiceProviderRegistry for resuming the server.
         * When the server is resumed:
         * - find out which modules are in the deployment repository
         * - register all deployed modules as service providers
         * - for each service provider registered, callback clients will be notified (automatically) that the module is again available
         * This does not apply when the server is starting as deployments are not possible until ther server is started.
         *
         * @param context the server resume context
         * @return a completion stage for the SuspendController
         */
        @Override
        public CompletionStage<Void> resume(ServerResumeContext context) {
            if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarSuspendableActivity: Resuming (suspended = %s): server resume context: isStarting = %s", isSuspended(), context.isStarting());

            // we are coming out of suspension
            suspended = false;

            // resume should always re-register the deployments - whether starting or post-start
            CompletableFuture<Void> result = new CompletableFuture<>();
            // safe to assume that there are no concurrent accesses during resume
            AtomicInteger count = new AtomicInteger(modules.size());

            // case: only register if modules deployed
            if (count.get() != 0) {
                if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                    EjbLogger.DEPLOYMENT_LOGGER.trace("ModuleAvailabilityRegistrarSuspendableActivity: Processing deployments:");

                // iterate through the locally deployed modules and add registrations to the module availability registrar
                for (EJBModuleIdentifier moduleId : modules.keySet()) {
                    if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                        EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarSuspendableActivity: Processing module %s", moduleId);

                    DeploymentHolder holder = modules.get(moduleId);
                    if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                        EjbLogger.DEPLOYMENT_LOGGER.tracef("ModuleAvailabilityRegistrarSuspendableActivity: Registering listener for module %s", moduleId);

                    CompletableFuture.supplyAsync(() -> serviceRegistrar.register(moduleId))
                            .whenComplete((registration, e) -> {
                                if (e != null) {
                                    result.completeExceptionally(e);
                                } else {
                                    holder.registrationReference.set(registration);
                                    if (count.decrementAndGet() == 0) {
                                        if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                                            EjbLogger.DEPLOYMENT_LOGGER.trace("ModuleAvailabilityRegistrarSuspendableActivity: Resume-completed");
                                        result.complete(null);
                                    }
                                }
                            });
                }
                suspended = false;
                return result;
            }
            if (EjbLogger.DEPLOYMENT_LOGGER.isTraceEnabled())
                EjbLogger.DEPLOYMENT_LOGGER.trace("ModuleAvailabilityRegistrarSuspendableActivity: Resumed");
            return SuspendableActivity.COMPLETED;
        }
    }

    private static final class DeploymentHolder {
        final ModuleDeployment deployment;
        final AtomicReference<ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>> registrationReference;
        volatile boolean started = false;

        private DeploymentHolder(ModuleDeployment deployment, AtomicReference<ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>> registrationReference) {
            this.deployment = deployment;
            this.registrationReference = registrationReference;
        }
    }
}
