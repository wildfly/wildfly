/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.as.ejb3.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;


/**
 * Repository for information about deployed modules. This includes information on all the deployed EJB's in the module
 *
 * @author Stuart Douglas
 */
public class DeploymentRepository implements Service<DeploymentRepository> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ee", "deploymentRepository");

    /**
     * All deployed modules. This is a copy on write map that is updated infrequently and read often.
     */
    private volatile Map<DeploymentModuleIdentifier, DeploymentHolder> modules;

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
        modules = null;
    }

    @Override
    public DeploymentRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void add(DeploymentModuleIdentifier identifier, ModuleDeployment deployment) {
        final List<DeploymentRepositoryListener> listeners;
        final boolean suspended;
        synchronized (this) {
            final Map<DeploymentModuleIdentifier, DeploymentHolder> modules = new HashMap<DeploymentModuleIdentifier, DeploymentHolder>(this.modules);
            modules.put(identifier, new DeploymentHolder(deployment));
            this.modules = Collections.unmodifiableMap(modules);
            listeners = new ArrayList<DeploymentRepositoryListener>(this.listeners);
            suspended = this.suspended;
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentAvailable(identifier, deployment);
                if (suspended) {
                    listener.deploymentSuspended(identifier);
                }
            } catch (Throwable t) {
                EjbLogger.DEPLOYMENT_LOGGER.deploymentAddListenerException(t);
            }
        }
    }

    public boolean startDeployment(DeploymentModuleIdentifier identifier) {
        DeploymentHolder deployment;
        final List<DeploymentRepositoryListener> listeners;
        synchronized (this) {
            deployment = modules.get(identifier);
            if (deployment == null) return false;
            deployment.started = true;
            listeners = new ArrayList<DeploymentRepositoryListener>(this.listeners);
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentStarted(identifier, deployment.deployment);
            } catch (Throwable t) {
                EjbLogger.DEPLOYMENT_LOGGER.deploymentAddListenerException(t);
            }
        }
        return true;
    }


    public void addListener(final DeploymentRepositoryListener listener) {
        synchronized (this) {
            listeners.add(listener);
        }
        listener.listenerAdded(this);
    }

    public synchronized void removeListener(final DeploymentRepositoryListener listener) {
        listeners.remove(listener);
    }

    public void remove(DeploymentModuleIdentifier identifier) {
        final List<DeploymentRepositoryListener> listeners;
        synchronized (this) {
            final Map<DeploymentModuleIdentifier, DeploymentHolder> modules = new HashMap<DeploymentModuleIdentifier, DeploymentHolder>(this.modules);
            modules.remove(identifier);
            this.modules = Collections.unmodifiableMap(modules);
            listeners = new ArrayList<DeploymentRepositoryListener>(this.listeners);
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentRemoved(identifier);
            } catch (Throwable t) {
                EjbLogger.DEPLOYMENT_LOGGER.deploymentRemoveListenerException(t);
            }
        }
    }

    public void suspend() {
        final List<DeploymentRepositoryListener> listeners;
        final Set<DeploymentModuleIdentifier> moduleIdentifiers;
        synchronized (this) {
            moduleIdentifiers = new HashSet<>(this.modules.keySet());
            listeners = new ArrayList<>(this.listeners);
            suspended = true;
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            for (DeploymentModuleIdentifier moduleIdentifier : moduleIdentifiers)
            try {
                listener.deploymentSuspended(moduleIdentifier);
            } catch (Throwable t) {
                EjbLogger.DEPLOYMENT_LOGGER.deploymentAddListenerException(t);
            }
        }
    }

    public void resume() {
        final List<DeploymentRepositoryListener> listeners;
        final Set<DeploymentModuleIdentifier> moduleIdentifiers;
        synchronized (this) {
            moduleIdentifiers = new HashSet<>(this.modules.keySet());
            listeners = new ArrayList<>(this.listeners);
            suspended = false;
        }
        for (final DeploymentRepositoryListener listener : listeners) {
            for (DeploymentModuleIdentifier moduleIdentifier : moduleIdentifiers)
                try {
                    listener.deploymentResumed(moduleIdentifier);
                } catch (Throwable t) {
                    EjbLogger.DEPLOYMENT_LOGGER.deploymentAddListenerException(t);
                }
        }
    }

    /**
     * Returns all the deployments. These deployments may not be in a started state, i.e. not all components might be ready to receive invocations.
     * @return All the deployments
     */
    public Map<DeploymentModuleIdentifier, ModuleDeployment> getModules() {
        Map<DeploymentModuleIdentifier, ModuleDeployment> modules = new HashMap<DeploymentModuleIdentifier, ModuleDeployment>();
        for(Map.Entry<DeploymentModuleIdentifier, DeploymentHolder> entry : this.modules.entrySet()) {
            modules.put(entry.getKey(), entry.getValue().deployment);
        }
        return modules;
    }

    /**
     * Returns all the deployments that are in a started state, i.e. all components are ready to receive invocations.
     * @return All the started deployments
     */
    public Map<DeploymentModuleIdentifier, ModuleDeployment> getStartedModules() {
        Map<DeploymentModuleIdentifier, ModuleDeployment> modules = new HashMap<DeploymentModuleIdentifier, ModuleDeployment>();
        for(Map.Entry<DeploymentModuleIdentifier, DeploymentHolder> entry : this.modules.entrySet()) {
            if(entry.getValue().started) {
                modules.put(entry.getKey(), entry.getValue().deployment);
            }
        }
        return modules;
    }

    private class DeploymentHolder {
        final ModuleDeployment deployment;
        volatile boolean started = false;

        private DeploymentHolder(ModuleDeployment deployment) {
            this.deployment = deployment;
        }
    }

}
