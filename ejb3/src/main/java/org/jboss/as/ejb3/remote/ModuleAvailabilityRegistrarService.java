/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.remote;

import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryListener;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.manager.Service;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationEvent;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationListener;
import org.wildfly.subsystem.service.ServiceDependency;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service which implements a ModuleAvailabilityRegistrar.
 * <p>
 * The purpose of this class is to take local module availablity information and make it globally available
 * across the cluster. It does this by doing thee folowing:
 * * listening for local deployment events (provided by a DeploymentRepository)
 * * listening for remote deployment events (provided by a distributed ServiceProviderRegistry)
 * * communicate that module deployment information to local listeners (via ModuleAvailabilityRegistrarListemer).
 * <p>
 * This class is also suspend and resume aware, so that locally depployed modules will be marked as unavailable
 * when the server is suspended, and marked as available when the server is resumed.
 */
public class ModuleAvailabilityRegistrarService implements ModuleAvailabilityRegistrar, Service {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("as", "ejb3", "remote", "module-availability-registrar-service");

    protected static final Logger log = Logger.getLogger(ModuleAvailabilityRegistrarService.class.getSimpleName());

    private final ServiceDependency<SuspendableActivityRegistry> activityRegistryDependency;
    private final ServiceDependency<ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember>> serviceRegistrarDependency;
    private final ServiceDependency<DeploymentRepository> repositoryDependency;

    private SuspendableActivityRegistry activityRegistry;
    private ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember> serviceRegistrar;
    private DeploymentRepository deploymentRepository;

    private final DeploymentRepositoryListener deploymentRepositoryListener ;
    private final SuspendableActivity activity;

    private final List<ModuleAvailabilityRegistrarListener> moduleAvailabilityListeners = new ArrayList<>();
    private final Map<EJBModuleIdentifier, ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>> registrations = new ConcurrentHashMap<>();

    private boolean started;

    /**
     * Create an instance of a ModuleAvailabilityRegistrar which reflects the content of a given DepoymentRepository.
     *
     * @param activityRegistryDependency a dependency reference to the suspendable activity registry to register our server activity with
     * @param serviceRegistrarDependency a dependency reference to ServiceProviderRegistrar providing module availability information
     * @param repositoryDependency a dependency reference to the deployment repository
     */
    public ModuleAvailabilityRegistrarService(ServiceDependency<SuspendableActivityRegistry> activityRegistryDependency, ServiceDependency<ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember>> serviceRegistrarDependency, ServiceDependency<DeploymentRepository> repositoryDependency) {
        this.activityRegistryDependency = activityRegistryDependency;
        this.serviceRegistrarDependency = serviceRegistrarDependency;
        this.repositoryDependency = repositoryDependency;

        this.activity = new ModuleAvailabilityRegistrarSuspendableActivity();
        this.deploymentRepositoryListener = new ModuleAvailabilityRegistrarDeploymentRepositoryListener();
        this.started = false;
    }

    // the service interface

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
        log.info("Starting ModuleAvailabilityRegistrarService");

        // inject the dependencies
        this.activityRegistry = this.activityRegistryDependency.get();
        this.serviceRegistrar = this.serviceRegistrarDependency.get();
        this.deploymentRepository = this.repositoryDependency.get();

        // register as a listener of the DeploymentRepository
        deploymentRepository.addListener(this.deploymentRepositoryListener);

        // register as a ServerActivity
        activityRegistry.registerActivity(this.activity);

        // mark this services as started
        this.started = true;
    }

    @Override
    public void stop() {
        log.info("Stopping ModuleAvailabilityRegistrarService");

        // unregister as a listener of the DeploymentRepository
        this.deploymentRepository.removeListener(this.deploymentRepositoryListener);

        // unregister as a ServerActivity
        activityRegistry.unregisterActivity(this.activity);

        // nullify the depenedencies
        this.activityRegistry = null;
        this.serviceRegistrar = null;
        this.deploymentRepository = null;

        // mark the service as stopped
        this.started = false;
    }

    // the ModuleAvailabilityRegistrar listener interface

    /**
     * Return the set of modules currently deployed in the cluster
     * @return a set of EJBModuleIdentifier instances representing each module
     */
    @Override
    public Set<EJBModuleIdentifier> getServices() {
        Set<EJBModuleIdentifier> services = new HashSet();
        for (EJBModuleIdentifier service : serviceRegistrar.getServices()) {
            services.add(service);
        }
        return services;
    }

    /**
     * Return the set of providers (nodes) on which this module is deployed.
     * @param service the deployment identifier
     * @return the set of proeviders
     */
    @Override
    public Set<GroupMember> getProviders(EJBModuleIdentifier service) {
        return serviceRegistrar.getProviders(service);
    }

    /**
     * Add a listener which will receive updates on changes in which modules are deployed in a cluster.
     *
     * @param listener
     */
    @Override
    public void addListener(final ModuleAvailabilityRegistrarListener listener) {
        synchronized (this) {
            moduleAvailabilityListeners.add(listener);
        }
        listener.listenerAdded(this);
    }

    /**
     * Remove a listener receiving updates on changes in which modules are deployed in a cluster.
     *
     * @param listener
     */
    @Override
    public void removeListener(final ModuleAvailabilityRegistrarListener listener) {
        synchronized (this) {
            moduleAvailabilityListeners.remove(listener);
        }
    }

    /*
     * A SuspendableActivity that controls what happens when the server is suspended and resumed.
     */
    class ModuleAvailabilityRegistrarSuspendableActivity implements SuspendableActivity {

        /**
         * Prepare the ServiceProviderRegistry for suspension of the server.
         * When the server is suspended:
         * - unregister all registered service providers
         * - for each service provider unregistered, callback clients will be notified that the module is no longer available
         * This also includes the case where the server is being suspended as part pf clean shutdown.
         *
         * @param context the server suspend context
         * @return a completion stage for the ServerSuspendController
         */
        @Override
        public CompletionStage<Void> prepare(ServerSuspendContext context) {
            log.infof("Preparing for suspend - context: isStarting = %s, isStopping = %s", context.isStarting(), context.isStopping());

            // unregister all of the service providers we have registered
            Iterator<Map.Entry<EJBModuleIdentifier, ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>>> iterator = registrations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<EJBModuleIdentifier, ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>> entry = iterator.next();
                EJBModuleIdentifier moduleId = entry.getKey();
                log.infof("Closing registration for module %s", moduleId);
                ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = entry.getValue();
                registration.close();
                iterator.remove();
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
            log.infof("Suspending - context: isStarting = %s, isStopping = %s", context.isStarting(), context.isStopping());
            // available if necessary

            log.info("Suspended");
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
         * @return
         */
        @Override
        public CompletionStage<Void> resume(ServerResumeContext context) {
            log.infof("Resuming - context: isStarting = %s", context.isStarting());

            if (!context.isStarting()) {
                log.info("Server not starting - performing resume actions");

                // interrogate the deployment repository and register the current deployments
                Map<EJBModuleIdentifier, ModuleDeployment> deployedModules = deploymentRepository.getModules();

                // create one service entry for each module
                for (EJBModuleIdentifier moduleId : deployedModules.keySet()) {
                    // only register modules that do not already have a local registration entry
                    if (registrations.get(moduleId) == null) {
                        ModuleAvailabilityRegistrarServiceProviderRegistrationListener serviceProviderRegistrationListener = new ModuleAvailabilityRegistrarServiceProviderRegistrationListener(moduleId, moduleAvailabilityListeners);
                        log.infof("Opening registration for module %s" + moduleId);
                        ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = serviceRegistrar.register(moduleId, serviceProviderRegistrationListener);
                        // keep track of registrations
                        ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> oldRegistration = registrations.putIfAbsent(moduleId, registration);
                        if (oldRegistration != null) {
                            log.warnf("resume: Found stale registration for module %s" + moduleId);
                        }
                    }
                }
            }
            log.info("Resumed");
            return SuspendableActivity.COMPLETED;
        }
    }

    /*
     * This listener is notified of changes for a particular service that has been registered.
     *
     * NOTE: because this listener reports changes to providers only, in order to determine if providers were added
     * orremoved, we need to keep track of the current providers to determine whether nodes were addede or removed
     * for this module.
     */
    class ModuleAvailabilityRegistrarServiceProviderRegistrationListener implements ServiceProviderRegistrationListener<GroupMember> {

        private static final Logger log = Logger.getLogger(ModuleAvailabilityRegistrarServiceProviderRegistrationListener.class.getSimpleName());
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
         * @param providers a registration event describing members providing the given service
         */
        @Override
        public synchronized void providersChanged(ServiceProviderRegistrationEvent<GroupMember> providers) {
            log.infof("Calling providersChanged() for module %s : previous = %s, current = %s", moduleId, providers.getPreviousProviders(), providers.getCurrentProviders());

            Set<GroupMember> added = providers.getNewProviders();
            Set<GroupMember> removed = providers.getObsoleteProviders();

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
     * This listener receives events concerning the local deployment repository and adds the information to the
     * shared ServiceProviderRegistry.so that remote nodes can see what is deployed on this node.
     */
    class ModuleAvailabilityRegistrarDeploymentRepositoryListener implements DeploymentRepositoryListener {

        private static final Logger log = Logger.getLogger(ModuleAvailabilityRegistrarDeploymentRepositoryListener.class.getSimpleName());

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a set of new deployment (possibly
         * not yet started).
         *
         * @param repository the deployment repository
         */
        @Override
        public synchronized void listenerAdded (DeploymentRepository repository){
            log.info("Adding ModuleAvailabilityRegistrarDeploymentRepositoryListener");

            // get all deployments in the DeploymentRepository, started or not
            Map<EJBModuleIdentifier, ModuleDeployment> availableModules = repository.getModules();

            for (EJBModuleIdentifier moduleId : availableModules.keySet()){
                log.infof("Adding moduleID %s to ServiceProviderRegistry", moduleId);

                // initialize the listener for the new service
                ModuleAvailabilityRegistrarServiceProviderRegistrationListener serviceProviderRegistrationListener = new ModuleAvailabilityRegistrarServiceProviderRegistrationListener(moduleId, moduleAvailabilityListeners);

                // register the new service
                ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = serviceRegistrar.register(moduleId, serviceProviderRegistrationListener);

                // keep track of registrations
                ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> oldRegistration = registrations.putIfAbsent(moduleId, registration);
                if (oldRegistration != null) {
                    log.warnf("listenerAdded: Found stale registration for module %s" + moduleId);
                }
            }
        }

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a new deployment (possibly
         * not yet started).
         *
         * @param moduleId The deployment
         * @param moduleDeployment module deployment
         */
        @Override
        public synchronized void deploymentAvailable (EJBModuleIdentifier moduleId, ModuleDeployment moduleDeployment){
            // add an entry to the ServiceProviderRegistrar
            log.infof("Adding moduleID %s to ServiceProviderRegistry", moduleId);

            // initialize the listener for the new service
            ModuleAvailabilityRegistrarServiceProviderRegistrationListener serviceProviderRegistrationListener = new ModuleAvailabilityRegistrarServiceProviderRegistrationListener(moduleId, moduleAvailabilityListeners);

            // register the new service
            ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = serviceRegistrar.register(moduleId, serviceProviderRegistrationListener);

            // keep track of which services are registered
            registrations.putIfAbsent(moduleId, registration);
        }

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a new deployment which has now started.
         *
         * @param moduleId The deployment
         * @param moduleDeployment module deployment
         */
        @Override
        public synchronized void deploymentStarted (EJBModuleIdentifier moduleId, ModuleDeployment moduleDeployment){
            // TODO: how do we differentiate between deployments whch have not started and those which have started?
            log.infof("Adding started moduleID %s to ServiceProviderRegistry", moduleId);
        }

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a deployment which has been removed.
         *
         * @param moduleId The deployment
         */
        @Override
        public synchronized void deploymentRemoved (EJBModuleIdentifier moduleId){
            log.infof("Removing moduleID %s from ServiceProviderRegistry", moduleId);

            // get hold of the deployment's service registration
            ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = registrations.remove(moduleId);

            // close the registration
            registration.close();
        }

        @Override
        public void deploymentSuspended (EJBModuleIdentifier moduleId){
            // this method will be deprecated
        }

        @Override
        public void deploymentResumed (EJBModuleIdentifier moduleId){
            // this method will be deprecated
        }
    }
}


