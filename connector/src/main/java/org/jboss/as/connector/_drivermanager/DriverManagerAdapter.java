/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector._drivermanager;

import java.util.Enumeration;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class DriverManagerAdapter {

    private DriverManagerAdapter() {
    }

    public static Enumeration<Driver> getDrivers() {
        return DriverManager.getDrivers();
    }

    public static void deregisterDriver(final Driver driver) throws SQLException {
        DriverManager.deregisterDriver(driver);
    }
}
