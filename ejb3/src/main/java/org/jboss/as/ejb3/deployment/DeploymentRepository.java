package org.jboss.as.ejb3.deployment;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;


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

    // Copy-on-write set, since it's not updated frequently but will be traversed relatively more often
    private final Collection<DeploymentRepositoryListener> listeners = new CopyOnWriteArraySet<DeploymentRepositoryListener>();


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
            listener.deploymentAvailable(identifier, deployment);
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
            listener.deploymentRemoved(identifier);
        }
    }

    public Map<DeploymentModuleIdentifier, ModuleDeployment> getModules() {
        return modules;
    }

}
