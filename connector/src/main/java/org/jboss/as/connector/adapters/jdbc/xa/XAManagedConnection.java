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

package org.jboss.as.connector.adapters.jdbc.xa;

import org.jboss.as.connector.adapters.jdbc.BaseWrapperManagedConnection;

import java.sql.SQLException;
import java.util.Properties;

import javax.resource.ResourceException;
import javax.resource.spi.LocalTransaction;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * XAManagedConnection
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks </a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="weston.price@jboss.com">Weston Price</a>
 * @version $Revision: 76313 $
 */
public class XAManagedConnection extends BaseWrapperManagedConnection implements XAResource, LocalTransaction {
    /**
     * The XA connection
     */
    protected final XAConnection xaConnection;

    /**
     * The XAResource
     */
    protected final XAResource xaResource;

    /**
     * The Xid
     */
    protected Xid currentXid;

    /**
     * Constructor
     *
     * @param mcf                  The managed connection factory
     * @param xaConnection         The XA connection
     * @param props                The properties
     * @param transactionIsolation The transaction isolation
     * @param psCacheSize          The prepared statement cache size
     * @throws SQLException Thrown if an error occurs
     */
    public XAManagedConnection(XAManagedConnectionFactory mcf, XAConnection xaConnection, Properties props,
                               int transactionIsolation, int psCacheSize) throws SQLException {
        super(mcf, xaConnection.getConnection(), props, transactionIsolation, psCacheSize);

        this.xaConnection = xaConnection;
        xaConnection.addConnectionEventListener(new javax.sql.ConnectionEventListener() {
            /**
             * {@inheritDoc}
             */
            public void connectionClosed(javax.sql.ConnectionEvent ce) {
                //only we can do this, ignore
            }

            /**
             * {@inheritDoc}
             */
            public void connectionErrorOccurred(javax.sql.ConnectionEvent ce) {
                SQLException ex = ce.getSQLException();
                broadcastConnectionError(ex);
            }
        });

        this.xaResource = xaConnection.getXAResource();
    }

    /**
     * {@inheritDoc}
     */
    public void begin() throws ResourceException {
        lock();
        try {
            synchronized (stateLock) {
                if (!inManagedTransaction) {
                    try {
                        if (underlyingAutoCommit) {
                            underlyingAutoCommit = false;
                            con.setAutoCommit(false);
                        }
                        checkState();
                        inManagedTransaction = true;
                    } catch (SQLException e) {
                        checkException(e);
                    }
                } else
                    throw new ResourceException("Trying to begin a nested local tx");
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void commit() throws ResourceException {
        lock();
        try {
            synchronized (stateLock) {
                if (inManagedTransaction)
                    inManagedTransaction = false;
            }
            try {
                con.commit();
            } catch (SQLException e) {
                checkException(e);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rollback() throws ResourceException {
        lock();
        try {
            synchronized (stateLock) {
                if (inManagedTransaction)
                    inManagedTransaction = false;
            }
            try {
                con.rollback();
            } catch (SQLException e) {
                try {
                    checkException(e);
                } catch (Exception e2) {
                    // Ignore
                }
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void broadcastConnectionError(SQLException e) {
        super.broadcastConnectionError(e);
    }

    /**
     * {@inheritDoc}
     */
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() throws ResourceException {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() throws ResourceException {
        try {
            super.destroy();
        } finally {
            try {
                xaConnection.close();
            } catch (SQLException e) {
                checkException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void start(Xid xid, int flags) throws XAException {
        lock();
        try {
            try {
                checkState();
            } catch (SQLException e) {
                getLog().warn("Error setting state ", e);
            }

            try {
                xaResource.start(xid, flags);
            } catch (XAException e) {
                //JBAS-3336 Connections that fail in enlistment should not be returned
                //to the pool
                if (isFailedXA(e.errorCode)) {
                    broadcastConnectionError(e);
                }

                throw e;
            }

            synchronized (stateLock) {
                currentXid = xid;
                inManagedTransaction = true;
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void end(Xid xid, int flags) throws XAException {
        lock();
        try {
            try {
                xaResource.end(xid, flags);
            } catch (XAException e) {
                broadcastConnectionError(e);
                throw e;
            }


            //we want to allow ending transactions that are not the current
            //one. When one does this, inManagedTransaction is still true.
            synchronized (stateLock) {
                if (currentXid != null && currentXid.equals(xid)) {
                    inManagedTransaction = false;
                    currentXid = null;
                }
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int prepare(Xid xid) throws XAException {
        return xaResource.prepare(xid);
    }

    /**
     * {@inheritDoc}
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        xaResource.commit(xid, onePhase);
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(Xid xid) throws XAException {
        xaResource.rollback(xid);
    }

    /**
     * {@inheritDoc}
     */
    public void forget(Xid xid) throws XAException {
        xaResource.forget(xid);
    }

    /**
     * {@inheritDoc}
     */
    public Xid[] recover(int flag) throws XAException {
        return xaResource.recover(flag);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSameRM(XAResource other) throws XAException {
        Boolean overrideValue = ((XAManagedConnectionFactory) mcf).getIsSameRMOverrideValue();
        if (overrideValue != null) {
            return overrideValue.booleanValue();
        }

        // compare apples to apples
        return (other instanceof XAManagedConnection)
                ? xaResource.isSameRM(((XAManagedConnection) other).xaResource)
                : xaResource.isSameRM(other);
    }

    /**
     * {@inheritDoc}
     */
    public int getTransactionTimeout() throws XAException {
        return xaResource.getTransactionTimeout();
    }

    /**
     * {@inheritDoc}
     */
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return xaResource.setTransactionTimeout(seconds);
    }

    private boolean isFailedXA(int errorCode) {
        return (errorCode == XAException.XAER_RMERR || errorCode == XAException.XAER_RMFAIL);
    }
}
