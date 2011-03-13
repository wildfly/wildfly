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

package org.jboss.as.connector.adapters.jdbc;

import org.jboss.jca.core.api.connectionmanager.transaction.JTATransactionChecker;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.sql.DataSource;
import javax.transaction.RollbackException;

import org.jboss.logging.Logger;
import org.jboss.tm.TransactionTimeoutConfiguration;

/**
 * WrapperDataSource
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @version $Revision: 71788 $
 */
public class WrapperDataSource extends JBossWrapper implements Referenceable, DataSource, Serializable {
    private static final long serialVersionUID = 3570285419164793501L;

    private static Logger spyLogger = Logger.getLogger(Constants.SPY_LOGGER_CATEGORY);

    private final BaseWrapperManagedConnectionFactory mcf;
    private final ConnectionManager cm;

    private PrintWriter logger;
    private Reference reference;

    /**
     * Constructor
     *
     * @param mcf The managed connection factory
     * @param cm  The connection manager
     */
    protected WrapperDataSource(final BaseWrapperManagedConnectionFactory mcf, final ConnectionManager cm) {
        this.mcf = mcf;
        this.cm = cm;
    }

    /**
     * {@inheritDoc}
     */
    public PrintWriter getLogWriter() throws SQLException {
        return logger;
    }

    /**
     * {@inheritDoc}
     */
    public void setLogWriter(PrintWriter pw) throws SQLException {
        logger = pw;
    }

    /**
     * {@inheritDoc}
     */
    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void setLoginTimeout(int param1) throws SQLException {
    }

    /**
     * {@inheritDoc}
     */
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection() throws SQLException {
        try {
            WrappedConnection wc = (WrappedConnection) cm.allocateConnection(mcf, null);
            wc.setDataSource(this);
            wc.setSpy(mcf.getSpy().booleanValue());
            wc.setJndiName(mcf.getJndiName());
            return wc;
        } catch (ResourceException re) {
            throw new SQLException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection(String user, String password) throws SQLException {
        ConnectionRequestInfo cri = new WrappedConnectionRequestInfo(user, password);
        try {
            WrappedConnection wc = (WrappedConnection) cm.allocateConnection(mcf, cri);
            wc.setDataSource(this);
            wc.setSpy(mcf.getSpy().booleanValue());
            wc.setJndiName(mcf.getJndiName());
            return wc;
        } catch (ResourceException re) {
            throw new SQLException(re);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setReference(final Reference reference) {
        this.reference = reference;
    }

    /**
     * {@inheritDoc}
     */
    public Reference getReference() {
        return reference;
    }

    /**
     * Get the time left before a transaction timeout
     *
     * @return The amount in seconds; <code>-1</code> if no timeout
     * @throws SQLException Thrown if an error occurs
     */
    protected int getTimeLeftBeforeTransactionTimeout() throws SQLException {
        try {
            if (cm instanceof TransactionTimeoutConfiguration) {
                long timeout = ((TransactionTimeoutConfiguration) cm).getTimeLeftBeforeTransactionTimeout(true);
                // No timeout
                if (timeout == -1)
                    return -1;
                // Round up to the nearest second
                long result = timeout / 1000;
                if ((result % 1000) != 0)
                    ++result;
                return (int) result;
            } else
                return -1;
        } catch (RollbackException e) {
            throw new SQLException(e);
        }
    }

    /**
     * Check whether a tranasction is active
     *
     * @throws SQLException if the transaction is not active, preparing, prepared or committing or
     *                      for any error in the transaction manager
     */
    protected void checkTransactionActive() throws SQLException {
        if (cm == null)
            throw new SQLException("No connection manager");
        try {
            if (cm instanceof JTATransactionChecker)
                ((JTATransactionChecker) cm).checkTransactionActive();
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}
