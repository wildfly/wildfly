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
import org.wildfly.clustering.server.provider.ServiceProviderListener;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrar;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.subsystem.service.ServiceDependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletionStage;

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
    private final Map<EJBModuleIdentifier, ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>> registrations = new HashMap<>();

    private boolean started;

    /**
     * Create an instance of a ModuleAvailabilityRegistrar which reflects the content of a given DepoymentRepository.
     *
     * @param deploymentRepository the repository this registrar listens to for module availability information on a given host
     * @param registrar            the ServiceProviderRegistrar instance used to store module availability information
     * @param registry             the suspendable activity registry to register our server activity with
     */
    public ModuleAvailabilityRegistrarService(ServiceDependency<SuspendableActivityRegistry> activityRegistryDependency, ServiceDependency<ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember>> serviceRegistrarDependency, ServiceDependency<DeploymentRepository> repositoryDependency) {
        this.activityRegistryDependency = activityRegistryDependency;
        this.serviceRegistrarDependency = serviceRegistrarDependency;
        this.repositoryDependency = repositoryDependency;

        this.activity = new ModuleAvailabilityRegistrarSuspendableActivity();
        this.deploymentRepositoryListener = new ModuleAvailabilityRegistrarDeploymentRepositoryListener();
        this.started = false;
        log.info("ModuleAvailabilityRegistrarService : <init>");
    }

    // service interface
    @Override
    public boolean isStarted() {
        // how to check started
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
        log.info("Calling getServices()");
        Set<EJBModuleIdentifier> services = new HashSet();
        for (EJBModuleIdentifier service : serviceRegistrar.getServices()) {
            services.add(service);
        }
        log.infof("Called getServices(): result = %s\n", services);
        return services;
    }

    /**
     * Return the set of providers (nodes) on which this module is deployed.
     * @param service the deployment identifier
     * @return the set of proeviders
     */
    @Override
    public Set<GroupMember> getProviders(EJBModuleIdentifier service) {
        log.infof("Calling getProviders(%s)\n", service);
        Set<GroupMember> result = serviceRegistrar.getProviders(service);
        log.infof("Called getProviders(%s): result = %s\n", result);
        return result;
//        return serviceRegistrar.getProviders(service);
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
     * Remove a listener receiving receive updates on changes in which modules are deployed in a cluster.
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
         *
         * @param context
         * @return
         */
        @Override
        public CompletionStage<Void> prepare(ServerSuspendContext context) {
            log.infof("Preparing for suspend - context: isStarting = %s, isStopping = %s\n", context.isStarting(), context.isStopping());

            if (!context.isStopping()) {
                log.info("Server not stopping - performing prapare actions");

                // unregister all of the service providers we have registered
                Iterator<Map.Entry<EJBModuleIdentifier, ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>>> iterator = registrations.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<EJBModuleIdentifier, ServiceProviderRegistration<EJBModuleIdentifier, GroupMember>> entry = iterator.next();
                    EJBModuleIdentifier moduleId = entry.getKey();
                    log.infof("Closing registration for module %s\n", moduleId);
                    ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = entry.getValue();
                    registration.close();
                    iterator.remove();
                }
            }
            log.info("Prepared for suspend");
            return SuspendableActivity.COMPLETED;
        }

        /**
         * Adjust the ServiceProviderRegistry once suspension of the server cas completed.
         *
         * @param context the server suspend context
         * @return
         */
        @Override
        public CompletionStage<Void> suspend(ServerSuspendContext context) {
            log.infof("Suspending - context: isStarting = %s, isStopping = %s\n", context.isStarting(), context.isStopping());
            // available if necessary
            if (!context.isStopping()) {
                log.info("Server not stopping - performing suspend actions");
                // guard against peforming actions in susoend while server stopping
            }
            log.info("Suspended");
            return SuspendableActivity.COMPLETED;
        }

        /**
         * Prepare the ServiceProviderRegistry for resuming the server.
         * When the server is resumed:
         * - find out which modules are in the deployment repository
         * - register all deployed modules as service providers
         * - for each service provider registered, callback clients will be notified (automatically) that the module is again available
         *
         * @param context the server resume context
         * @return
         */
        @Override
        public CompletionStage<Void> resume(ServerResumeContext context) {
            log.infof("Resuming - context: isStarting = %s\n", context.isStarting());

            if (!context.isStarting()) {
                log.info("Server not starting - performing resume actions");

                // interrogate the deployment repository and register the current deployments
                Map<EJBModuleIdentifier, ModuleDeployment> deployedModules = deploymentRepository.getModules();

                // create one service entry for each module
                for (EJBModuleIdentifier moduleId : deployedModules.keySet()) {
                    // only register modules that do not already have a local registration entry
                    if (registrations.get(moduleId) == null) {
                        ModuleAvailabilityRegistrarServiceProviderListener serviceProviderListener = new ModuleAvailabilityRegistrarServiceProviderListener(moduleId, moduleAvailabilityListeners);
                        log.infof("Opening registration for module %s\n" + moduleId);
                        ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = serviceRegistrar.register(moduleId, serviceProviderListener);
                        // set the initial value of the providers
                        serviceProviderListener.setCurrentProviders(registration.getProviders());
                        // keep track of registrartions
                        registrations.putIfAbsent(moduleId, registration);
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
    class ModuleAvailabilityRegistrarServiceProviderListener implements ServiceProviderListener<GroupMember> {

        private final EJBModuleIdentifier moduleId;
        private final List<ModuleAvailabilityRegistrarListener> listeners;
        private Set<GroupMember> providers;

        public ModuleAvailabilityRegistrarServiceProviderListener(EJBModuleIdentifier moduleId, List<ModuleAvailabilityRegistrarListener> listeners) {
            this.moduleId = moduleId;
            this.listeners = listeners;
            this.providers = new HashSet<GroupMember>();
        }

        /**
         * Set the initial value of current providers, immediately after registration
         *
         * @param currentProviders
         */
        public void setCurrentProviders(Set<GroupMember> currentProviders) {
            this.providers = currentProviders;
        }

        /**
         * Use the information on changes in providers to notifiy listeners that modules have been added or removed.
         * NOTE: called even when the change in providers is locally initiated.
         *
         * @param providers the new set of group members providing the given service
         */
        @Override
        public void providersChanged(Set<GroupMember> providers) {
            log.infof("Calling providerChanged() for module %s : providers = %s\n", moduleId, providers);
            // check if this module has been added or removed

            // calculate providers which have not changed
            Set<GroupMember> intersection = new HashSet<GroupMember>();
            intersection.addAll(this.providers);
            intersection.retainAll(providers);

            // calculate providers which have left
            Set<GroupMember> removed = new HashSet<GroupMember>();
            removed.addAll(this.providers);
            removed.removeAll(intersection);

            // calculate providers which have appeared
            Set<GroupMember> added = new HashSet<GroupMember>();
            added.addAll(providers);
            added.removeAll(intersection);

            if (! added.isEmpty()) {
                // some providers were added
                for (ModuleAvailabilityRegistrarListener listener : this.listeners) {
                    List<GroupMember> list = new ArrayList<GroupMember>();
                    list.addAll(added);
                    // EJBModuleIdentifier
                    Map<EJBModuleIdentifier, List<GroupMember>> map = Map.of(moduleId, list);
                    listener.modulesAvailable(map);
                }
            }

            if (! removed.isEmpty()) {
                // some providers were removed, advise our listeners
                for (ModuleAvailabilityRegistrarListener listener : this.listeners) {
                    List<GroupMember> list = new ArrayList<GroupMember>();
                    list.addAll(removed);
                    Map<EJBModuleIdentifier, List<GroupMember>> map = Map.of(moduleId, list);
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

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a set of new deployment (possibly
         * not yet started).
         *
         * @param repository the deployment repository
         */
        @Override
        public void listenerAdded (DeploymentRepository repository){
            log.info("Adding ModuleAvailabilityRegistrarDeploymentModuleListener");

            // get all deployments in the DeploymentRepository, started or not
            Map<EJBModuleIdentifier, ModuleDeployment> availableModules = repository.getModules();

            for (EJBModuleIdentifier moduleId : availableModules.keySet()){
                log.infof("Adding moduleID %s to ServiceProviderRegistry\n", moduleId);

                // initialize the listener for the new service
                ModuleAvailabilityRegistrarServiceProviderListener serviceProviderListener = new ModuleAvailabilityRegistrarServiceProviderListener(moduleId, moduleAvailabilityListeners);

                // register the new service
                ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = serviceRegistrar.register(moduleId, serviceProviderListener);

                // set the initial value of the providers
                serviceProviderListener.setCurrentProviders(registration.getProviders());

                // keep track of registrations
                registrations.putIfAbsent(moduleId, registration);
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
        public void deploymentAvailable (EJBModuleIdentifier moduleId, ModuleDeployment moduleDeployment){
            // add an entry to the ServiceProviderRegistrar
            log.infof("Adding moduleID %s to ServiceProviderRegistry\n", moduleId);

            // initialize the listener for the new service
            ModuleAvailabilityRegistrarServiceProviderListener serviceProviderListener = new ModuleAvailabilityRegistrarServiceProviderListener(moduleId, moduleAvailabilityListeners);

            // register the new service
            ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = serviceRegistrar.register(moduleId, serviceProviderListener);

            // set the initial value of the providers
            serviceProviderListener.setCurrentProviders(registration.getProviders());

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
        public void deploymentStarted (EJBModuleIdentifier moduleId, ModuleDeployment moduleDeployment){
            // TODO: how do we differentiate between deployments whch have not started and those which have started?
            log.infof("Adding started moduleID %s to ServiceProviderRegistry\n", moduleId);
        }

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a deployment which has been removed.
         *
         * @param moduleId The deployment
         */
        @Override
        public void deploymentRemoved (EJBModuleIdentifier moduleId){
            log.infof("Removing moduleID %s from ServiceProviderRegistry\n", moduleId);

            // get hold of the deployment's service registration
            ServiceProviderRegistration<EJBModuleIdentifier, GroupMember> registration = registrations.remove(moduleId);

            // close the registrarion
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

    /**
     * Dump out the contents of the local registrations map (debugging)
     *
     * @param a descriptive message of context
     * @param serviceRegistrar
     */
    private void dumpRegistrations(String message, Map<EJBModuleIdentifier, ServiceProviderRegistration<Object, GroupMember>> registrations) {

        log.infof("Dumping registered modules on node %s for: %s\n", System.getProperty("jboss.node.name", "unknown") , message);
        for (EJBModuleIdentifier moduleId: registrations.keySet()) {
            log.info("Registered module: " + moduleId);
        }
    }

    /**
     * Dump out the contents of the registrar (debugging)
     *
     * @param a descriptive message of context
     * @param serviceRegistrar
     */
    private void dumpServices(String message, ServiceProviderRegistrar<EJBModuleIdentifier, GroupMember> serviceRegistrar) {
        log.infof("Dumping service registrar contents for : %s\n" + message);
        for (EJBModuleIdentifier moduleId: serviceRegistrar.getServices()) {
            log.infof("Registered module: %s\n" + (EJBModuleIdentifier) moduleId);
            for (GroupMember m: serviceRegistrar.getProviders(moduleId)) {
                log.infof("Module provider: %s\n" + m);
            }
        }
    }
}


