package org.wildfly.clustering.diagnostics.extension.deployment;


/**
 * Listener class that notifies on deployment availability changes
 *
 * @author Stuart Douglas
 * @author Richard Achmatowicz (c) 2013 Red Hat Inc.
 */
public interface ClusteredDeploymentRepositoryListener {

    /**
     * Called when the listener is added to the repository. This method runs in a synchronized block,
     * so the listener can get the current state of the repository.
     */
    void listenerAdded(final ClusteredDeploymentRepository repository);

    /**
     * Callback when a deployment becomes available
     * @param deployment The deployment
     * @param moduleDeployment
     */
    void deploymentAvailable(final String deployment, final ClusteredModuleDeployment moduleDeployment);

    /**
     * Called when a deployment is no longer available
     *
     * @param deployment The deployment
     */
    void deploymentRemoved(final String deployment);

}
