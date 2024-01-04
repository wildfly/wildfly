/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.deployment;

/**
 * Listener class that notifies on deployment availability changes
 *
 * @author Stuart Douglas
 */
public interface DeploymentRepositoryListener {

    /**
     * Called when the listener is added to the repository. This method runs in a synchronized block,
     * so the listener can get the current state of the repository.
     */
    void listenerAdded(final DeploymentRepository repository);

    /**
     * Callback when a deployment becomes available
     * @param deployment The deployment
     * @param moduleDeployment module deployment
     */
    void deploymentAvailable(final DeploymentModuleIdentifier deployment, final ModuleDeployment moduleDeployment);


    /**
     * Callback when a deployment has started, i.e. all components have started
     * @param deployment The deployment
     * @param moduleDeployment module deployment
     */
    void deploymentStarted(final DeploymentModuleIdentifier deployment, final ModuleDeployment moduleDeployment);

    /**
     * Called when a deployment is no longer available
     *
     * @param deployment The deployment
     */
    void deploymentRemoved(final DeploymentModuleIdentifier deployment);

    /**
     * Called when a deployment is suspended, as a result of server suspension.
     * @param deployment The deployment
     */
    default void deploymentSuspended(final DeploymentModuleIdentifier deployment){}

    /**
     * Called when a deployment is no longer suspended, as a result of server resume.
     * <br>
     * Can only be invoked after {@link #deploymentSuspended(DeploymentModuleIdentifier)}, i.e, if none of these two
     * methods have been invoked is because the server is not suspended.
     *
     * @param deployment The deployment
     */
    default void deploymentResumed(final DeploymentModuleIdentifier deployment) {}
}
