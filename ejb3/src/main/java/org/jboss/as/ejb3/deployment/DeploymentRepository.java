/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment;

import java.util.Map;

/**
 * @author Radoslav Husar
 */
public interface DeploymentRepository {

    void add(DeploymentModuleIdentifier identifier, ModuleDeployment deployment);

    boolean startDeployment(DeploymentModuleIdentifier identifier);

    void addListener(DeploymentRepositoryListener listener);

    void removeListener(DeploymentRepositoryListener listener);

    void remove(DeploymentModuleIdentifier identifier);

    void suspend();

    void resume();

    boolean isSuspended();

    /**
     * Returns all deployments. These deployments may not be in a started state, i.e. not all components might be ready to receive invocations.
     *
     * @return all deployments
     */
    Map<DeploymentModuleIdentifier, ModuleDeployment> getModules();

    /**
     * Returns all deployments that are in a started state, i.e. all components are ready to receive invocations.
     *
     * @return all started deployments
     */
    Map<DeploymentModuleIdentifier, ModuleDeployment> getStartedModules();

}
