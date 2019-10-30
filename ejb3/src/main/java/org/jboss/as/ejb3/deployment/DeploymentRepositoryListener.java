/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
