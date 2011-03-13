/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.connector.adapters.jdbc.extensions.mysql;

import org.jboss.as.connector.adapters.jdbc.spi.ValidConnectionChecker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jboss.logging.Logger;

/**
 * <p>This class is an implementation of ValidConnectionChecker for MySQL
 * ReplicatedDriver. It supports both isValid and ping methods on the
 * connection object.
 * <p/>
 * <p>Please note that the isValid method requires java 6 classes to be present.
 * <p/>
 * <p>The code was inspired by MySQLValidConnectionChecker. See it's javadoc for
 * authors info. This code is released under the LGPL license.
 *
 * @author Luc Boudreau (lucboudreau att gmail dott com)
 */
public class MySQLReplicationValidConnectionChecker implements ValidConnectionChecker, Serializable {
    /**
     * Serial version ID
     */
    private static final long serialVersionUID = 2658231045989623858L;

    /**
     * Tells if the connection supports the isValid method.
     * (Java 6 only)
     */
    private boolean driverHasIsValidMethod;

    private transient Method isValid;

    /**
     * Tells if the connection supports the ping method.
     */
    private boolean driverHasPingMethod;

    private transient Method ping;

    /**
     * Classname of the supported connection
     */
    protected static final String CONNECTION_CLASS = "com.mysql.jdbc.ReplicationConnection";

    /**
     * Log access object
     */
    private static transient Logger log;

    // The timeout (apparently the timeout is ignored?)
    private static Object[] timeoutParam = new Object[]{};

    /**
     * Initiates the ValidConnectionChecker implementation.
     */
    public MySQLReplicationValidConnectionChecker() {
        try {
            initPing();
        } catch (Exception e) {
            log.warn("Cannot find the driver class defined in CONNECTION_CLASS.  Will use 'SELECT 1' instead.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SQLException isValidConnection(Connection c) {
        if (driverHasIsValidMethod) {
            try {
                isValid.invoke(c, timeoutParam);
            } catch (Exception e) {
                if (e instanceof SQLException) {
                    return (SQLException) e;
                } else {
                    log.warn("Unexpected error in ping", e);
                    return new SQLException("ping failed: " + e.toString());
                }
            }
        } else if (driverHasPingMethod) {
            //if there is a ping method then use it
            try {
                ping.invoke(c, timeoutParam);
            } catch (Exception e) {
                if (e instanceof SQLException) {
                    return (SQLException) e;
                } else {
                    log.warn("Unexpected error in ping", e);
                    return new SQLException("ping failed: " + e.toString());
                }
            }
        } else {
            //otherwise just use a 'SELECT 1' statement
            Statement stmt = null;
            ResultSet rs = null;

            try {
                stmt = c.createStatement();
                rs = stmt.executeQuery("SELECT 1");
            } catch (Exception e) {
                if (e instanceof SQLException) {
                    return (SQLException) e;
                } else {
                    log.warn("Unexpected error in ping (SELECT 1)", e);
                    return new SQLException("ping (SELECT 1) failed: " + e.toString());
                }
            } finally {
                // Cleanup everything and make sure to handle
                // sql exceptions occuring
                try {
                    if (rs != null)
                        rs.close();
                } catch (SQLException e) {
                    // Ignore
                }
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException e) {
                    // Ignore
                }
            }
        }

        return null;
    }


    @SuppressWarnings("unchecked")
    private void initPing() throws ClassNotFoundException, NoSuchMethodException {
        driverHasIsValidMethod = false;
        driverHasPingMethod = false;

        log = Logger.getLogger(MySQLReplicationValidConnectionChecker.class);

        // Load connection class
        Class<?> mysqlConnection = Thread.currentThread().getContextClassLoader().loadClass(CONNECTION_CLASS);

        // Check for Java 6 compatibility and use isValid on the connection
        try {
            isValid = mysqlConnection.getMethod("isValid", new Class<?>[]{});
            driverHasIsValidMethod = true;
        } catch (NoSuchMethodException e) {
            // Notify someone
            log.info("Cannot resolve com.mysq.jdbc.ReplicationConnection.isValid method. Fallback to ping.", e);
        } catch (SecurityException e) {
            // Notify someone
            log.info("Cannot resolve com.mysq.jdbc.ReplicationConnection.isValid method. Fallback to ping.", e);
        }

        if (!driverHasIsValidMethod) {
            try {
                // Check for ping method
                ping = mysqlConnection.getMethod("ping", new Class<?>[]{});
                driverHasPingMethod = true;
            } catch (NoSuchMethodException e) {
                // Notify someone
                log.warn("Cannot resolve com.mysq.jdbc.ReplicationConnection.ping method. Will use 'SELECT 1' instead.", e);
            } catch (SecurityException e) {
                // Notify someone
                log.info("Cannot resolve com.mysq.jdbc.ReplicationConnection.ping method. Will use 'SELECT 1' instead.", e);
            }
        }
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        // nothing
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        try {
            initPing();
        } catch (Exception e) {
            IOException ioe = new IOException("Unable to resolve ping method: " + e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }
}
