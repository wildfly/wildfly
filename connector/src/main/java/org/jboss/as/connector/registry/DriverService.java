/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

/**
 *
 */
package org.jboss.as.connector.registry;

import java.sql.Driver;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service wrapper for a {@link java.sql.Driver}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DriverService implements Service<Driver> {

    private static final Logger log = Logger.getLogger("org.jboss.as.connector.registry");

    private InjectedValue<DriverRegistry> injectedDriverRegistry = new InjectedValue<DriverRegistry>();

    private final InstalledDriver driverMetaData;
    private final Driver driver;

    public DriverService(InstalledDriver driverMetaData, Driver driver) {
        assert driverMetaData != null : "driverMetaData is null";
        assert driver != null : "driver is null";
        this.driverMetaData = driverMetaData;
        this.driver = driver;
    }

    @Override
    public Driver getValue() throws IllegalStateException, IllegalArgumentException {
        return driver;
    }

    @Override
    public void start(StartContext context) throws StartException {
        DriverRegistry registry = injectedDriverRegistry.getValue();
        boolean ok = false;
        registry.registerInstalledDriver(driverMetaData);
        try {
            DriverManager.registerDriver(driver);
            ok = true;
        } catch (SQLException e) {
            throw new StartException(e);
        } finally {
            if (! ok) try {
                registry.unregisterInstalledDriver(driverMetaData);
            } catch (Throwable t) {
                log.error("Failed to unregister JDBC driver from JCA registry after service failed", t);
            }
        }
    }

    @Override
    public void stop(StopContext context) {
        try {
            injectedDriverRegistry.getValue().unregisterInstalledDriver(driverMetaData);
        } catch (Throwable t) {
            log.error("Failed to unregister JDBC driver from JCA registry in service stop", t);
        }
        try {
            DriverManager.deregisterDriver(driver);
        } catch (SQLException e) {
            log.error("Failed to unregister JDBC driver from driver manager in service stop", e);
        }
    }

    public Injector<DriverRegistry> getDriverRegistryServiceInjector() {
        return injectedDriverRegistry;
    }

}
