/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.driver.registry;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER;

import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The JDBC driver registry service
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public final class DriverRegistryService implements Service<DriverRegistry> {

    private final DriverRegistry value;

    /**
     * Create an instance
     */
    public DriverRegistryService() {
        this.value = new DriverRegistryImpl();
    }

    @Override
    public DriverRegistry getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.debugf("Starting service %s", ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE);
    }

    @Override
    public void stop(StopContext context) {
        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.debugf("Stopping service %s", ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE);
    }
}
