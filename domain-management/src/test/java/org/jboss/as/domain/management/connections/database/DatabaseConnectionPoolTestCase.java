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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.sql.Driver;
import java.sql.SQLException;

import org.jboss.modules.ModuleLoadException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
/**
 *  Database Connection Pool Test
 *
 *  @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionPoolTestCase {

    private DatabaseConnectionPool connectionPool;


    @Before
    public void init() throws Exception {
        connectionPool = new DatabaseConnectionPool("", "org.h2.Driver", "jdbc:h2:mem:pooltestcase", "sa", "sa", 1, 2, 500, 500, 500) {
            @Override
            protected Driver getDriver(String module, String driverClassName) throws ModuleLoadException,  ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
                    InvocationTargetException {
                return Class.forName(driverClassName).asSubclass(Driver.class).newInstance();

            }
        };
    }

    @After
    public void terminate() throws SQLException {
        connectionPool.closeConnections();
        connectionPool = null;
    }

    @Test
    public void testCloseConnections() throws Exception {
        DatabaseConnection connection = (DatabaseConnection) connectionPool.getConnection();
        connectionPool.closeConnections();
        assertEquals(false,connection.inUse());
        assertEquals(true,connection.isClosed());
    }

    @Test
    public void testGetConnection() throws Exception {
        DatabaseConnection connection = (DatabaseConnection) connectionPool.getConnection();
        assertEquals(true,connection.validate());
    }

    @Test
    public void testReturnConnection() throws Exception {
        DatabaseConnection connection = (DatabaseConnection) connectionPool.getConnection();
        connectionPool.returnConnection(connection);
        assertEquals(false,connection.inUse());
    }

    @Test
    public void testMaxPoolSize() throws Exception {
        connectionPool.setTimeout(1000);
        connectionPool.getConnection();
        connectionPool.getConnection();
        try {
            connectionPool.getConnection();
            fail("Expect the getConnection throw a timeout because of timeout on acquire lock");
        } catch (InterruptedException e) {
        }
    }

    @Test
    public void testReaper() throws Exception {
        connectionPool = new DatabaseConnectionPool("","org.h2.Driver", "jdbc:h2:mem:pooltestcase", "sa", "sa", 2,5,100,100,100) {
            @Override
            protected Driver getDriver(String module, String driverClassName) throws ModuleLoadException,  ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
                    InvocationTargetException {
                return Class.forName(driverClassName).asSubclass(Driver.class).newInstance();

            }
        };

        Thread.sleep(1000);
        assertEquals(2,connectionPool.getCurrentPoolSize());

    }

}
