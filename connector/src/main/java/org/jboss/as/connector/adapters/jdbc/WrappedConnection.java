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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.jboss.logging.Logger;

/**
 * A wrapper for a connection.
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @version $Revision: 96595 $
 */
public abstract class WrappedConnection extends JBossWrapper implements Connection {
    private static Logger log = Logger.getLogger(WrappedConnection.class);

    /**
     * The spy logger
     */
    protected static Logger spyLogger = Logger.getLogger(Constants.SPY_LOGGER_CATEGORY);

    private volatile BaseWrapperManagedConnection mc;
    private BaseWrapperManagedConnection lockedMC;
    private int lockCount;

    private WrapperDataSource dataSource;

    private HashMap<WrappedStatement, Throwable> statements;

    private boolean closed = false;

    private int trackStatements;

    /**
     * Spy functionality
     */
    protected boolean spy = false;

    /**
     * The jndi name
     */
    protected String jndiName = null;

    /**
     * Constructor
     *
     * @param mc       The managed connection
     * @param spy      The spy value
     * @param jndiName The jndi name
     */
    public WrappedConnection(final BaseWrapperManagedConnection mc, boolean spy, String jndiName) {
        setManagedConnection(mc);
        setSpy(spy);
        setJndiName(jndiName);
    }

    /**
     * Set the managed connection
     *
     * @param mc The managed connection
     */
    void setManagedConnection(final BaseWrapperManagedConnection mc) {
        this.mc = mc;

        if (mc != null)
            trackStatements = mc.getTrackStatements();
    }

    /**
     * Set the spy value
     *
     * @param v The value
     */
    void setSpy(boolean v) {
        this.spy = v;
    }

    /**
     * Set the jndi name value
     *
     * @param v The value
     */
    void setJndiName(String v) {
        this.jndiName = v;
    }

    /**
     * Lock connection
     *
     * @throws SQLException Thrown if an error occurs
     */
    protected void lock() throws SQLException {
        BaseWrapperManagedConnection mc = this.mc;
        if (mc != null) {
            mc.tryLock();
            if (lockedMC == null)
                lockedMC = mc;

            lockCount++;
        } else {
            throw new SQLException("Connection is not associated with a managed connection." + this);
        }
    }

    /**
     * Unlock connection
     */
    protected void unlock() {
        BaseWrapperManagedConnection mc = this.lockedMC;
        if (--lockCount == 0)
            lockedMC = null;

        if (mc != null)
            mc.unlock();
    }

    /**
     * Get the datasource
     *
     * @return The value
     */
    public WrapperDataSource getDataSource() {
        return dataSource;
    }

    /**
     * Set the datasource
     *
     * @param dataSource The value
     */
    protected void setDataSource(WrapperDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * {@inheritDoc}
     */
    public void setReadOnly(boolean readOnly) throws SQLException {
        lock();
        try {
            checkStatus();

            if (spy)
                spyLogger.debugf("%s [%s] setReadOnly(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, readOnly);

            mc.setJdbcReadOnly(readOnly);
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadOnly() throws SQLException {
        checkStatus();

        if (spy)
            spyLogger.debugf("%s [%s] isReadOnly()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

        return mc.isJdbcReadOnly();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws SQLException {
        closed = true;

        if (spy)
            spyLogger.debugf("%s [%s] close()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

        if (mc != null) {
            if (trackStatements != BaseWrapperManagedConnectionFactory.TRACK_STATEMENTS_FALSE_INT) {
                synchronized (this) {
                    if (statements != null && statements.size() > 0) {
                        for (Iterator<Map.Entry<WrappedStatement, Throwable>> i = statements.entrySet().iterator();
                             i.hasNext();) {
                            Map.Entry<WrappedStatement, Throwable> entry = i.next();
                            WrappedStatement ws = entry.getKey();
                            if (trackStatements == BaseWrapperManagedConnectionFactory.TRACK_STATEMENTS_TRUE_INT) {
                                Throwable stackTrace = entry.getValue();
                                log.warn("Closing a statement you left open, please do your own housekeeping", stackTrace);
                            }
                            try {
                                ws.internalClose();
                            } catch (Throwable t) {
                                log.warn("Exception trying to close statement:", t);
                            }
                        }
                    }
                }
            }
            mc.closeHandle(this);
        }
        mc = null;
        dataSource = null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed() throws SQLException {
        if (spy)
            spyLogger.debugf("%s [%s] isClosed()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

        return closed;
    }

    /**
     * Wrap statement
     *
     * @param statement The statement
     * @param spy       The spy value
     * @param jndiName  The jndi name
     * @return The wrapped statement
     */
    protected abstract WrappedStatement wrapStatement(Statement statement, boolean spy, String jndiName);

    /**
     * {@inheritDoc}
     */
    public Statement createStatement() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] createStatement()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return wrapStatement(mc.getConnection().createStatement(), spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] createStatement(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            resultSetType, resultSetConcurrency);

                return wrapStatement(mc.getConnection().createStatement(resultSetType, resultSetConcurrency),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] createStatement(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            resultSetType, resultSetConcurrency, resultSetHoldability);

                return wrapStatement(mc.getConnection()
                        .createStatement(resultSetType, resultSetConcurrency, resultSetHoldability),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * Wrap a prepared statement
     *
     * @param statement The statement
     * @param spy       The spy value
     * @param jndiName  The jndi name
     * @return The wrapped prepared statement
     */
    protected abstract WrappedPreparedStatement wrapPreparedStatement(PreparedStatement statement,
                                                                      boolean spy, String jndiName);

    /**
     * {@inheritDoc}
     */
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] prepareStatement(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, sql);

                return wrapPreparedStatement(mc.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY,
                        ResultSet.CONCUR_READ_ONLY),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] prepareStatement(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            sql, resultSetType, resultSetConcurrency);

                return wrapPreparedStatement(mc.prepareStatement(sql, resultSetType, resultSetConcurrency),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                              int resultSetHoldability) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] prepareStatement(%s, %s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            sql, resultSetType, resultSetConcurrency, resultSetHoldability);

                return wrapPreparedStatement(mc.getConnection()
                        .prepareStatement(sql, resultSetType,
                                resultSetConcurrency, resultSetHoldability),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] prepareStatement(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            sql, autoGeneratedKeys);

                return wrapPreparedStatement(mc.getConnection().prepareStatement(sql, autoGeneratedKeys),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] prepareStatement(%s, %s)"
                            , jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            sql, Arrays.toString(columnIndexes));

                return wrapPreparedStatement(mc.getConnection().prepareStatement(sql, columnIndexes),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] prepareStatement(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            sql, Arrays.toString(columnNames));

                return wrapPreparedStatement(mc.getConnection().prepareStatement(sql, columnNames),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * Wrap a callable statement
     *
     * @param statement The statement
     * @param spy       The spy value
     * @param jndiName  The jndi name
     * @return The wrapped callable statement
     */
    protected abstract WrappedCallableStatement wrapCallableStatement(CallableStatement statement,
                                                                      boolean spy, String jndiName);

    /**
     * {@inheritDoc}
     */
    public CallableStatement prepareCall(String sql) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] prepareCall(%s)", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, sql);

                return wrapCallableStatement(mc.prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] prepareCall(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            sql, resultSetType, resultSetConcurrency);

                return wrapCallableStatement(mc.prepareCall(sql, resultSetType, resultSetConcurrency),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] prepareCall(%s, %s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            sql, resultSetType, resultSetConcurrency, resultSetHoldability);

                return wrapCallableStatement(mc.getConnection()
                        .prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
                        spy, jndiName);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String nativeSQL(String sql) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] nativeSQL(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, sql);

                return mc.getConnection().nativeSQL(sql);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAutoCommit(boolean autocommit) throws SQLException {
        lock();
        try {
            checkStatus();

            if (spy)
                spyLogger.debugf("%s [%s] setAutoCommit(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, autocommit);

            mc.setJdbcAutoCommit(autocommit);
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getAutoCommit() throws SQLException {
        lock();
        try {
            checkStatus();

            if (spy)
                spyLogger.debugf("%s [%s] getAutoCommit()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

            return mc.isJdbcAutoCommit();
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void commit() throws SQLException {
        lock();
        try {
            checkTransaction();

            if (spy)
                spyLogger.debugf("%s [%s] commit()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

            mc.jdbcCommit();
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rollback() throws SQLException {
        lock();
        try {
            checkTransaction();

            if (spy)
                spyLogger.debugf("%s [%s] rollback()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

            mc.jdbcRollback();
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(Savepoint savepoint) throws SQLException {
        lock();
        try {
            checkTransaction();

            if (spy)
                spyLogger.debugf("%s [%s] rollback(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, savepoint);

            mc.jdbcRollback(savepoint);
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public DatabaseMetaData getMetaData() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] getMetaData()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return mc.getConnection().getMetaData();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setCatalog(String catalog) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setCatalog(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, catalog);

                mc.getConnection().setCatalog(catalog);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getCatalog() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] getCatalog()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return mc.getConnection().getCatalog();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setTransactionIsolation(int isolationLevel) throws SQLException {
        lock();
        try {
            checkStatus();

            if (spy)
                spyLogger.debugf("%s [%s] setTransactionIsolation(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, isolationLevel);

            mc.setJdbcTransactionIsolation(isolationLevel);
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getTransactionIsolation() throws SQLException {
        lock();
        try {
            checkStatus();

            if (spy)
                spyLogger.debugf("%s [%s] getTransactionIsolation()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

            return mc.getJdbcTransactionIsolation();
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public SQLWarning getWarnings() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] getWarnings()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return mc.getConnection().getWarnings();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clearWarnings() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] clearWarnings()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                mc.getConnection().clearWarnings();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] getTypeMap()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return mc.getConnection().getTypeMap();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setTypeMap(Map<String, Class<?>> typeMap) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setTypeMap(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, typeMap);

                mc.getConnection().setTypeMap(typeMap);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setHoldability(int holdability) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setHoldability(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, holdability);

                mc.getConnection().setHoldability(holdability);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getHoldability() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] getHoldability()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return mc.getConnection().getHoldability();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Savepoint setSavepoint() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setSavepoint()", jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return mc.getConnection().setSavepoint();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Savepoint setSavepoint(String name) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setSavepoint(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, name);

                return mc.getConnection().setSavepoint(name);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] releaseSavepoint(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION, savepoint);

                mc.getConnection().releaseSavepoint(savepoint);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }


    /**
     * {@inheritDoc}
     */
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        lock();
        try {
            Connection c = getUnderlyingConnection();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] createArrayOf(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            typeName, Arrays.toString(elements));

                return c.createArrayOf(typeName, elements);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Blob createBlob() throws SQLException {
        lock();
        try {
            Connection c = getUnderlyingConnection();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] createBlob()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return c.createBlob();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Clob createClob() throws SQLException {
        lock();
        try {
            Connection c = getUnderlyingConnection();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] createClob()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return c.createClob();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public NClob createNClob() throws SQLException {
        lock();
        try {
            Connection c = getUnderlyingConnection();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] createNClob()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return c.createNClob();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public SQLXML createSQLXML() throws SQLException {
        lock();
        try {
            Connection c = getUnderlyingConnection();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] createSQLXML()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return c.createSQLXML();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        lock();
        try {
            Connection c = getUnderlyingConnection();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] createStruct(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            typeName, Arrays.toString(attributes));

                return c.createStruct(typeName, attributes);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Properties getClientInfo() throws SQLException {
        lock();
        try {
            Connection c = getUnderlyingConnection();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] getClientInfo()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION);

                return c.getClientInfo();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getClientInfo(String name) throws SQLException {
        lock();
        try {
            Connection c = getUnderlyingConnection();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] getClientInfo(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            name);

                return c.getClientInfo(name);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isValid(int timeout) throws SQLException {
        lock();
        try {
            Connection c = getUnderlyingConnection();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] isValid(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                            timeout);

                return c.isValid(timeout);
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        try {
            lock();
            try {
                Connection c = getUnderlyingConnection();
                try {
                    if (spy)
                        spyLogger.debugf("%s [%s] setClientInfo(%s)",
                                jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                                properties);

                    c.setClientInfo(properties);
                } catch (Throwable t) {
                    throw checkException(t);
                }
            } catch (SQLClientInfoException e) {
                throw e;
            } catch (SQLException e) {
                SQLClientInfoException t = new SQLClientInfoException();
                t.initCause(e);
                throw t;
            }
        } catch (SQLException e) {
            SQLClientInfoException t = new SQLClientInfoException();
            t.initCause(e);
            throw t;
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        try {
            lock();
            try {
                Connection c = getUnderlyingConnection();
                try {
                    if (spy)
                        spyLogger.debugf("%s [%s] setClientInfo(%s, %s)",
                                jndiName, Constants.SPY_LOGGER_PREFIX_CONNECTION,
                                name, value);

                    c.setClientInfo(name, value);
                } catch (Throwable t) {
                    throw checkException(t);
                }
            } catch (SQLClientInfoException e) {
                throw e;
            } catch (SQLException e) {
                SQLClientInfoException t = new SQLClientInfoException();
                t.initCause(e);
                throw t;
            }
        } catch (SQLException e) {
            SQLClientInfoException t = new SQLClientInfoException();
            t.initCause(e);
            throw t;
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Connection getUnderlyingConnection() throws SQLException {
        lock();
        try {
            checkTransaction();
            return mc.getConnection();
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection getWrappedObject() throws SQLException {
        return getUnderlyingConnection();
    }

    /**
     * {@inheritDoc}
     */
    protected void checkTransaction() throws SQLException {
        checkStatus();
        mc.checkTransaction();
    }

    /**
     * {@inheritDoc}
     */
    void checkTransactionActive() throws SQLException {
        if (dataSource == null)
            return;
        dataSource.checkTransactionActive();
    }

    /**
     * The checkStatus method checks that the handle has not been closed and
     * that it is associated with a managed connection.
     *
     * @throws SQLException if an error occurs
     */
    protected void checkStatus() throws SQLException {
        if (closed)
            throw new SQLException("Connection handle has been closed and is unusable");
        if (mc == null)
            throw new SQLException("Connection handle is not currently associated with a ManagedConnection");
        checkTransactionActive();
    }

    /**
     * The base checkException method rethrows the supplied exception, informing
     * the ManagedConnection of the error. Subclasses may override this to
     * filter exceptions based on their severity.
     *
     * @param t a throwable
     * @return the sql exception
     * @throws SQLException if an error occurs
     */
    protected SQLException checkException(Throwable t) throws SQLException {
        Throwable result = null;
        if (t instanceof AbstractMethodError) {
            t = new SQLFeatureNotSupportedException("Method is not implemented by JDBC driver", t);
        }

        if (mc != null)
            result = mc.connectionError(t);

        if (result instanceof SQLException) {
            throw (SQLException) result;
        } else {
            throw new SQLException("Error", result);
        }

    }

    /**
     * Get the track statement status
     *
     * @return The value
     */
    int getTrackStatements() {
        return trackStatements;
    }

    /**
     * Register a statement
     *
     * @param ws The statement
     */
    void registerStatement(WrappedStatement ws) {
        if (trackStatements == BaseWrapperManagedConnectionFactory.TRACK_STATEMENTS_FALSE_INT)
            return;

        synchronized (this) {
            if (statements == null)
                statements = new HashMap<WrappedStatement, Throwable>(1);

            if (trackStatements == BaseWrapperManagedConnectionFactory.TRACK_STATEMENTS_TRUE_INT)
                statements.put(ws, new Throwable("STACKTRACE"));
            else
                statements.put(ws, null);
        }
    }

    /**
     * Unregister a statement
     *
     * @param ws The statement
     */
    void unregisterStatement(WrappedStatement ws) {
        if (trackStatements == BaseWrapperManagedConnectionFactory.TRACK_STATEMENTS_FALSE_INT)
            return;
        synchronized (this) {
            if (statements != null)
                statements.remove(ws);
        }
    }

    /**
     * Check configured query timeout
     *
     * @param ws The statement
     * @throws SQLException Thrown if an error occurs
     */
    void checkConfiguredQueryTimeout(WrappedStatement ws) throws SQLException {
        if (mc == null || dataSource == null)
            return;

        int timeout = 0;

        // Use the transaction timeout
        if (mc.isTransactionQueryTimeout())
            timeout = dataSource.getTimeLeftBeforeTransactionTimeout();

        // Look for a configured value
        if (timeout <= 0)
            timeout = mc.getQueryTimeout();

        if (timeout > 0)
            ws.setQueryTimeout(timeout);
    }

    /**
     * Get the logger
     *
     * @return The value
     */
    Logger getLogger() {
        return log;
    }
}
