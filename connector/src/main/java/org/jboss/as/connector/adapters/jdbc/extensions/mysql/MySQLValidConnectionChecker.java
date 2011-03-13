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
 * Implements check valid connection sql Requires MySQL driver 3.1.8 or later.
 * This should work on just about any version of the database itself but will
 * only be "fast" on version 3.22.1 and later. Prior to that version it just
 * does "SELECT 1" anyhow.
 *
 * @author <a href="mailto:adrian@jboss.org">Adrian Brock</a>
 * @author <a href="mailto:acoliver ot jbosss dat org">Andrew C. Oliver</a>
 * @author <a href="mailto:jim.moran@jboss.org">Jim Moran</a>
 * @version $Revision: 78074 $
 */
public class MySQLValidConnectionChecker implements ValidConnectionChecker, Serializable {
    private static transient Logger log;

    private static final long serialVersionUID = 1323747853035005642L;

    private boolean driverHasPingMethod;

    private final Integer pingTimeOut = null;

    private transient Method ping;

    /**
     * Constructor
     */
    public MySQLValidConnectionChecker() {
        try {
            initPing();
        } catch (Exception e) {
            log.warn("Cannot resolve com.mysq.jdbc.Connection.ping method.  Will use 'SELECT 1' instead.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SQLException isValidConnection(Connection c) {

        //if there is a ping method then use it, otherwise just use a 'SELECT 1' statement
        if (driverHasPingMethod) {
            Object[] params = new Object[]{pingTimeOut};

            try {
                ping.invoke(c, params);
            } catch (Exception e) {
                if (e instanceof SQLException) {
                    return (SQLException) e;
                } else {
                    log.warn("Unexpected error in ping", e);
                    return new SQLException("ping failed: " + e.toString());
                }
            }
        } else {
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
                try {
                    if (rs != null)
                        rs.close();
                } catch (SQLException ignore) {
                    // Ignore
                }
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException ignore) {
                    // Ignore
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void initPing() throws ClassNotFoundException, NoSuchMethodException {
        log = Logger.getLogger(MySQLValidConnectionChecker.class);
        driverHasPingMethod = false;

        Class<?> mysqlConnection =
                Thread.currentThread().getContextClassLoader().loadClass("com.mysql.jdbc.Connection");

        ping = mysqlConnection.getMethod("ping", new Class<?>[]{});

        if (ping != null) {
            driverHasPingMethod = true;
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

    /**
     * Get the pingTimeOut.
     *
     * @return the pingTimeOut.
     */
    public final Integer getPingTimeOut() {
        return pingTimeOut;
    }
}
