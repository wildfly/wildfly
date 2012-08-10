/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.connections.database;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

import org.jboss.modules.Module;
/**
 * The only purpose for this class is to fool the {@link DriverManager} to
 * think the driver class is loaded by the current class loader and not
 * by the {@link Module} class loader.
 *
 *  @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseDriverWrapper implements Driver {

    private final Driver driver;

    public DatabaseDriverWrapper(Driver driver) {
        this.driver = driver;
    }
    @Override
    public boolean acceptsURL(String arg0) throws SQLException {
        return this.driver.acceptsURL(arg0);
    }

    @Override
    public Connection connect(String arg0, Properties arg1) throws SQLException {
        return this.driver.connect(arg0, arg1);
    }

    @Override
    public int getMajorVersion() {
        return this.driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return this.driver.getMinorVersion();
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String arg0, Properties arg1) throws SQLException {
        return this.getPropertyInfo(arg0, arg1);
    }

    @Override
    public boolean jdbcCompliant() {
        return this.jdbcCompliant();
    }

}
