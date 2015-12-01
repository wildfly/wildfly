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
package org.jboss.as.connector.services.driver.registry;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.connector.logging.ConnectorLogger;
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
        if (driver == null)
            throw new IllegalArgumentException(ConnectorLogger.ROOT_LOGGER.nullVar("driver"));

        DEPLOYMENT_CONNECTOR_REGISTRY_LOGGER.tracef("Adding driver: %s", driver);
        drivers.put(driver.getDriverName(), driver);

    }

    @Override
    public void unregisterInstalledDriver(InstalledDriver driver) {
        if (driver == null)
            throw new IllegalArgumentException(ConnectorLogger.ROOT_LOGGER.nullVar("driver"));

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
