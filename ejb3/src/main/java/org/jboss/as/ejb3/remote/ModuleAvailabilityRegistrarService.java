/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.remote;

import org.jboss.as.ejb3.deployment.DeploymentModuleIdentifier;
import org.jboss.as.ejb3.deployment.DeploymentRepository;
import org.jboss.as.ejb3.deployment.DeploymentRepositoryListener;
import org.jboss.as.ejb3.deployment.ModuleDeployment;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.as.server.suspend.ServerResumeContext;
import org.jboss.as.server.suspend.ServerSuspendContext;
import org.jboss.as.server.suspend.SuspendableActivity;
import org.jboss.as.server.suspend.SuspendableActivityRegistry;
import org.jboss.ejb.client.EJBModuleIdentifier;
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
    private final ServiceDependency<SuspendableActivityRegistry> activityRegistryDependency;
    private final ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> serviceRegistrarDependency;
    private final ServiceDependency<DeploymentRepository> repositoryDependency;

    private SuspendableActivityRegistry activityRegistry;
    private ServiceProviderRegistrar<Object, GroupMember> serviceRegistrar;
    private DeploymentRepository deploymentRepository;

    private final DeploymentRepositoryListener deploymentRepositoryListener ;
    private final SuspendableActivity activity;

    private final List<ModuleAvailabilityRegistrarListener> moduleAvailabilityListeners = new ArrayList<>();
    private final Map<EJBModuleIdentifier, ServiceProviderRegistration<Object, GroupMember>> registrations = new HashMap<>();

    private boolean started;

    /**
     * Create an instance of a ModuleAvailabilityRegistrar which reflects the content of a given DepoymentRepository.
     *
     * @param deploymentRepository the repository this registrar listens to for module availability information on a given host
     * @param registrar            the ServiceProviderRegistrar instance used to store module availability information
     * @param registry             the suspendable activity registry to register our server activity with
     */
    public ModuleAvailabilityRegistrarService(ServiceDependency<SuspendableActivityRegistry> activityRegistryDependency, ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> serviceRegistrarDependency, ServiceDependency<DeploymentRepository> repositoryDependency) {
        this.activityRegistryDependency = activityRegistryDependency;
        this.serviceRegistrarDependency = serviceRegistrarDependency;
        this.repositoryDependency = repositoryDependency;

        this.activity = new ModuleAvailabilityRegistrarSuspendableActivity();
        this.deploymentRepositoryListener = new ModuleAvailabilityRegistrarDeploymentRepositoryListener();
        this.started = false;
        EjbLogger.ROOT_LOGGER.info("ModuleAvailabilityRegistrarService : <init>");
    }

    // service interface
    @Override
    public boolean isStarted() {
        // how to check started
        return started;
    }

    @Override
    public void start() {
        EjbLogger.ROOT_LOGGER.debug("Starting ModuleAvailabilityRegistrarService");

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
        EjbLogger.ROOT_LOGGER.debug("Stopping ModuleAvailabilityRegistrarService");

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
        for (Object service : serviceRegistrar.getServices()) {
            services.add((EJBModuleIdentifier) service);
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
            EjbLogger.ROOT_LOGGER.debug("Preparing for suspend of ModuleAvailabilityRegistrar service");

            // unregister all of the service providers we have registered
            Iterator<Map.Entry<EJBModuleIdentifier,ServiceProviderRegistration<Object,GroupMember>>> iterator = registrations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<EJBModuleIdentifier, ServiceProviderRegistration<Object, GroupMember>> entry = iterator.next();
                EJBModuleIdentifier ejbModuleId = entry.getKey();
                EjbLogger.ROOT_LOGGER.debug("Closing registration for module " + ejbModuleId);
                ServiceProviderRegistration<Object, GroupMember> registration = entry.getValue();
                registration.close();
                iterator.remove();
            }
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
            EjbLogger.ROOT_LOGGER.debug("Suspended ModuleAvailabilityRegistrar service");
            // available if necessary
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
            EjbLogger.ROOT_LOGGER.debug("Resuming ModuleAvailabilityRegistrar service");
            // interrogate the deployment repository and register the current deployments
            Map<DeploymentModuleIdentifier, ModuleDeployment> deployedModules = deploymentRepository.getModules();

            // create one service entry for each module
            for (DeploymentModuleIdentifier deploymentModuleId : deployedModules.keySet()) {
                // convert DeploymentModuleIdentifier to EJBModuleIdentifier before stashing into SPR
                EJBModuleIdentifier ejbModuleId = convertModuleIdentifier(deploymentModuleId);

                // only register modules that do not already have a local registration entry
                if (registrations.get(ejbModuleId) == null) {
                    ModuleAvailabilityRegistrarServiceProviderListener serviceProviderListener = new ModuleAvailabilityRegistrarServiceProviderListener(ejbModuleId, moduleAvailabilityListeners);
                    EjbLogger.ROOT_LOGGER.debug("Opening registration for module " + ejbModuleId);
                    ServiceProviderRegistration<Object, GroupMember> registration = serviceRegistrar.register(ejbModuleId, serviceProviderListener);
                    // set the initial value of the providers
                    serviceProviderListener.setCurrentProviders(registration.getProviders());
                    // keep track of registrartions
                    registrations.putIfAbsent(ejbModuleId, registration);
                }
            }
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
            EjbLogger.ROOT_LOGGER.debugf("Calling providerChanged() for module %s : providers = ", moduleId, providers);
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
            EjbLogger.ROOT_LOGGER.debug("Adding ModuleAvailabilityRegistrarDeploymentModuleListener");

            // get all deployments in the DeploymentRepository, started or not
            Map<DeploymentModuleIdentifier, ModuleDeployment> availableModules = repository.getModules();

            for (DeploymentModuleIdentifier deploymentModuleId : availableModules.keySet()){
                EjbLogger.ROOT_LOGGER.debugf("Adding moduleID %s to ServiceProviderRegistry", deploymentModuleId);

                // convert module identifier before stashing into SPR
                EJBModuleIdentifier ejbModuleId = convertModuleIdentifier(deploymentModuleId);

                // initialize the listener for the new service
                ModuleAvailabilityRegistrarServiceProviderListener serviceProviderListener = new ModuleAvailabilityRegistrarServiceProviderListener(ejbModuleId, moduleAvailabilityListeners);

                // register the new service
                ServiceProviderRegistration<Object, GroupMember> registration = serviceRegistrar.register(ejbModuleId, serviceProviderListener);

                // set the initial value of the providers
                serviceProviderListener.setCurrentProviders(registration.getProviders());

                // keep track of registrations
                registrations.putIfAbsent(ejbModuleId, registration);
            }
        }

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a new deployment (possibly
         * not yet started).
         *
         * @param deployment The deployment
         * @param moduleDeployment module deployment
         */
        @Override
        public void deploymentAvailable (DeploymentModuleIdentifier deployment, ModuleDeployment moduleDeployment){
            // add an entry to the ServiceProviderRegistrar
            EjbLogger.ROOT_LOGGER.debugf("Adding moduleID %s to ServiceProviderRegistry", deployment);

            // convert module identifier before stashing into SPR
            EJBModuleIdentifier ejbModuleId = convertModuleIdentifier(deployment);

            // initialize the listener for the new service
            ModuleAvailabilityRegistrarServiceProviderListener serviceProviderListener = new ModuleAvailabilityRegistrarServiceProviderListener(ejbModuleId, moduleAvailabilityListeners);

            // register the new service
            ServiceProviderRegistration<Object, GroupMember> registration = serviceRegistrar.register(ejbModuleId, serviceProviderListener);

            // set the initial value of the providers
            serviceProviderListener.setCurrentProviders(registration.getProviders());

            // keep track of which services are registered
            registrations.putIfAbsent(ejbModuleId, registration);
        }

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a new deployment which has now started.
         *
         * @param deployment The deployment
         * @param moduleDeployment module deployment
         */
        @Override
        public void deploymentStarted (DeploymentModuleIdentifier deployment, ModuleDeployment moduleDeployment){
            // TODO: how do we differentiate between deployments whch have not started and those which have started?
            EjbLogger.ROOT_LOGGER.debugf("Adding started moduleID %s to ServiceProviderRegistry", deployment);
        }

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a deployment which has been removed.
         *
         * @param deployment The deployment
         */
        @Override
        public void deploymentRemoved (DeploymentModuleIdentifier deployment){
            EjbLogger.ROOT_LOGGER.debugf("Removing moduleID %s from ServiceProviderRegistry", deployment);

            // convert module identifier before stashing into SPR
            EJBModuleIdentifier ejbModuleId = convertModuleIdentifier(deployment);

            // get hold of the deployment's service registration
            ServiceProviderRegistration<Object, GroupMember> registration = registrations.remove(ejbModuleId);

            // close the registrarion
            registration.close();
        }

        @Override
        public void deploymentSuspended (DeploymentModuleIdentifier deployment){
            // this method will be deprecated
        }

        @Override
        public void deploymentResumed (DeploymentModuleIdentifier deployment){
            // this method will be deprecated
        }
    }

    /**
     * Method to translate DeploymentModuleIdentifier instances into EJBModuleIdentifier instances
     * @param deploymentModuleIdentifier
     * @return
     */
    private EJBModuleIdentifier convertModuleIdentifier(DeploymentModuleIdentifier deploymentModuleIdentifier) {
        return new EJBModuleIdentifier(deploymentModuleIdentifier.getApplicationName(), deploymentModuleIdentifier.getModuleName(), deploymentModuleIdentifier.getDistinctName());
    }

    /**
     * Dump out the contents of the local registrations map (debugging)
     *
     * @param a descriptive message of context
     * @param serviceRegistrar
     */
    private void dumpRegistrations(String message, Map<EJBModuleIdentifier, ServiceProviderRegistration<Object, GroupMember>> registrations) {

        System.out.println("Dumping registered modules on node " + System.getProperty("jboss.node.name", "unknown") + " for: " + message);
        for (EJBModuleIdentifier id: registrations.keySet()) {
            System.out.println("Registered module: " + id);
        }
    }

    /**
     * Dump out the contents of the registrar (debugging)
     *
     * @param a descriptive message of context
     * @param serviceRegistrar
     */
    private void dumpServices(String message, ServiceProviderRegistrar<Object, GroupMember> serviceRegistrar) {
        System.out.println("Dumping service registrar contents for : " + message);
        for (Object objectId: serviceRegistrar.getServices()) {
            System.out.println("Registered module: " + (EJBModuleIdentifier) objectId);
            for (GroupMember m: serviceRegistrar.getProviders(objectId)) {
                System.out.println("Module provider: " + m);
            }
        }
    }
}


