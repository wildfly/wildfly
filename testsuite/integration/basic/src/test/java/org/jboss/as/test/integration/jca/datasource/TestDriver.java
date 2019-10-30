/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.jca.datasource;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Test JDBC driver
 */
public class TestDriver implements Driver {
    /**
    * {@inheritDoc}
    */
    public Connection connect(String url, Properties info) throws SQLException {
        return null;
    }

    /**
    * {@inheritDoc}
    */
    public boolean acceptsURL(String url) throws SQLException {
        return true;
    }

    /**
    * {@inheritDoc}
    */
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        Driver driver = DriverManager.getDriver(url);
        return driver.getPropertyInfo(url, info);
    }

    /**
    * {@inheritDoc}
    */
    public int getMajorVersion() {
        return 1;
    }

    /**
    * {@inheritDoc}
    */
    public int getMinorVersion() {
        return 0;
    }

    /**
    * {@inheritDoc}
    */
    public boolean jdbcCompliant() {
        return false;
    }

    /**
    * {@inheritDoc}
    */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}
