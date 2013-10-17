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
import java.util.List;
import java.util.Map;

import org.jboss.as.ejb3.EjbLogger;
import org.jboss.logging.Logger;
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

    public synchronized void add(DeploymentModuleIdentifier identifier, ModuleDeployment deployment) {
        final Map<DeploymentModuleIdentifier, DeploymentHolder> modules = new HashMap<DeploymentModuleIdentifier, DeploymentHolder>(this.modules);
        modules.put(identifier, new DeploymentHolder(deployment));
        this.modules = Collections.unmodifiableMap(modules);
        for(final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentAvailable(identifier, deployment);
            } catch (Throwable t) {
                EjbLogger.ROOT_LOGGER.deploymentAddListenerException(t);
            }
        }
    }

    public synchronized void startDeployment(DeploymentModuleIdentifier identifier) {
        DeploymentHolder deployment = modules.get(identifier);
        deployment.started = true;
        for(final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentStarted(identifier, deployment.deployment);
            } catch (Throwable t) {
                EjbLogger.ROOT_LOGGER.deploymentAddListenerException(t);
            }
        }
    }


    public synchronized void addListener(final DeploymentRepositoryListener listener) {
        listener.listenerAdded(this);
        listeners.add(listener);
    }

    public synchronized void removeListener(final DeploymentRepositoryListener listener) {
        listeners.remove(listener);
    }

    public synchronized void remove(DeploymentModuleIdentifier identifier) {
        final Map<DeploymentModuleIdentifier, DeploymentHolder> modules = new HashMap<DeploymentModuleIdentifier, DeploymentHolder>(this.modules);
        modules.remove(identifier);
        this.modules = Collections.unmodifiableMap(modules);
        for(final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentRemoved(identifier);
            } catch (Throwable t) {
                EjbLogger.ROOT_LOGGER.deploymentRemoveListenerException(t);
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
     * Returns all the deployments that are in a started state, i.e. all components are ready to recieve invocations.
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
