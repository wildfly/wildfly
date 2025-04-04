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
 * across the cluster. It does this by listening for local deployment events (provided by a DeploymentRepository)
 * as well as remote deployment events (provided by a distributed ServiceProviderRegistry) and communicate that
 * module deployment information to local listeners (via ModuleAvailabilityRegistrarListemer).
 * <p>
 * This class is also suspend and resume aware, so that locally depployed modules will be marked as unavailable
 * when the server is suspended, and marked as available when the server is resumed.
 *
 * As this service is only required to support EJB deployments, it should ideally be started ON_DEMAND and
 * made available only when one or more EJBs are deployed on the server.
 */
public class ModuleAvailabilityRegistrarService implements ModuleAvailabilityRegistrar, Service {
    private final ServiceDependency<SuspendableActivityRegistry> registryDependency;
    private final ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> registrarDependency;
    private final ServiceDependency<DeploymentRepository> repositoryDependency;

    private SuspendableActivityRegistry registry;
    private ServiceProviderRegistrar<Object, GroupMember> registrar;
    private DeploymentRepository deploymentRepository;

    private final DeploymentRepositoryListener deploymentRepositoryListener ;
    private final SuspendableActivity activity;

    private final List<ModuleAvailabilityRegistrarListener> listeners = new ArrayList<>();
    private final Map<DeploymentModuleIdentifier, ServiceProviderRegistration<Object, GroupMember>> registrations = new HashMap<>();

    private boolean started;

    /**
     * Create an instance of a ModuleAvailabilityRegistrar which reflects the content of a given DepoymentRepository.
     *
     * @param deploymentRepository the repository this registrar listens to for module availability information on a given host
     * @param registrar            the ServiceProviderRegistrar instance used to store module availability information
     * @param registry             the suspendable activity registry to register our server activity with
     */
    public ModuleAvailabilityRegistrarService(ServiceDependency<SuspendableActivityRegistry> registryDependency, ServiceDependency<ServiceProviderRegistrar<Object, GroupMember>> registrarDependency, ServiceDependency<DeploymentRepository> repositoryDependency) {
        this.registryDependency = registryDependency;
        this.registrarDependency = registrarDependency;
        this.repositoryDependency = repositoryDependency;

        this.activity = new ModuleAvailabilityRegistrarSuspendableActivity();
        this.deploymentRepositoryListener = new ModuleAvailabilityRegistrarDeploymentRepositoryListener();

        this.started = false;
    }

    // service interface
    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public void start() {
        EjbLogger.ROOT_LOGGER.info("Starting ModuleAvailabilityRegistrarService");
        // inject the dependencies
        this.registry = this.registryDependency.get();
        this.registrar = this.registrarDependency.get();
        this.deploymentRepository = this.repositoryDependency.get();
        // register as a listener of the DeploymentRepository
        deploymentRepository.addListener(this.deploymentRepositoryListener);
        // register as a ServerActivity
        registry.registerActivity(this.activity);
        // mark this services as started
        this.started = true;
    }

    @Override
    public void stop() {
        EjbLogger.ROOT_LOGGER.info("Stopping ModuleAvailabilityRegistrarService");
        // unregister as a listener of the DeploymentRepository
        deploymentRepository.removeListener(this.deploymentRepositoryListener);
        // unregister as a ServerActivity
        registry.unregisterActivity(this.activity);
        // nullify the depenedencies
        this.registry = null;
        this.registrar = null;
        this.deploymentRepository = null;
        // mark the service as stopped
        this.started = false;
    }

    // the ModuleAvailabilityRegistrar listener interface

    @Override
    public void addListener(final ModuleAvailabilityRegistrarListener listener) {
        synchronized (this) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(final ModuleAvailabilityRegistrarListener listener) {
        synchronized (this) {
            listeners.remove(listener);
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
         * @param context the server suspend context
         * @return
         */
        @Override
        public CompletionStage<Void> suspend(ServerSuspendContext context) {
            // avoid spurious stop on startup during activity restoration
            if (!context.isStarting()) {
                EjbLogger.ROOT_LOGGER.debug("Suspending ModuleAvailabilityRegistrar service");
                // unregister all of the service providers we have registered
                Iterator<Map.Entry<DeploymentModuleIdentifier, ServiceProviderRegistration<Object, GroupMember>>> iterator = registrations.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<DeploymentModuleIdentifier, ServiceProviderRegistration<Object, GroupMember>> entry = iterator.next();
                    DeploymentModuleIdentifier moduleId = entry.getKey();
                    ServiceProviderRegistration<Object, GroupMember> registration = entry.getValue();
                    registration.close();
                    iterator.remove();
                }
            }
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
            for (DeploymentModuleIdentifier moduleId : deployedModules.keySet()) {
                ModuleAvailabilityRegistrarServiceProviderListener listener = new ModuleAvailabilityRegistrarServiceProviderListener(moduleId, listeners);
                ServiceProviderRegistration<Object, GroupMember> registration = registrar.register(moduleId, listener);
                // set the initial value of the providers
                listener.setCurrentProviders(registration.getProviders());
                // keep track of registrartions
                registrations.putIfAbsent(moduleId, registration);
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

        private final DeploymentModuleIdentifier moduleId;
        private final List<ModuleAvailabilityRegistrarListener> listeners;
        private Set<GroupMember> providers;

        public ModuleAvailabilityRegistrarServiceProviderListener(DeploymentModuleIdentifier moduleId, List<ModuleAvailabilityRegistrarListener> listeners) {
            this.moduleId = moduleId;
            this.listeners = listeners;
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
                    Map<DeploymentModuleIdentifier, List<GroupMember>> map = Map.of(moduleId, list);
                    listener.modulesAvailable(map);
                }
            }

            if (! removed.isEmpty()) {
                // some providers were removed, advise our listeners
                for (ModuleAvailabilityRegistrarListener listener : this.listeners) {
                    List<GroupMember> list = new ArrayList<GroupMember>();
                    list.addAll(removed);
                    Map<DeploymentModuleIdentifier, List<GroupMember>> map = Map.of(moduleId, list);
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
         * nt yet started).
         *
         * @param repository the deployment repository
         */
        @Override
        public void listenerAdded (DeploymentRepository repository){
            EjbLogger.ROOT_LOGGER.info("Adding ModuleAvailabilityRegistrarDeploymentModuleListener");
            // get all deployments in the DeploymentRepository, started or not
            Map<DeploymentModuleIdentifier, ModuleDeployment> availableModules = repository.getModules();
            for (DeploymentModuleIdentifier moduleId : availableModules.keySet()){
                EjbLogger.ROOT_LOGGER.infof("Adding moduleID %s to ServiceProviderRegistry", moduleId);
                // initialize the listener for the new service
                ModuleAvailabilityRegistrarServiceProviderListener listener = new ModuleAvailabilityRegistrarServiceProviderListener(moduleId, listeners);
                // register the new service
                ServiceProviderRegistration<Object, GroupMember> registration = registrar.register(moduleId, listener);
                // set the initial value of the providers
                listener.setCurrentProviders(registration.getProviders());
                // keep track of registrations
                ModuleAvailabilityRegistrarService.this.registrations.putIfAbsent(moduleId, registration);
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
            EjbLogger.ROOT_LOGGER.infof("Adding moduleID %s to ServiceProviderRegistry", deployment);
            ServiceProviderRegistration<Object, GroupMember> registration = registrar.register(deployment);
            // keep track of which services are registered
            registrations.putIfAbsent(deployment, registration);

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
            EjbLogger.ROOT_LOGGER.infof("Starting moduleID %s", deployment);
        }

        /**
         * Adjust the contents of the ServiceProviderRegistrar to account for a deployment which has been removed.
         *
         * @param deployment The deployment
         */
        @Override
        public void deploymentRemoved (DeploymentModuleIdentifier deployment){
            EjbLogger.ROOT_LOGGER.infof("Removing moduleID %s from ServiceProviderRegistry", deployment);
            // get hold of the deployment's service registration
            //ServiceProviderRegistration<Object, GroupMember> registration = ModuleAvailabilityRegistrarService.this.registrations.remove(deployment);
            ServiceProviderRegistration<Object, GroupMember> registration = registrations.remove(deployment);
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
}
