/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 *
 */
package org.jboss.as.connector.services.driver;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYER_JDBC_LOGGER;

import java.sql.Driver;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service wrapper for a {@link Driver}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DriverService implements Service<Driver> {

    private final InjectedValue<DriverRegistry> injectedDriverRegistry = new InjectedValue<DriverRegistry>();

    private final InstalledDriver driverMetaData;
    private final Driver driver;

    public DriverService(InstalledDriver driverMetaData, Driver driver) {
        assert driverMetaData != null : ConnectorLogger.ROOT_LOGGER.nullVar("driverMetaData");
        assert driver != null : ConnectorLogger.ROOT_LOGGER.nullVar("driver");
        this.driverMetaData = driverMetaData;
        this.driver = driver;
    }

    @Override
    public Driver getValue() throws IllegalStateException, IllegalArgumentException {
        return driver;
    }

    @Override
    public void start(StartContext context) throws StartException {
        injectedDriverRegistry.getValue().registerInstalledDriver(driverMetaData);
        DEPLOYER_JDBC_LOGGER.startedDriverService(driverMetaData.getDriverName());

    }

    @Override
    public void stop(StopContext context) {
        injectedDriverRegistry.getValue().unregisterInstalledDriver(driverMetaData);
        DEPLOYER_JDBC_LOGGER.stoppedDriverService(driverMetaData.getDriverName());
    }

    public Injector<DriverRegistry> getDriverRegistryServiceInjector() {
        return injectedDriverRegistry;
    }

}
