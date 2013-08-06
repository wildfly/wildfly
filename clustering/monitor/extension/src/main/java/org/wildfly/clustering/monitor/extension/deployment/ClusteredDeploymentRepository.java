package org.wildfly.clustering.monitor.extension.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.monitor.extension.ClusterSubsystemLogger;

/**
 * Repository of deployment information for clustered deployments.
 *
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public class ClusteredDeploymentRepository implements Service<ClusteredDeploymentRepository> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ee", "clusteredDeploymentRepository");

    private volatile Map<String, ClusteredModuleDeployment> modules;
    private final List<ClusteredDeploymentRepositoryListener> listeners = new ArrayList<ClusteredDeploymentRepositoryListener>();

    public ClusteredDeploymentRepository() {
    }

    @Override
    public ClusteredDeploymentRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        modules = Collections.emptyMap();
    }

    @Override
    public void stop(StopContext context) {
        modules = null;
    }

    public synchronized void add(String identifier, ClusteredModuleDeployment deployment) {
        final Map<String, ClusteredModuleDeployment> modules = new HashMap<String, ClusteredModuleDeployment>(this.modules);
        modules.put(identifier, deployment);
        this.modules = Collections.unmodifiableMap(modules);
        for(final ClusteredDeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentAvailable(identifier, deployment);
            } catch (Throwable t) {
                ClusterSubsystemLogger.ROOT_LOGGER.deploymentAddListenerException(t);
            }
        }
    }

    public synchronized void remove(String identifier) {
        if (this.modules == null) {
            System.out.println("ClusteredDeploymentRepository: modules is null - something wrong!");
            return;
        }
        final Map<String, ClusteredModuleDeployment> modules = new HashMap<String, ClusteredModuleDeployment>(this.modules);
        modules.remove(identifier);
        this.modules = Collections.unmodifiableMap(modules);
        for(final ClusteredDeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentRemoved(identifier);
            } catch (Throwable t) {
                ClusterSubsystemLogger.ROOT_LOGGER.deploymentRemoveListenerException(t);
            }
        }
    }

    public synchronized void addListener(final ClusteredDeploymentRepositoryListener listener) {
        listener.listenerAdded(this);
        listeners.add(listener);
    }

    public synchronized void removeListener(final ClusteredDeploymentRepositoryListener listener) {
        listeners.remove(listener);
    }

    public Map<String, ClusteredModuleDeployment> getModules() {
        return modules;
    }
}
