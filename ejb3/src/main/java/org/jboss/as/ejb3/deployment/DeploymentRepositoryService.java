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

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.ejb.client.EJBModuleIdentifier;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;


/**
 * Repository for information about deployed modules. This includes information on all the deployed Jakarta Enterprise Beans's in the module
 *
 * @author Stuart Douglas
 */
public class DeploymentRepositoryService implements DeploymentRepository, Service<DeploymentRepository> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ee", "deploymentRepository");

    /**
     * All deployed modules. This is a copy on write map that is updated infrequently and read often.
     */
    private volatile Map<EJBModuleIdentifier, DeploymentHolder> modules;

    private final List<DeploymentRepositoryListener> listeners = new ArrayList<DeploymentRepositoryListener>();

    /**
     * Keeps track of whether the repository is suspended or not
     */
    private volatile boolean suspended = false;


    @Override
    public void start(StartContext context) throws StartException {
        modules = Collections.emptyMap();
    }

    @Override
    public void stop(StopContext context) {
        modules = Collections.emptyMap();
    }

    @Override
    public DeploymentRepository getValue() {
        return this;
    }

    @Override
    public void add(EJBModuleIdentifier moduleId, ModuleDeployment deployment) {
        final List<DeploymentRepositoryListener> listeners;
        final boolean suspended;
        synchronized (this) {
            final Map<EJBModuleIdentifier, DeploymentHolder> modules = new HashMap<EJBModuleIdentifier, DeploymentHolder>(this.modules);
            modules.put(moduleId, new DeploymentHolder(deployment));
            this.modules = Collections.unmodifiableMap(modules);
            listeners = new ArrayList<DeploymentRepositoryListener>(this.listeners);
            suspended = this.suspended;
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentAvailable(moduleId, deployment);
                if (suspended) {
                    listener.deploymentSuspended(moduleId);
                }
            } catch (Throwable t) {
                EjbLogger.DEPLOYMENT_LOGGER.deploymentAddListenerException(t);
            }
        }
    }

    @Override
    public boolean startDeployment(EJBModuleIdentifier moduleId) {
        DeploymentHolder deployment;
        final List<DeploymentRepositoryListener> listeners;
        synchronized (this) {
            deployment = modules.get(moduleId);
            if (deployment == null) return false;
            deployment.started = true;
            listeners = new ArrayList<DeploymentRepositoryListener>(this.listeners);
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentStarted(moduleId, deployment.deployment);
            } catch (Throwable t) {
                EjbLogger.DEPLOYMENT_LOGGER.deploymentAddListenerException(t);
            }
        }
        return true;
    }

    @Override
    public void addListener(final DeploymentRepositoryListener listener) {
        synchronized (this) {
            listeners.add(listener);
        }
        listener.listenerAdded(this);
    }

    @Override
    public synchronized void removeListener(final DeploymentRepositoryListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void remove(EJBModuleIdentifier moduleId) {
        final List<DeploymentRepositoryListener> listeners;
        synchronized (this) {
            final Map<EJBModuleIdentifier, DeploymentHolder> modules = new HashMap<EJBModuleIdentifier, DeploymentHolder>(this.modules);
            modules.remove(moduleId);
            this.modules = Collections.unmodifiableMap(modules);
            listeners = new ArrayList<DeploymentRepositoryListener>(this.listeners);
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentRemoved(moduleId);
            } catch (Throwable t) {
                EjbLogger.DEPLOYMENT_LOGGER.deploymentRemoveListenerException(t);
            }
        }
    }

    @Override
    public boolean isSuspended() {
        return suspended;
    }

    @Override
    public void suspend() {
        final List<DeploymentRepositoryListener> listeners;
        final Set<EJBModuleIdentifier> moduleIdentifiers;
        synchronized (this) {
            moduleIdentifiers = new HashSet<>(this.modules.keySet());
            listeners = new ArrayList<>(this.listeners);
            suspended = true;
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            for (EJBModuleIdentifier moduleIdentifier : moduleIdentifiers)
            try {
                listener.deploymentSuspended(moduleIdentifier);
            } catch (Throwable t) {
                EjbLogger.DEPLOYMENT_LOGGER.deploymentAddListenerException(t);
            }
        }
    }

    @Override
    public void resume() {
        final List<DeploymentRepositoryListener> listeners;
        final Set<EJBModuleIdentifier> moduleIdentifiers;
        synchronized (this) {
            moduleIdentifiers = new HashSet<>(this.modules.keySet());
            listeners = new ArrayList<>(this.listeners);
            suspended = false;
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            for (EJBModuleIdentifier moduleIdentifier : moduleIdentifiers)
                try {
                    listener.deploymentResumed(moduleIdentifier);
                } catch (Throwable t) {
                    EjbLogger.DEPLOYMENT_LOGGER.deploymentAddListenerException(t);
                }
        }
    }

    @Override
    public Map<EJBModuleIdentifier, ModuleDeployment> getModules() {
        Map<EJBModuleIdentifier, ModuleDeployment> modules = new HashMap<EJBModuleIdentifier, ModuleDeployment>();
        for(Map.Entry<EJBModuleIdentifier, DeploymentHolder> entry : this.modules.entrySet()) {
            modules.put(entry.getKey(), entry.getValue().deployment);
        }
        return modules;
    }

    @Override
    public Map<EJBModuleIdentifier, ModuleDeployment> getStartedModules() {
        Map<EJBModuleIdentifier, ModuleDeployment> modules = new HashMap<EJBModuleIdentifier, ModuleDeployment>();
        for(Map.Entry<EJBModuleIdentifier, DeploymentHolder> entry : this.modules.entrySet()) {
            if(entry.getValue().started) {
                modules.put(entry.getKey(), entry.getValue().deployment);
            }
        }
        return modules;
    }

    private static final class DeploymentHolder {
        final ModuleDeployment deployment;
        volatile boolean started = false;

        private DeploymentHolder(ModuleDeployment deployment) {
            this.deployment = deployment;
        }
    }

}
