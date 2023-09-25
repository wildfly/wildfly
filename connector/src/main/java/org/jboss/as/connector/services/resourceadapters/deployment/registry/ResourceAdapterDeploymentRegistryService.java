/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.resourceadapters.deployment.registry;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The resource adapter deployment registry service
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterDeploymentRegistryService implements Service<ResourceAdapterDeploymentRegistry> {

    private final ResourceAdapterDeploymentRegistry value;

    /**
     * Create an instance
     */
    public ResourceAdapterDeploymentRegistryService() {
        this.value = new ResourceAdapterDeploymentRegistryImpl();
    }

    @Override
    public ResourceAdapterDeploymentRegistry getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.debugf("Starting service %s", ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE);
    }

    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {
        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.debugf("Stopping service %s", ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE);
    }
}
