/**
 *
 */
package org.jboss.as.connector.services.driver;

import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.sql.Driver;

import static org.jboss.as.connector.logging.ConnectorMessages.MESSAGES;

/**
 * Service wrapper for a {@link java.sql.Driver}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DriverService implements Service<Driver> {

    private final InjectedValue<DriverRegistry> injectedDriverRegistry = new InjectedValue<DriverRegistry>();

    private final InstalledDriver driverMetaData;
    private final Driver driver;

    public DriverService(InstalledDriver driverMetaData, Driver driver) {
        assert driverMetaData != null : MESSAGES.nullVar("driverMetaData");
        assert driver != null : MESSAGES.nullVar("driver");
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
    }

    @Override
    public void stop(StopContext context) {
        injectedDriverRegistry.getValue().unregisterInstalledDriver(driverMetaData);
    }

    public Injector<DriverRegistry> getDriverRegistryServiceInjector() {
        return injectedDriverRegistry;
    }

}
