/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.loginmodules.databases;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import org.jboss.as.test.integration.security.common.servlets.SimpleSecuredServlet;
import org.jboss.logging.Logger;

/**
 * This bean will create users and roles in ExampleDS datasource on startup.
 *
 * @author Filip Bogyai
 */

@Singleton
@Startup
public class DatabaseCreatorBean {

    public static final String DS_JNDI = "java:jboss/datasources/ExampleDS";

    public static final String TABLE_NAME_USERS = "test_users";
    public static final String TABLE_NAME_ROLES = "test_roles";

    public static final String COLUMN_USER = "test_user";
    public static final String COLUMN_ROLE = "test_role";
    public static final String COLUMN_ROLE_GROUP = "test_role_group";
    public static final String COLUMN_PASSWORD = "test_password";

    private static Logger LOGGER = Logger.getLogger(DatabaseCreatorBean.class);

    private DataSource ds;

    @PostConstruct
    public void initDatabase() throws Exception {
        InitialContext iniCtx = new InitialContext();
        ds = (DataSource) iniCtx.lookup(DS_JNDI);

        final Connection conn = ds.getConnection();
        executeUpdate(conn, "CREATE TABLE " + TABLE_NAME_ROLES + "(" + COLUMN_USER + " Varchar(50), " + COLUMN_ROLE + " Varchar(50), "
                + COLUMN_ROLE_GROUP + " Varchar(50))");
        executeUpdate(conn, "INSERT INTO " + TABLE_NAME_ROLES + " VALUES ('anil','" + SimpleSecuredServlet.ALLOWED_ROLE + "','Roles')");
        executeUpdate(conn, "INSERT INTO " + TABLE_NAME_ROLES + " VALUES ('marcus','superuser','Roles')");
        executeUpdate(conn, "CREATE TABLE " + TABLE_NAME_USERS + "(" + COLUMN_USER + " Varchar(50), " + COLUMN_PASSWORD + " Varchar(50))");
        executeUpdate(conn, "INSERT INTO " + TABLE_NAME_USERS + " VALUES ('anil','anil')");
        executeUpdate(conn, "INSERT INTO " + TABLE_NAME_USERS + " VALUES ('marcus','marcus')");
        conn.close();
    }

    private void executeUpdate(Connection connection, String query) {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            final int updateResult = statement.executeUpdate(query);
            LOGGER.trace("Result: " + updateResult + ".  SQL statement: " + query);
        } catch (SQLException e) {
            LOGGER.error("SQL execution failed", e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOGGER.error("Statement close failed", e);
                }
            }
        }
    }

    @PreDestroy
    public void dropTables() {
        try {
            final Connection conn = ds.getConnection();
            executeUpdate(conn, "DROP TABLE " + TABLE_NAME_ROLES);
            executeUpdate(conn, "DROP TABLE " + TABLE_NAME_USERS);
            conn.close();
        } catch (SQLException e) {
            LOGGER.error("Dropping tables failed", e);
        }
    }
}
