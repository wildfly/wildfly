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
    private volatile Map<DeploymentModuleIdentifier, ModuleDeployment> modules;

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
        final Map<DeploymentModuleIdentifier, ModuleDeployment> modules = new HashMap<DeploymentModuleIdentifier, ModuleDeployment>(this.modules);
        modules.put(identifier, deployment);
        this.modules = Collections.unmodifiableMap(modules);
        for(final DeploymentRepositoryListener listener : listeners) {
            try {
                listener.deploymentAvailable(identifier, deployment);
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
        final Map<DeploymentModuleIdentifier, ModuleDeployment> modules = new HashMap<DeploymentModuleIdentifier, ModuleDeployment>(this.modules);
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

    public Map<DeploymentModuleIdentifier, ModuleDeployment> getModules() {
        return modules;
    }

}
