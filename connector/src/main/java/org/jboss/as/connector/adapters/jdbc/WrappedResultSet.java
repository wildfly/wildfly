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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.logging.Logger;

/**
 * A wrapper for a result set
 *
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersne@jboss.org">Jesper Pedersen</a>
 * @version $Revision: 74335 $
 */
public abstract class WrappedResultSet extends JBossWrapper implements ResultSet {
    /**
     * The spy logger
     */
    protected static Logger spyLogger = Logger.getLogger(Constants.SPY_LOGGER_CATEGORY);

    /**
     * The wrapped statement
     */
    private WrappedStatement statement;

    /**
     * The real result set
     */
    private ResultSet resultSet;

    /**
     * Whether we are closed
     */
    private AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Spy functionality
     */
    protected final boolean spy;

    /**
     * The jndi name
     */
    protected final String jndiName;

    /**
     * Create a new wrapped result set
     *
     * @param statement the wrapped statement
     * @param resultSet the real result set
     * @param spy       The spy value
     * @param jndiName  The jndi name
     */
    public WrappedResultSet(WrappedStatement statement, ResultSet resultSet,
                            final boolean spy, final String jndiName) {
        if (statement == null)
            throw new IllegalArgumentException("Null statement!");

        if (resultSet == null)
            throw new IllegalArgumentException("Null result set!");

        this.statement = statement;
        this.resultSet = resultSet;
        this.spy = spy;
        this.jndiName = jndiName;
    }

    /**
     * {@inheritDoc}
     */
    public ResultSet getUnderlyingResultSet() throws SQLException {
        statement.lock();
        try {
            checkTransaction();
            return resultSet;
        } finally {
            statement.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean absolute(int row) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] absolute(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        row);

            return resultSet.absolute(row);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void afterLast() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] afterLast()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            resultSet.afterLast();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] beforeFirst()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            resultSet.beforeFirst();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancelRowUpdates() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] cancelRowUpdates()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            resultSet.cancelRowUpdates();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clearWarnings() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] clearWarnings()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            resultSet.clearWarnings();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws SQLException {
        if (closed.get())
            return;

        if (spy)
            spyLogger.debugf("%s [%s] close()",
                    jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

        closed.set(true);
        statement.unregisterResultSet(this);
        internalClose();
    }

    /**
     * {@inheritDoc}
     */
    public void deleteRow() throws SQLException {
        statement.lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] deleteRow()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

                resultSet.deleteRow();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            statement.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public int findColumn(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] findColumn(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.findColumn(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean first() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] first()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.first();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Array getArray(int i) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getArray(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        i);

            return resultSet.getArray(i);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Array getArray(String colName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getArray(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        colName);

            return resultSet.getArray(colName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getAsciiStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getAsciiStream(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getAsciiStream(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getAsciiStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getAsciiStream(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBigDecimal(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getBigDecimal(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBigDecimal(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, scale);

            return resultSet.getBigDecimal(columnIndex, scale);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getBigDecimal(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBigDecimal(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getBigDecimal(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBigDecimal(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, scale);

            return resultSet.getBigDecimal(columnName, scale);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBinaryStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getBinaryStream(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getBinaryStream(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBinaryStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getBinaryStream(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Blob getBlob(int i) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBlob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        i);

            return resultSet.getBlob(i);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Blob getBlob(String colName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBlob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        colName);

            return resultSet.getBlob(colName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBoolean(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getBoolean(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBoolean(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getBoolean(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte getByte(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getByte(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getByte(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte getByte(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getByte(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getByte(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getBytes(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBytes(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getBytes(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getBytes(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBytes(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getBytes(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getCharacterStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getCharacterStream(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reader getCharacterStream(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getCharacterStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getCharacterStream(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Clob getClob(int i) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getClob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        i);

            return resultSet.getClob(i);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Clob getClob(String colName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getClob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        colName);

            return resultSet.getClob(colName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getConcurrency() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getConcurrency()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.getConcurrency();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getCursorName() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getCursorName()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.getCursorName();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDate(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getDate(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDate(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, cal);

            return resultSet.getDate(columnIndex, cal);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDate(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getDate(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate(String columnName, Calendar cal) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDate(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, cal);

            return resultSet.getDate(columnName, cal);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDouble(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getDouble(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDouble(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getDouble(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getFetchDirection() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getFetchDirection()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.getFetchDirection();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getFetchSize() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getFetchSize()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.getFetchSize();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getFloat(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getFloat(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getFloat(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getFloat(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getInt(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getInt(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getInt(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getInt(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getLong(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getLong(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getLong(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getLong(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ResultSetMetaData getMetaData() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getMetaData()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.getMetaData();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getObject(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getObject(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object getObject(int i, Map<String, Class<?>> map) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getObject(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        i, map);

            return resultSet.getObject(i, map);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getObject(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getObject(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object getObject(String colName, Map<String, Class<?>> map) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getObject(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        colName, map);

            return resultSet.getObject(colName, map);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Ref getRef(int i) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getRef(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        i);

            return resultSet.getRef(i);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Ref getRef(String colName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getRef(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        colName);

            return resultSet.getRef(colName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getRow() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getRow()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.getRow();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getShort(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getShort(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getShort(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getShort(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Statement getStatement() throws SQLException {
        checkState();

        if (spy)
            spyLogger.debugf("%s [%s] getStatement()",
                    jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

        return statement;
    }

    /**
     * {@inheritDoc}
     */
    public String getString(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getString(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getString(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getString(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getString(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getString(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Time getTime(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTime(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getTime(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTime(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, cal);

            return resultSet.getTime(columnIndex, cal);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Time getTime(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTime(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getTime(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Time getTime(String columnName, Calendar cal) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTime(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, cal);

            return resultSet.getTime(columnName, cal);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTimestamp(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getTimestamp(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTimestamp(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, cal);

            return resultSet.getTimestamp(columnIndex, cal);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Timestamp getTimestamp(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTimestamp(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getTimestamp(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTimestamp(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, cal);

            return resultSet.getTimestamp(columnName, cal);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getType() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getType()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.getType();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getUnicodeStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getUnicodeStream(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public InputStream getUnicodeStream(String columnName) throws SQLException {
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getUnicodeStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getUnicodeStream(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public URL getURL(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getURL(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getURL(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public URL getURL(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getURL(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            return resultSet.getURL(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public SQLWarning getWarnings() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getWarnings()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.getWarnings();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void insertRow() throws SQLException {
        statement.lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] insertRow()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

                resultSet.insertRow();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            statement.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAfterLast() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] isAfterLast()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.isAfterLast();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isBeforeFirst() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] isBeforeFirst()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.isBeforeFirst();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFirst() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] isFirst()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.isFirst();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLast() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] isLast()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.isLast();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean last() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] last()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.last();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void moveToCurrentRow() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] moveToCurrentRow()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            resultSet.moveToCurrentRow();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void moveToInsertRow() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] moveToInsertRow()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            resultSet.moveToInsertRow();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean next() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] next()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.next();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean previous() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] previous()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.previous();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void refreshRow() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] refreshRow()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            resultSet.refreshRow();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean relative(int rows) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] relative(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        rows);

            return resultSet.relative(rows);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean rowDeleted() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] rowDeleted()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.rowDeleted();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean rowInserted() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] rowInserted()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.rowInserted();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean rowUpdated() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] rowUpdated()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.rowUpdated();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setFetchDirection(int direction) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setFetchDirection(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        direction);

            resultSet.setFetchDirection(direction);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setFetchSize(int rows) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setFetchSize(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        rows);

            resultSet.setFetchSize(rows);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateArray(int columnIndex, Array x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateArray(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateArray(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateArray(String columnName, Array x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateArray(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateArray(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateAsciiStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x, length);

            resultSet.updateAsciiStream(columnIndex, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateAsciiStream(String columnName, InputStream x, int length) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateAsciiStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x, length);

            resultSet.updateAsciiStream(columnName, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBigDecimal(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateBigDecimal(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBigDecimal(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateBigDecimal(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBinaryStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x, length);

            resultSet.updateBinaryStream(columnIndex, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBinaryStream(String columnName, InputStream x, int length) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBinaryStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x, length);

            resultSet.updateBinaryStream(columnName, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBlob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateBlob(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBlob(String columnName, Blob x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBlob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateBlob(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBoolean(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateBoolean(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBoolean(String columnName, boolean x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBoolean(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateBoolean(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateByte(int columnIndex, byte x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateByte(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateByte(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateByte(String columnName, byte x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateByte(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateByte(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBytes(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, Arrays.toString(x));

            resultSet.updateBytes(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBytes(String columnName, byte[] x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBytes(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, Arrays.toString(x));

            resultSet.updateBytes(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x, length);

            resultSet.updateCharacterStream(columnIndex, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateCharacterStream(String columnName, Reader reader, int length) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, reader, length);

            resultSet.updateCharacterStream(columnName, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateClob(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateClob(String columnName, Clob x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateClob(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateDate(int columnIndex, Date x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateDate(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateDate(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateDate(String columnName, Date x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateDate(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateDate(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateDouble(int columnIndex, double x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateDouble(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateDouble(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateDouble(String columnName, double x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateDouble(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateDouble(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateFloat(int columnIndex, float x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateFloat(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateFloat(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateFloat(String columnName, float x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateFloat(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateFloat(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateInt(int columnIndex, int x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateInt(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateInt(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateInt(String columnName, int x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateInt(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateInt(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateLong(int columnIndex, long x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateLong(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateLong(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateLong(String columnName, long x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateLong(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateLong(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNull(int columnIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNull(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            resultSet.updateNull(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNull(String columnName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNull(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName);

            resultSet.updateNull(columnName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateObject(int columnIndex, Object x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateObject(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateObject(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateObject(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x, scale);

            resultSet.updateObject(columnIndex, x, scale);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateObject(String columnName, Object x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateObject(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateObject(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateObject(String columnName, Object x, int scale) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateObject(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x, scale);

            resultSet.updateObject(columnName, x, scale);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateRef(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateRef(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateRef(String columnName, Ref x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateRef(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateRef(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateRow() throws SQLException {
        statement.lock();
        try {
            checkTransaction();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] updateRow()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

                resultSet.updateRow();
            } catch (Throwable t) {
                throw checkException(t);
            }
        } finally {
            statement.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateShort(int columnIndex, short x) throws SQLException {
        checkState();
        try {
            resultSet.updateShort(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateShort(String columnName, short x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateShort(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateShort(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateString(int columnIndex, String x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateString(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateString(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateString(String columnName, String x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateString(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateString(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateTime(int columnIndex, Time x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateTime(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateTime(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateTime(String columnName, Time x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateTime(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateTime(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateTimestamp(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateTimestamp(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateTimestamp(String columnName, Timestamp x) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateTimestamp(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnName, x);

            resultSet.updateTimestamp(columnName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean wasNull() throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] wasNull()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.wasNull();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getHoldability() throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getHoldability()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

            return resultSet.getHoldability();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNCharacterStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getNCharacterStream(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNCharacterStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel);

            return resultSet.getNCharacterStream(columnLabel);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NClob getNClob(int columnIndex) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNClob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getNClob(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NClob getNClob(String columnLabel) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNClob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel);

            return resultSet.getNClob(columnLabel);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNString(int columnIndex) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNString(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getNString(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNString(String columnLabel) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNString(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel);

            return resultSet.getNString(columnLabel);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RowId getRowId(int columnIndex) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getRowId(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getRowId(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RowId getRowId(String columnLabel) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getRowId(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel);

            return resultSet.getRowId(columnLabel);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getSQLXML(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex);

            return resultSet.getSQLXML(columnIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getSQLXML(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel);

            return resultSet.getSQLXML(columnLabel);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed() throws SQLException {
        if (spy)
            spyLogger.debugf("%s [%s] isClosed()",
                    jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET);

        ResultSet resultSet = getWrappedObject();
        if (resultSet == null)
            return true;
        try {
            return resultSet.isClosed();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateAsciiStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x, length);

            resultSet.updateAsciiStream(columnIndex, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateAsciiStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateAsciiStream(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateAsciiStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, x, length);

            resultSet.updateAsciiStream(columnLabel, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateAsciiStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, x);

            resultSet.updateAsciiStream(columnLabel, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBinaryStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x, length);

            resultSet.updateBinaryStream(columnIndex, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBinaryStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateBinaryStream(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBinaryStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, x, length);

            resultSet.updateBinaryStream(columnLabel, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBinaryStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, x);

            resultSet.updateBinaryStream(columnLabel, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBlob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, inputStream, length);

            resultSet.updateBlob(columnIndex, inputStream, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBlob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, inputStream);

            resultSet.updateBlob(columnIndex, inputStream);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBlob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, inputStream, length);

            resultSet.updateBlob(columnLabel, inputStream, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateBlob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, inputStream);

            resultSet.updateBlob(columnLabel, inputStream);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x, length);

            resultSet.updateCharacterStream(columnIndex, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateCharacterStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateCharacterStream(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, reader, length);

            resultSet.updateCharacterStream(columnLabel, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateCharacterStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, reader);

            resultSet.updateCharacterStream(columnLabel, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateClob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, reader, length);

            resultSet.updateClob(columnIndex, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, reader);

            resultSet.updateClob(columnIndex, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateClob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, reader, length);

            resultSet.updateClob(columnLabel, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, reader);

            resultSet.updateClob(columnLabel, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x, length);

            resultSet.updateNCharacterStream(columnIndex, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNCharacterStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateNCharacterStream(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, reader, length);

            resultSet.updateNCharacterStream(columnLabel, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNCharacterStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, reader);

            resultSet.updateNCharacterStream(columnLabel, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNClob(int columnIndex, NClob clob) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, clob);

            resultSet.updateNClob(columnIndex, clob);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNClob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, reader, length);

            resultSet.updateNClob(columnIndex, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, reader);

            resultSet.updateNClob(columnIndex, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNClob(String columnLabel, NClob clob) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, clob);

            resultSet.updateNClob(columnLabel, clob);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNClob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, reader, length);

            resultSet.updateNClob(columnLabel, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, reader);

            resultSet.updateNClob(columnLabel, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNString(int columnIndex, String string) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNString(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, string);

            resultSet.updateNString(columnIndex, string);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateNString(String columnLabel, String string) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateNString(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, string);

            resultSet.updateNString(columnLabel, string);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateRowId(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, x);

            resultSet.updateRowId(columnIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateRowId(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, x);

            resultSet.updateRowId(columnLabel, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateSQLXML(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnIndex, xmlObject);

            resultSet.updateSQLXML(columnIndex, xmlObject);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        ResultSet resultSet = getUnderlyingResultSet();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] updateSQLXML(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_RESULTSET,
                        columnLabel, xmlObject);

            resultSet.updateSQLXML(columnLabel, xmlObject);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected ResultSet getWrappedObject() throws SQLException {
        return getUnderlyingResultSet();
    }

    /**
     * {@inheritDoc}
     */
    protected SQLException checkException(Throwable t) throws SQLException {
        throw statement.checkException(t);
    }

    /**
     * {@inheritDoc}
     */
    void internalClose() throws SQLException {
        closed.set(true);
        resultSet.close();
    }

    /**
     * {@inheritDoc}
     */
    void checkState() throws SQLException {
        if (closed.get())
            throw new SQLException("The result set is closed.");
    }

    /**
     * {@inheritDoc}
     */
    protected void checkTransaction() throws SQLException {
        checkState();
        statement.checkTransactionActive();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o == null)
            return false;
        else if (o == this)
            return true;
        else if (o instanceof WrappedResultSet)
            return (resultSet.equals(((WrappedResultSet) o).resultSet));
        else if (o instanceof ResultSet)
            return resultSet.equals(o);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return resultSet.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return resultSet.toString();
    }
}
