/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.services.resourceadapters.deployment.registry;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.connector.logging.ConnectorLogger;
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
        if (deployment == null)
            throw new IllegalArgumentException(ConnectorLogger.ROOT_LOGGER.nullVar("Deployment"));

        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.tracef("Adding deployment: %s", deployment);

        deployments.add(deployment);
    }

    /**
     * Unregister a resource adapter deployment
     * @param deployment The deployment
     */
    public void unregisterResourceAdapterDeployment(ResourceAdapterDeployment deployment) {
        if (deployment == null)
            throw new IllegalArgumentException(ConnectorLogger.ROOT_LOGGER.nullVar("Deployment"));

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
