/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.services.driver.registry;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER;
import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.connector.services.driver.InstalledDriver;

/**
 * Standard {@link DriverRegistry} implementation.
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class DriverRegistryImpl implements DriverRegistry {

    private final Map<String, InstalledDriver> drivers = Collections.synchronizedMap(new HashMap<>());

    @Override
    public void registerInstalledDriver(InstalledDriver driver) throws IllegalArgumentException {
        checkNotNullParam("driver", driver);

        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.tracef("Adding driver: %s", driver);
        drivers.put(driver.getDriverName(), driver);

    }

    @Override
    public void unregisterInstalledDriver(InstalledDriver driver) {
        checkNotNullParam("driver", driver);

        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.tracef("Removing deployment: %s", driver);
        drivers.remove(driver.getDriverName());
    }

    @Override
    public Set<InstalledDriver> getInstalledDrivers() {
        synchronized (drivers) {
            return Collections.unmodifiableSet(new HashSet<>(drivers.values()));
        }
    }

    @Override
    public InstalledDriver getInstalledDriver(String name) throws IllegalStateException {
        return drivers.get(name);
    }
}
