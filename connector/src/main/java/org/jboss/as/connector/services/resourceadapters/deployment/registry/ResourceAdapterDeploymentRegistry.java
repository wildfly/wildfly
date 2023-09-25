/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.resourceadapters.deployment.registry;

import java.util.Set;

import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;

/**
 * The interface for the resource adapter deployment registry
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public interface ResourceAdapterDeploymentRegistry {

    /**
     * Register a resource adapter deployment
     * @param deployment The deployment
     */
    void registerResourceAdapterDeployment(ResourceAdapterDeployment deployment);

    /**
     * Unregister a resource adapter deployment
     * @param deployment The deployment
     */
    void unregisterResourceAdapterDeployment(ResourceAdapterDeployment deployment);

    /**
     * Get the resource adapter deployments
     * @return The set of deployments
     */
    Set<ResourceAdapterDeployment> getResourceAdapterDeployments();
}
