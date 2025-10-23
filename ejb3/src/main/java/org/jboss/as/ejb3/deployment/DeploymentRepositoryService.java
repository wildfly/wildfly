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

import org.jboss.as.ejb3.remote.ModuleAvailabilityRegistrar;
import org.jboss.as.ejb3.remote.ModuleAvailabilityRegistrarListener;
import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationEvent;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationListener;
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

    protected static final Logger log = Logger.getLogger(DeploymentRepositoryService.class.getSimpleName());

    private final ServiceDependency<SuspendableActivityRegistry> activityRegistryDependency;
    private final ServiceDependency<ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember>> serviceRegistrarDependency;

    private SuspendableActivityRegistry activityRegistry;
    private SuspendableActivity activity;
    private ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember> serviceRegistrar;

    private final List<ModuleAvailabilityRegistrarListener> listeners = new ArrayList<ModuleAvailabilityRegistrarListener>();

    /**
     * All deployed modules. This is a copy on write map that is updated infrequently and read often.
     */
    protected volatile Map<EJBModuleIdentifier, DeploymentHolder> modules;
    private boolean started;
    // servers start out suspended
    private boolean suspended = true;

    public DeploymentRepositoryService(ServiceDependency<SuspendableActivityRegistry> activityRegistryDependency, ServiceDependency<ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember>> serviceRegistrarDependency) {
        this.activityRegistryDependency = activityRegistryDependency;
        this.serviceRegistrarDependency = serviceRegistrarDependency;
        this.activity = new ModuleAvailabilityRegistrarSuspendableActivity();
    }

    @Override
    public void start()  {
        log.info("Starting DeploymentRepositoryService");

        // inject the dependencies
        this.activityRegistry = this.activityRegistryDependency.get();
        this.serviceRegistrar = this.serviceRegistrarDependency.get();

        // register as a ServerActivity
        activityRegistry.registerActivity(this.activity);

        // initialize the map of module identifiers to modules
        modules = Collections.emptyMap();

        // mark this service as started
        started = true;
    }

    @Override
    public void stop() {
        log.info("Stopping DeploymentRepositoryService");

        // unregister as a server activity
        activityRegistry.unregisterActivity(this.activity);

        modules = Collections.emptyMap();

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
        log.infof("Adding moduleId %s to DeploymentRepository", moduleId);
        synchronized (this) {
            final Map<EJBModuleIdentifier, DeploymentHolder> modules = new HashMap<EJBModuleIdentifier, DeploymentHolder>(this.modules);
            AtomicReference<ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>> registrationReference = new AtomicReference<>();

            // register the moduleId with the ServiceProviderRegistrar and provide a callback listener to process updates
            ModuleAvailabilityRegistrarServiceProviderRegistrationListener listener =
                    new ModuleAvailabilityRegistrarServiceProviderRegistrationListener(moduleId, listeners);
            ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = serviceRegistrar.register(moduleId, listener);
            registrationReference.set(registration);

            // update the local map of deployments
            modules.put(moduleId, new DeploymentHolder(deployment, registrationReference));
            this.modules = Collections.unmodifiableMap(modules);
        }
    }

    @Override
    public boolean startDeployment(EJBModuleIdentifier moduleId) {
        log.infof("Starting moduleId %s in DeploymentRepository", moduleId);
        DeploymentHolder deployment;
        synchronized (this) {
            deployment = modules.get(moduleId);
            if (deployment == null) return false;
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
        log.infof("Removing moduleId %s from DeploymentRepository", moduleId);
        synchronized (this) {
            final Map<EJBModuleIdentifier, DeploymentHolder> modules = new HashMap<EJBModuleIdentifier, DeploymentHolder>(this.modules);

            // remove the DeeploymentHolder of the undeployed module from the map
            DeploymentHolder deploymentHolder = modules.remove(moduleId);
            this.modules = Collections.unmodifiableMap(modules);

            // close the registration of the undeployed module in the service provider registry
            ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = deploymentHolder.registrationReference.get();
            if (registration != null) {
                registration.close();
            } else {
                log.warnf("Removing moduleId %s from DeploymentRepository: module registration not present!", moduleId);
            }
            deploymentHolder = null;
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
     *
     * NOTE: because this listener reports changes to providers only, in order to determine if providers were added
     * orremoved, we need to keep track of the current providers to determine whether nodes were addede or removed
     * for this module.
     */
    class ModuleAvailabilityRegistrarServiceProviderRegistrationListener implements ServiceProviderRegistrationListener<GroupMember> {

        private static final Logger log = Logger.getLogger(DeploymentRepositoryService.ModuleAvailabilityRegistrarServiceProviderRegistrationListener.class.getSimpleName());
        private final EJBModuleIdentifier moduleId;
        private final List<ModuleAvailabilityRegistrarListener> listeners;

        public ModuleAvailabilityRegistrarServiceProviderRegistrationListener(EJBModuleIdentifier moduleId, List<ModuleAvailabilityRegistrarListener> listeners) {
            this.moduleId = moduleId;
            this.listeners = listeners;
        }

        /**
         * Use the information on changes in providers to notify listeners that modules have been added or removed.
         * NOTE: called even when the change in providers is locally initiated.
         *
         * @param event a registration event describing members providing the given service
         */
        @Override
        public void providersChanged(ServiceProviderRegistrationEvent<GroupMember> event) {
            log.infof("Calling providersChanged() for module %s : previous = %s, current = %s", moduleId, event.getPreviousProviders(), event.getCurrentProviders());

            Set<GroupMember> added = event.getNewProviders();
            Set<GroupMember> removed = event.getObsoleteProviders();

            log.infof("Calling providersChanged() for module %s : added = %s, removed = %s", moduleId, added, removed);

            if (!added.isEmpty()) {
                // some providers were added - create the map
                List<GroupMember> list = new ArrayList<GroupMember>(added);
                Map<EJBModuleIdentifier, List<GroupMember>> map = Map.of(moduleId, list);

                log.infof("Calling ModuleAvailabilityRegistrarListener:modulesAvailable with map = %s", map);

                // call the listeners
                for (ModuleAvailabilityRegistrarListener listener : this.listeners) {
                    listener.modulesAvailable(map);
                }
            }

            if (!removed.isEmpty()) {
                // some providers were removed - create the map
                List<GroupMember> list = new ArrayList<GroupMember>(removed);
                Map<EJBModuleIdentifier, List<GroupMember>> map = Map.of(moduleId, list);

                log.infof("Calling ModuleAvailabilityRegistrarListener:modulesUnavailable with map = %s", map);

                // call the listeners
                for (ModuleAvailabilityRegistrarListener listener : this.listeners) {
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
            log.infof("Preparing for suspend: server suspend context: isStarting = %s, isStopping = %s", context.isStarting(), context.isStopping());

            if (modules.size() != 0) {
                // unregister the service providers we have registered
                Map<EJBModuleIdentifier, DeploymentHolder> deployedModules = modules;
                for (EJBModuleIdentifier moduleId : deployedModules.keySet()) {
                    DeploymentHolder holder = deployedModules.get(moduleId);
                    ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = holder.registrationReference.get();
                    log.infof("Closing registration for module %s", moduleId);
                    if (registration != null) {
                        registration.close();
                        holder.registrationReference.set(null);
                    } else {
                        log.warnf("Closing registration for module %s: registration is null", moduleId);
                    }
                }
                log.info("Prepared for suspend - with modules");
            }
            log.info("Prepared for suspend");
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
            log.infof("Suspending: server suspend context: isStarting = %s, isStopping = %s", context.isStarting(), context.isStopping());
            // available if necessary
            log.info("Suspended");
            suspended = true;
            return SuspendableActivity.COMPLETED;
         }

        /**
         * Prepare the ServiceProviderRegistry for resuming the server.
         * When the server is resumed:
         * - find out which modules are in the deployment repository
         * - register all deployed modules as service providers
         * - for each service provider registered, callback clients will be notified (automatically) that the module is again available
         * This does not apply when the server is starting as deployments are not possible until ther server is atarted.
         *
         * @param context the server resume context
         * @return a completion stage for the SuspendController
         */
        @Override
        public CompletionStage<Void> resume(ServerResumeContext context) {
            log.infof("Resuming: server resume context: isStarting = %s", context.isStarting());

            // case: we don't need to register deployments if the server is starting
            if (!context.isStarting()) {
                log.info("Resuming - server is not starting:");
                CompletableFuture<Void> result = new CompletableFuture<>();
                // it is safe to assume no concurrent modifications while server is resuming
                AtomicInteger count = new AtomicInteger(modules.size());

                // case: only register if modules deployed
                if (count.get() != 0) {
                    log.info("Resuming - deployments need to be processed:");
                    // iterate through the locally deployed modules and add registrations to the module availability registrar
                    for (EJBModuleIdentifier moduleId : modules.keySet()) {
                        DeploymentHolder holder = modules.get(moduleId);
                        ModuleAvailabilityRegistrarServiceProviderRegistrationListener registrationListener = new ModuleAvailabilityRegistrarServiceProviderRegistrationListener(moduleId, listeners);
                        CompletableFuture.supplyAsync(() -> serviceRegistrar.register(moduleId, registrationListener))
                                .whenComplete((registration, e) -> {
                                    if (e != null) {
                                        result.completeExceptionally(e);
                                    } else {
                                        holder.registrationReference.set(registration);
                                        if (count.decrementAndGet() == 0) {
                                            log.info("Resume-completed");
                                            result.complete(null);
                                        }
                                    }
                                });
                    }
                    log.info("Resumed - deployments processed");
                    suspended = false;
                    return result;
                }
            }
            log.info("Resuming - server is starting");

            log.info("Resumed");
            suspended = false;
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
