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

import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.security.auth.Subject;

import org.jboss.logging.Logger;

/**
 * BaseWrapperManagedConnection
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:weston.price@jboss.com">Weston Price</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @version $Revision: 105425 $
 */

public abstract class BaseWrapperManagedConnection implements ManagedConnection {
    private static final WrappedConnectionFactory WRAPPED_CONNECTION_FACTORY;

    /**
     * JDBC 4 factory
     */
    private static final String JDBC4_FACTORY = "org.jboss.as.connector.adapters.jdbc.jdk6.WrappedConnectionFactoryJDK6";

    /**
     * JDBC 4.1 factory
     */
    private static final String JDBC41_FACTORY = "org.jboss.as.connector.adapters.jdbc.jdk7.WrappedConnectionFactoryJDK7";

    /**
     * The managed connection factory
     */
    protected final BaseWrapperManagedConnectionFactory mcf;

    /**
     * The connection
     */
    protected final Connection con;

    /**
     * The properties
     */
    protected final Properties props;

    private final int transactionIsolation;

    private final boolean readOnly;

    private ReentrantLock lock = new ReentrantLock(true);

    private final Collection<ConnectionEventListener> cels = new ArrayList<ConnectionEventListener>();

    private final Set<WrappedConnection> handles = new HashSet<WrappedConnection>();

    private PreparedStatementCache psCache = null;

    /**
     * The state lock
     */
    protected final Object stateLock = new Object();

    /**
     * Is inside a managed transaction
     */
    protected boolean inManagedTransaction = false;

    /**
     * Is inside a local transaction
     */
    protected AtomicBoolean inLocalTransaction = new AtomicBoolean(false);

    /**
     * JDBC auto-commit
     */
    protected boolean jdbcAutoCommit = true;

    /**
     * Underlying auto-commit
     */
    protected boolean underlyingAutoCommit = true;

    /**
     * JDBC read-only
     */
    protected boolean jdbcReadOnly;

    /**
     * Underlying read-only
     */
    protected boolean underlyingReadOnly;

    /**
     * JDBC transaction isolation
     */
    protected int jdbcTransactionIsolation;

    /**
     * Destroyed
     */
    protected boolean destroyed = false;

    static {
        Class<?> connectionFactory = null;
        try {
            connectionFactory = BaseWrapperManagedConnection.class.forName(JDBC4_FACTORY);
        } catch (ClassNotFoundException e) {
            try {
                connectionFactory = BaseWrapperManagedConnection.class.forName(JDBC41_FACTORY);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException("Unabled to load wrapped connection factory", cnfe);
            }
        }

        try {
            WRAPPED_CONNECTION_FACTORY = (WrappedConnectionFactory) connectionFactory.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error initializign connection factory", e);
        }
    }

    /**
     * Constructor
     *
     * @param mcf                  The managed connection factory
     * @param con                  The connection
     * @param props                The properties
     * @param transactionIsolation The transaction isolation
     * @param psCacheSize          The prepared statement cache size
     * @throws SQLException Thrown if an error occurs
     */
    public BaseWrapperManagedConnection(final BaseWrapperManagedConnectionFactory mcf,
                                        final Connection con,
                                        final Properties props,
                                        final int transactionIsolation,
                                        final int psCacheSize)
            throws SQLException {
        this.mcf = mcf;
        this.con = con;
        this.props = props;

        if (psCacheSize > 0)
            psCache = new PreparedStatementCache(psCacheSize);

        if (transactionIsolation == -1)
            this.transactionIsolation = con.getTransactionIsolation();

        else {
            this.transactionIsolation = transactionIsolation;
            con.setTransactionIsolation(transactionIsolation);
        }

        readOnly = con.isReadOnly();

        if (mcf.getNewConnectionSQL() != null) {
            Statement s = con.createStatement();
            try {
                s.execute(mcf.getNewConnectionSQL());
            } finally {
                s.close();
            }
        }

        underlyingReadOnly = readOnly;
        jdbcReadOnly = readOnly;
        jdbcTransactionIsolation = this.transactionIsolation;
    }

    /**
     * Add a connection event listener
     *
     * @param cel The listener
     */
    public void addConnectionEventListener(ConnectionEventListener cel) {
        synchronized (cels) {
            cels.add(cel);
        }
    }

    /**
     * Remove a connection event listener
     *
     * @param cel The listener
     */
    public void removeConnectionEventListener(ConnectionEventListener cel) {
        synchronized (cels) {
            cels.remove(cel);
        }
    }

    /**
     * Associate a handle
     *
     * @param handle The handle
     * @throws ResourceException Thrown if an error occurs
     */
    public void associateConnection(Object handle) throws ResourceException {
        if (!(handle instanceof WrappedConnection))
            throw new ResourceException("Wrong kind of connection handle to associate " + handle);

        WrappedConnection wc = (WrappedConnection) handle;
        wc.setManagedConnection(this);
        synchronized (handles) {
            handles.add(wc);
        }
    }

    /**
     * {@inheritDoc}
     */
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setLogWriter(PrintWriter param1) throws ResourceException {
    }

    /**
     * {@inheritDoc}
     */
    public void cleanup() throws ResourceException {
        synchronized (handles) {
            for (Iterator<WrappedConnection> i = handles.iterator(); i.hasNext();) {
                WrappedConnection lc = i.next();
                lc.setManagedConnection(null);
            }
            handles.clear();
        }

        // Reset all the properties we know about to defaults.
        synchronized (stateLock) {
            jdbcAutoCommit = true;
            jdbcReadOnly = readOnly;
            if (jdbcTransactionIsolation != transactionIsolation) {
                try {
                    con.setTransactionIsolation(transactionIsolation);
                    jdbcTransactionIsolation = transactionIsolation;
                } catch (SQLException e) {
                    mcf.log.warn("Error resetting transaction isolation ", e);
                }
            }
        }
        // I'm recreating the lock object when we return to the pool
        // because it looks too nasty to expect the connection handle
        // to unlock properly in certain race conditions
        // where the dissociation of the managed connection is "random".
        lock = new ReentrantLock(true);
    }

    /**
     * Lock
     */
    protected void lock() {
        lock.lock();
    }

    /**
     * Try lock
     *
     * @throws SQLException Thrown if a lock can't be obtained
     */
    protected void tryLock() throws SQLException {
        int tryLock = mcf.getUseTryLock().intValue();
        if (tryLock <= 0) {
            lock();
            return;
        }
        try {
            if (!lock.tryLock(tryLock, TimeUnit.SECONDS))
                throw new SQLException("Unable to obtain lock in " + tryLock + " seconds: " + this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted attempting lock: " + this);
        }
    }

    /**
     * Unlock
     */
    protected void unlock() {
        if (lock.isHeldByCurrentThread())
            lock.unlock();
    }

    /**
     * Get a connection
     *
     * @param subject The subject
     * @param cri     The connection request info
     * @return The connection
     * @throws ResourceException Thrown if an error occurs
     */
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        checkIdentity(subject, cri);
        WrappedConnection lc = WRAPPED_CONNECTION_FACTORY.createWrappedConnection(this,
                mcf.getSpy().booleanValue(),
                mcf.getJndiName());
        synchronized (handles) {
            handles.add(lc);
        }
        return lc;
    }

    /**
     * Destroy
     *
     * @throws ResourceException Thrown if an error occurs
     */
    public void destroy() throws ResourceException {
        synchronized (stateLock) {
            destroyed = true;
        }

        cleanup();
        try {
            // See JBAS-5678
            if (!underlyingAutoCommit)
                con.rollback();
        } catch (SQLException ignored) {
            getLog().trace("Ignored error during rollback: ", ignored);
        }
        try {
            con.close();
        } catch (SQLException ignored) {
            getLog().trace("Ignored error during close: ", ignored);
        }
    }

    /**
     * Check valid
     *
     * @return <code>True</code> if valid; otherwise <code>false</code>
     */
    public boolean checkValid() {
        SQLException e = mcf.isValidConnection(con);

        if (e == null) {
            // It's ok
            return true;
        } else {
            getLog().warn("Destroying connection that is not valid, due to the following exception: " + con, e);
            broadcastConnectionError(e);
            return false;
        }
    }

    /**
     * Get the properties
     *
     * @return The value
     */
    public Properties getProperties() {
        return this.props;
    }

    /**
     * Close a handle
     *
     * @param handle The handle
     */
    void closeHandle(WrappedConnection handle) {
        synchronized (stateLock) {
            if (destroyed)
                return;
        }

        synchronized (handles) {
            handles.remove(handle);
        }

        Collection<ConnectionEventListener> copy = null;
        synchronized (cels) {
            if (cels != null && cels.size() > 0)
                copy = new ArrayList<ConnectionEventListener>(cels);
        }

        if (copy != null) {
            ConnectionEvent ce = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
            ce.setConnectionHandle(handle);

            for (Iterator<ConnectionEventListener> i = copy.iterator(); i.hasNext();) {
                ConnectionEventListener cel = i.next();
                cel.connectionClosed(ce);
            }
        }
    }

    /**
     * Connection error
     *
     * @param t The error
     * @return The error
     */
    Throwable connectionError(Throwable t) {
        if (t instanceof SQLException) {
            if (mcf.isStaleConnection((SQLException) t)) {
                t = new StaleConnectionException((SQLException) t);

            } else {
                if (mcf.isExceptionFatal((SQLException) t)) {
                    broadcastConnectionError(t);
                }
            }
        } else {
            broadcastConnectionError(t);
        }

        return t;
    }

    /**
     * Broad cast a connection error
     *
     * @param e The error
     */
    protected void broadcastConnectionError(Throwable e) {
        synchronized (stateLock) {
            if (destroyed) {
                Logger log = getLog();
                if (log.isTraceEnabled())
                    log.trace("Not broadcasting error, already destroyed " + this, e);
                return;
            }
        }

        Exception ex = null;
        if (e instanceof Exception) {
            ex = (Exception) e;
        } else {
            ex = new ResourceAdapterInternalException("Unexpected error", e);
        }

        ConnectionEvent ce = new ConnectionEvent(this, ConnectionEvent.CONNECTION_ERROR_OCCURRED, ex);

        Collection<ConnectionEventListener> copy = null;
        synchronized (cels) {
            copy = new ArrayList<ConnectionEventListener>(cels);
        }

        for (Iterator<ConnectionEventListener> i = copy.iterator(); i.hasNext();) {
            ConnectionEventListener cel = i.next();
            try {
                cel.connectionErrorOccurred(ce);
            } catch (Throwable t) {
                getLog().warn("Error notifying of connection error for listener: " + cel, t);
            }
        }
    }

    /**
     * Get the connection
     *
     * @return The connection
     * @throws SQLException Thrown if there isn't a connection
     */
    Connection getConnection() throws SQLException {
        if (con == null)
            throw new SQLException("Connection has been destroyed!!!");

        return con;
    }

    /**
     * Get prepared statement
     *
     * @param sql                  The SQL
     * @param resultSetType        The result set type
     * @param resultSetConcurrency The result set concurrency
     * @return The statement
     * @throws SQLException Thrown if an error occurs
     */
    PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (psCache != null) {
            PreparedStatementCache.Key key =
                    new PreparedStatementCache.Key(sql,
                            PreparedStatementCache.Key.PREPARED_STATEMENT,
                            resultSetType,
                            resultSetConcurrency);

            CachedPreparedStatement cachedps = (CachedPreparedStatement) psCache.get(key);
            if (cachedps != null) {
                if (canUse(cachedps)) {
                    cachedps.inUse();
                } else {
                    return doPrepareStatement(sql, resultSetType, resultSetConcurrency);
                }
            } else {
                PreparedStatement ps = doPrepareStatement(sql, resultSetType, resultSetConcurrency);
                cachedps = WRAPPED_CONNECTION_FACTORY.createCachedPreparedStatement(ps);
                psCache.insert(key, cachedps);
            }

            return cachedps;
        } else {
            return doPrepareStatement(sql, resultSetType, resultSetConcurrency);
        }
    }

    /**
     * Create prepared statement
     *
     * @param sql                  The SQL
     * @param resultSetType        The result set type
     * @param resultSetConcurrency The result set concurrency
     * @return The statement
     * @throws SQLException Thrown if an error occurs
     */
    PreparedStatement doPrepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return con.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    /**
     * Get callable statement
     *
     * @param sql                  The SQL
     * @param resultSetType        The result set type
     * @param resultSetConcurrency The result set concurrency
     * @return The statement
     * @throws SQLException Thrown if an error occurs
     */
    CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (psCache != null) {
            PreparedStatementCache.Key key =
                    new PreparedStatementCache.Key(sql,
                            PreparedStatementCache.Key.CALLABLE_STATEMENT,
                            resultSetType,
                            resultSetConcurrency);

            CachedCallableStatement cachedps = (CachedCallableStatement) psCache.get(key);

            if (cachedps != null) {
                if (canUse(cachedps)) {
                    cachedps.inUse();
                } else {
                    return doPrepareCall(sql, resultSetType, resultSetConcurrency);
                }
            } else {
                CallableStatement cs = doPrepareCall(sql, resultSetType, resultSetConcurrency);
                cachedps = WRAPPED_CONNECTION_FACTORY.createCachedCallableStatement(cs);
                psCache.insert(key, cachedps);
            }
            return cachedps;
        } else {
            return doPrepareCall(sql, resultSetType, resultSetConcurrency);
        }
    }

    /**
     * Create callable statement
     *
     * @param sql                  The SQL
     * @param resultSetType        The result set type
     * @param resultSetConcurrency The result set concurrency
     * @return The statement
     * @throws SQLException Thrown if an error occurs
     */
    CallableStatement doPrepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return con.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    /**
     * Can the cached prepared statement be used
     *
     * @param cachedps The statement
     * @return <code>True</code> if available; otherwise <code>false</code>
     */
    boolean canUse(CachedPreparedStatement cachedps) {
        // Nobody is using it so we are ok
        if (!cachedps.isInUse())
            return true;

        // Cannot reuse prepared statements in auto commit mode
        // if will close the previous usage of the PS
        if (underlyingAutoCommit)
            return false;

        // We have been told not to share
        return mcf.sharePS;
    }

    /**
     * Get the logger
     *
     * @return The value
     */
    protected Logger getLog() {
        return mcf.log;
    }

    private void checkIdentity(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        Properties newProps = mcf.getConnectionProperties(subject, cri);
        if (!props.equals(newProps)) {
            throw new ResourceException("Wrong credentials passed to getConnection!");
        }
    }

    /**
     * The <code>checkTransaction</code> method makes sure the adapter follows the JCA
     * autocommit contract, namely all statements executed outside a container managed transaction
     * or a component managed transaction should be autocommitted. To avoid continually calling
     * setAutocommit(enable) before and after container managed transactions, we keep track of the state
     * and check it before each transactional method call.
     *
     * @throws SQLException Thrown if an error occurs
     */
    void checkTransaction() throws SQLException {
        synchronized (stateLock) {
            if (inManagedTransaction)
                return;

            // Check autocommit
            if (jdbcAutoCommit != underlyingAutoCommit) {
                con.setAutoCommit(jdbcAutoCommit);
                underlyingAutoCommit = jdbcAutoCommit;
            }
        }

        if (!jdbcAutoCommit && !inLocalTransaction.getAndSet(true)) {
            Collection<ConnectionEventListener> copy = null;
            synchronized (cels) {
                copy = new ArrayList<ConnectionEventListener>(cels);
            }

            ConnectionEvent ce = new ConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_STARTED);

            for (Iterator<ConnectionEventListener> i = copy.iterator(); i.hasNext();) {
                ConnectionEventListener cel = i.next();
                try {
                    cel.localTransactionStarted(ce);
                } catch (Throwable t) {
                    getLog().trace("Error notifying of connection committed for listener: " + cel, t);
                }
            }
        }

        checkState();
    }

    /**
     * Check state
     *
     * @throws SQLException Thrown if an error occurs
     */
    protected void checkState() throws SQLException {
        synchronized (stateLock) {
            // Check readonly
            if (jdbcReadOnly != underlyingReadOnly) {
                con.setReadOnly(jdbcReadOnly);
                underlyingReadOnly = jdbcReadOnly;
            }
        }
    }

    /**
     * Is JDBC auto-commit
     *
     * @return <code>True</code> if auto-commit; otherwise <code>false</code>
     */
    boolean isJdbcAutoCommit() {
        return inManagedTransaction ? false : jdbcAutoCommit;
    }

    /**
     * Set JDBC auto-commit
     *
     * @param jdbcAutoCommit The status
     * @throws SQLException Thrown if an error occurs
     */
    void setJdbcAutoCommit(final boolean jdbcAutoCommit) throws SQLException {
        synchronized (stateLock) {
            if (inManagedTransaction)
                throw new SQLException("You cannot set autocommit during a managed transaction!");

            this.jdbcAutoCommit = jdbcAutoCommit;
        }

        if (jdbcAutoCommit && inLocalTransaction.getAndSet(false)) {
            Collection<ConnectionEventListener> copy = null;
            synchronized (cels) {
                copy = new ArrayList<ConnectionEventListener>(cels);
            }

            ConnectionEvent ce = new ConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);

            for (Iterator<ConnectionEventListener> i = copy.iterator(); i.hasNext();) {
                ConnectionEventListener cel = i.next();
                try {
                    cel.localTransactionCommitted(ce);
                } catch (Throwable t) {
                    getLog().trace("Error notifying of connection committed for listener: " + cel, t);
                }
            }
        }
    }

    /**
     * Is JDBC read-only
     *
     * @return <code>True</code> if read-only; otherwise <code>false</code>
     */
    boolean isJdbcReadOnly() {
        return jdbcReadOnly;
    }

    /**
     * Set JDBC read-only
     *
     * @param readOnly The value
     * @throws SQLException Thrown if an error occurs
     */
    void setJdbcReadOnly(final boolean readOnly) throws SQLException {
        synchronized (stateLock) {
            if (inManagedTransaction)
                throw new SQLException("You cannot set read only during a managed transaction!");

            this.jdbcReadOnly = readOnly;
        }
    }

    /**
     * Get JDBC transaction isolation
     *
     * @return The value
     */
    int getJdbcTransactionIsolation() {
        return jdbcTransactionIsolation;
    }

    /**
     * Set JDBC transaction isolation
     *
     * @param isolationLevel The value
     * @throws SQLException Thrown if an error occurs
     */
    void setJdbcTransactionIsolation(final int isolationLevel) throws SQLException {
        synchronized (stateLock) {
            this.jdbcTransactionIsolation = isolationLevel;
            con.setTransactionIsolation(jdbcTransactionIsolation);
        }
    }

    /**
     * JDBC commit
     *
     * @throws SQLException Thrown if an error occurs
     */
    void jdbcCommit() throws SQLException {
        synchronized (stateLock) {
            if (inManagedTransaction)
                throw new SQLException("You cannot commit during a managed transaction!");

            if (jdbcAutoCommit)
                throw new SQLException("You cannot commit with autocommit set!");
        }
        con.commit();

        if (inLocalTransaction.getAndSet(false)) {
            Collection<ConnectionEventListener> copy = null;
            synchronized (cels) {
                copy = new ArrayList<ConnectionEventListener>(cels);
            }

            ConnectionEvent ce = new ConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_COMMITTED);

            for (Iterator<ConnectionEventListener> i = copy.iterator(); i.hasNext();) {
                ConnectionEventListener cel = i.next();
                try {
                    cel.localTransactionCommitted(ce);
                } catch (Throwable t) {
                    getLog().trace("Error notifying of connection committed for listener: " + cel, t);
                }
            }
        }
    }

    /**
     * JDBC rollback
     *
     * @throws SQLException Thrown if an error occurs
     */
    void jdbcRollback() throws SQLException {
        synchronized (stateLock) {
            if (inManagedTransaction)
                throw new SQLException("You cannot rollback during a managed transaction!");
            if (jdbcAutoCommit)
                throw new SQLException("You cannot rollback with autocommit set!");
        }
        con.rollback();

        if (inLocalTransaction.getAndSet(false)) {
            Collection<ConnectionEventListener> copy = null;
            synchronized (cels) {
                copy = new ArrayList<ConnectionEventListener>(cels);
            }

            ConnectionEvent ce = new ConnectionEvent(this, ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);

            for (Iterator<ConnectionEventListener> i = copy.iterator(); i.hasNext();) {
                ConnectionEventListener cel = i.next();
                try {
                    cel.localTransactionRolledback(ce);
                } catch (Throwable t) {
                    getLog().trace("Error notifying of connection rollback for listener: " + cel, t);
                }
            }
        }
    }

    /**
     * JDBC rollback
     *
     * @param savepoint A savepoint
     * @throws SQLException Thrown if an error occurs
     */
    void jdbcRollback(Savepoint savepoint) throws SQLException {
        synchronized (stateLock) {
            if (inManagedTransaction)
                throw new SQLException("You cannot rollback during a managed transaction!");

            if (jdbcAutoCommit)
                throw new SQLException("You cannot rollback with autocommit set!");
        }
        con.rollback(savepoint);
    }

    /**
     * Get track statements
     *
     * @return The value
     */
    int getTrackStatements() {
        return mcf.trackStatements;
    }

    /**
     * Is transaction query timeout
     *
     * @return <code>True</code> if ; otherwise <code>false</code>
     */
    boolean isTransactionQueryTimeout() {
        return mcf.isTransactionQueryTimeout;
    }

    /**
     * Get query timeout
     *
     * @return The value
     */
    int getQueryTimeout() {
        return mcf.getQueryTimeout();
    }

    /**
     * Check exception
     *
     * @param e The exception
     * @throws ResourceException Thrown if an error occurs
     */
    protected void checkException(SQLException e) throws ResourceException {
        connectionError(e);

        throw new ResourceException("SQLException", e);
    }
}
