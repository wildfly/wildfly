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
package org.jboss.as.domain.management.security;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DATA_SOURCE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;

import org.jboss.as.domain.management.connections.database.DatabaseConnectionManagerService;
import org.jboss.as.domain.management.connections.database.DatabaseConnectionPool;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoadException;
import org.jboss.sasl.util.UsernamePasswordHashUtil;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
/**
 *  Test helper for setting up the DatabaseConnectionManagerService and the connection pool
 *  and create the proper tables and test data
 *
 *  @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public abstract class AbstractDatabaseConnectionTestHelper {

    protected static final String TEST_REALM = "TestRealm";
    protected static UsernamePasswordHashUtil hashUtil;
    protected static String hashedPassword;
    protected static TestDatabaseConnectionPool connectionPool;

    protected DatabaseConnectionManagerService dcs;

    @BeforeClass
    public static void initDatabase() throws Exception {
        hashUtil = new UsernamePasswordHashUtil();
        hashedPassword = hashUtil.generateHashedHexURP("Henry.Deacon",TEST_REALM,"eureka".toCharArray());

        connectionPool = new TestDatabaseConnectionPool("", "org.h2.Driver", "jdbc:h2:mem:databaseauthtest", "sa", "sa", 1, 2, 500, 500, 500);
        initTables(connectionPool);
    }

    @AfterClass
    public static void terminateDatabase() throws Exception {
        connectionPool.closeConnections();
        connectionPool = null;
    }

    @Before
    public void init() throws Exception {
        ModelNode cmNode = new ModelNode();
        cmNode.get(OP).set(ADD);
        cmNode.get(DATA_SOURCE).set("test"); //just a name. it's not used anyway because the getConnection is override

        dcs = new DatabaseConnectionManagerService(cmNode) {
            @Override
            public Object getConnection() throws Exception {
                return connectionPool.getConnection();
            }
        };
        initAuthenticationModel(true);
        initCallbackHandler(dcs);
    }

    private static void initTables(TestDatabaseConnectionPool connectionPool) throws Exception {
        Connection connection = connectionPool.getConnection();
        Statement statement = connection.createStatement();
        statement.addBatch("CREATE TABLE USERS(user VARCHAR(32) PRIMARY KEY,   password VARCHAR(255));");
        statement.addBatch("CREATE TABLE ROLES(user VARCHAR(32) PRIMARY KEY,   roles VARCHAR(255));");
        statement.addBatch("insert into users values('Jack.Carter','eureka')");
        statement.addBatch("insert into users values('Henry.Deacon','"+hashedPassword+"')");
        statement.addBatch("insert into roles values('Jack.Carter','sheriff,dad,lifesaver')");
        statement.addBatch("insert into roles values('Henry.Deacon','')");
        statement.addBatch("insert into roles values('Christopher.Chance','buggydata;¤¤¤%,,')");
        statement.executeBatch();
        statement.close();
        connection.close();
    }

    /**
     * Setup the model for the database authentication / authorization
     * @param plainPassword
     */
    abstract void initAuthenticationModel(boolean plainPassword);

    static class TestDatabaseConnectionPool extends DatabaseConnectionPool {

        public TestDatabaseConnectionPool(String module, String driver, String url, String user, String password, int minPoolSize,
                int maxPoolSize, long timeout, long connectiontimeout, long delay) throws SQLException, InstantiationException,
                IllegalAccessException, ClassNotFoundException, InterruptedException, IllegalArgumentException,
                ModuleLoadException, SecurityException, NoSuchMethodException, InvocationTargetException {
            super(module, driver, url, user, password, minPoolSize, maxPoolSize, timeout, connectiontimeout, delay);
        }

        @Override
        protected Driver getDriver(String module, String driverClassName) throws ModuleLoadException,  ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException,
                InvocationTargetException {
            return Class.forName(driverClassName).asSubclass(Driver.class).newInstance();

        }
    }

    /**
     * Setup up the proper callback handler for your test
     * @param dcs
     */
    abstract void initCallbackHandler(DatabaseConnectionManagerService dcs) throws Exception;
}
