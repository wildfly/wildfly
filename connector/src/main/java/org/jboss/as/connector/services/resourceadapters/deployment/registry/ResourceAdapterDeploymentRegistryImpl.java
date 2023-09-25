/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.resourceadapters.deployment.registry;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER;
import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;


/**
 * The interface for the resource adapter deployment registry
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterDeploymentRegistryImpl implements ResourceAdapterDeploymentRegistry {

    private Set<ResourceAdapterDeployment> deployments;

    /**
     * Constructor
     */
    public ResourceAdapterDeploymentRegistryImpl() {
        this.deployments = new HashSet<ResourceAdapterDeployment>();
    }

    /**
     * Register a resource adapter deployment
     * @param deployment The deployment
     */
    public void registerResourceAdapterDeployment(ResourceAdapterDeployment deployment) {
        checkNotNullParam("deployment", deployment);

        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.tracef("Adding deployment: %s", deployment);

        deployments.add(deployment);
    }

    /**
     * Unregister a resource adapter deployment
     * @param deployment The deployment
     */
    public void unregisterResourceAdapterDeployment(ResourceAdapterDeployment deployment) {
        checkNotNullParam("deployment", deployment);

        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.tracef("Removing deployment: %s", deployment);

        deployments.remove(deployment);
    }

    /**
     * Get the resource adapter deployments
     * @return The set of deployments
     */
    public Set<ResourceAdapterDeployment> getResourceAdapterDeployments() {
        return Collections.unmodifiableSet(deployments);
    }
}
