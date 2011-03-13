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
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;

/**
 * WrappedCallableStatement
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @version $Revision: 71788 $
 */
public abstract class WrappedCallableStatement extends WrappedPreparedStatement implements CallableStatement {
    private final CallableStatement cs;

    /**
     * Constructor
     *
     * @param lc       The connection
     * @param cs       The callable statement
     * @param spy      The spy value
     * @param jndiName The jndi name
     */
    public WrappedCallableStatement(final WrappedConnection lc, final CallableStatement cs,
                                    boolean spy, String jndiName) {
        super(lc, cs, spy, jndiName);
        this.cs = cs;
    }

    /**
     * {@inheritDoc}
     */
    public CallableStatement getUnderlyingStatement() throws SQLException {
        return (CallableStatement) super.getUnderlyingStatement();
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getObject(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getObject(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject(int parameterIndex, Map<String, Class<?>> typeMap) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getObject(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, typeMap);

            return cs.getObject(parameterIndex, typeMap);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getObject(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getObject(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject(String parameterName, Map<String, Class<?>> typeMap) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getObject(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, typeMap);

            return cs.getObject(parameterName, typeMap);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBoolean(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getBoolean(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBoolean(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBoolean(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getBoolean(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte getByte(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getByte(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getByte(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte getByte(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getByte(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getByte(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getShort(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getShort(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public short getShort(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getShort(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getShort(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getInt(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getInt(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getInt(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getInt(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getInt(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getLong(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getLong(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLong(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getLong(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getLong(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getFloat(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getFloat(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getFloat(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getFloat(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDouble(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getDouble(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDouble(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getDouble(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getBytes(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBytes(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getBytes(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getBytes(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBytes(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getBytes(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public URL getURL(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getURL(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getURL(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public URL getURL(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getURL(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getURL(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getString(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getString(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getString(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getString(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getString(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getString(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Ref getRef(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getRef(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getRef(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Ref getRef(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getRef(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getRef(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Time getTime(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTime(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getTime(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Time getTime(int parameterIndex, Calendar calendar) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTime(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, calendar);

            return cs.getTime(parameterIndex, calendar);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Time getTime(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTime(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getTime(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Time getTime(String parameterName, Calendar calendar) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTime(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, calendar);

            return cs.getTime(parameterName, calendar);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDate(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getDate(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate(int parameterIndex, Calendar calendar) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDate(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, calendar);

            return cs.getDate(parameterIndex, calendar);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDate(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getDate(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Date getDate(String parameterName, Calendar calendar) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getDate(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, calendar);

            return cs.getDate(parameterName, calendar);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerOutParameter(int parameterIndex, int sqlType) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] registerOutParameter(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, sqlType);

            cs.registerOutParameter(parameterIndex, sqlType);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerOutParameter(int parameterIndex, int sqlType, int scale) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] registerOutParameter(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, sqlType, scale);

            cs.registerOutParameter(parameterIndex, sqlType, scale);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerOutParameter(int parameterIndex, int sqlType, String typeName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] registerOutParameter(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, sqlType, typeName);

            cs.registerOutParameter(parameterIndex, sqlType, typeName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerOutParameter(String parameterName, int sqlType) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] registerOutParameter(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, sqlType);

            cs.registerOutParameter(parameterName, sqlType);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerOutParameter(String parameterName, int sqlType, int scale) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] registerOutParameter(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, sqlType, scale);

            cs.registerOutParameter(parameterName, sqlType, scale);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerOutParameter(String parameterName, int sqlType, String typeName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] registerOutParameter(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, sqlType, typeName);

            cs.registerOutParameter(parameterName, sqlType, typeName);
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
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT);

            return cs.wasNull();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public BigDecimal getBigDecimal(int parameterIndex, int scale) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBigDecimal(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, scale);

            return cs.getBigDecimal(parameterIndex, scale);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBigDecimal(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getBigDecimal(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBigDecimal(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getBigDecimal(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTimestamp(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getTimestamp(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Timestamp getTimestamp(int parameterIndex, Calendar calendar) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTimestamp(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, calendar);

            return cs.getTimestamp(parameterIndex, calendar);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Timestamp getTimestamp(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTimestamp(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getTimestamp(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Timestamp getTimestamp(String parameterName, Calendar calendar) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getTimestamp(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, calendar);

            return cs.getTimestamp(parameterName, calendar);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Blob getBlob(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBlob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getBlob(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Blob getBlob(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getBlob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getBlob(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Clob getClob(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getClob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getClob(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Clob getClob(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getClob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getClob(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Array getArray(int parameterIndex) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getArray(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return cs.getArray(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Array getArray(String parameterName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getArray(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return cs.getArray(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed() throws SQLException {
        try {
            if (spy)
                spyLogger.debugf("%s [%s] isClosed()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT);

            PreparedStatement wrapped = getWrappedObject();
            if (wrapped == null)
                return true;
            return wrapped.isClosed();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPoolable() throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] isPoolable()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT);

            return statement.isPoolable();
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setPoolable(boolean poolable) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setPoolable(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        poolable);

            statement.setPoolable(poolable);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBoolean(String parameterName, boolean value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBoolean(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setBoolean(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setByte(String parameterName, byte value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setByte(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setByte(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setShort(String parameterName, short value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setShort(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setShort(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setInt(String parameterName, int value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setInt(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setInt(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setLong(String parameterName, long value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setLong(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setLong(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setFloat(String parameterName, float value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setFloat(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setFloat(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDouble(String parameterName, double value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setDouble(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setDouble(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setURL(String parameterName, URL value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setURL(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setURL(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setTime(String parameterName, Time value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setTime(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setTime(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setTime(String parameterName, Time value, Calendar calendar) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setTime(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value, calendar);

            cs.setTime(parameterName, value, calendar);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNull(String parameterName, int value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNull(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setNull(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNull(String parameterName, int sqlType, String typeName) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNull(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, sqlType, typeName);

            cs.setNull(parameterName, sqlType, typeName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBigDecimal(String parameterName, BigDecimal value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBigDecimal(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setBigDecimal(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setString(String parameterName, String value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setString(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setString(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBytes(String parameterName, byte[] value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBytes(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, Arrays.toString(value));

            cs.setBytes(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDate(String parameterName, Date value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setDate(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setDate(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDate(String parameterName, Date value, Calendar calendar) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setDate(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value, calendar);

            cs.setDate(parameterName, value, calendar);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setTimestamp(String parameterName, Timestamp value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setTimestamp(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setTimestamp(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setTimestamp(String parameterName, Timestamp value, Calendar calendar) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setTimestamp(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value, calendar);

            cs.setTimestamp(parameterName, value, calendar);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAsciiStream(String parameterName, InputStream stream, int length) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setAsciiStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, stream, length);

            cs.setAsciiStream(parameterName, stream, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStream(String parameterName, InputStream stream, int length) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBinaryStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, stream, length);

            cs.setBinaryStream(parameterName, stream, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setObject(String parameterName, Object value, int sqlType, int scale) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setObject(%s, %s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value, sqlType, scale);

            cs.setObject(parameterName, value, sqlType, scale);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setObject(String parameterName, Object value, int sqlType) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setObject(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value, sqlType);

            cs.setObject(parameterName, value, sqlType);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setObject(String parameterName, Object value) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setObject(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            cs.setObject(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterStream(String parameterName, Reader reader, int length) throws SQLException {
        checkState();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, reader, length);

            cs.setCharacterStream(parameterName, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setAsciiStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, x, length);

            statement.setAsciiStream(parameterIndex, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setAsciiStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, x);

            statement.setAsciiStream(parameterIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBinaryStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, x, length);

            statement.setBinaryStream(parameterIndex, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBinaryStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, x);

            statement.setBinaryStream(parameterIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBlob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, inputStream, length);

            statement.setBlob(parameterIndex, inputStream, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBlob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, inputStream);

            statement.setBlob(parameterIndex, inputStream);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, reader, length);

            statement.setCharacterStream(parameterIndex, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setCharacterStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, reader);

            statement.setCharacterStream(parameterIndex, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setClob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, reader, length);

            statement.setClob(parameterIndex, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, reader);

            statement.setClob(parameterIndex, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, value, length);

            statement.setNCharacterStream(parameterIndex, value, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNCharacterStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, value);

            statement.setNCharacterStream(parameterIndex, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, value);

            statement.setNClob(parameterIndex, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNClob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, reader, length);

            statement.setNClob(parameterIndex, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, reader);

            statement.setNClob(parameterIndex, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNString(int parameterIndex, String value) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNString(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, value);

            statement.setNString(parameterIndex, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setRowId(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, x);

            statement.setRowId(parameterIndex, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        PreparedStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setSQLXML(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex, xmlObject);

            statement.setSQLXML(parameterIndex, xmlObject);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getCharacterStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return statement.getCharacterStream(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reader getCharacterStream(String parameterName) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getCharacterStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return statement.getCharacterStream(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNCharacterStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return statement.getNCharacterStream(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNCharacterStream(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return statement.getCharacterStream(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NClob getNClob(int parameterIndex) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNClob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return statement.getNClob(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public NClob getNClob(String parameterName) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNClob(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return statement.getNClob(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNString(int parameterIndex) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNString(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return statement.getNString(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNString(String parameterName) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getNString(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return statement.getNString(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RowId getRowId(int parameterIndex) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getRowId(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return statement.getRowId(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RowId getRowId(String parameterName) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getRowId(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return statement.getRowId(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getSQLXML(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterIndex);

            return statement.getSQLXML(parameterIndex);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] getSQLXML(%s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName);

            return statement.getSQLXML(parameterName);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setAsciiStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, x, length);

            statement.setAsciiStream(parameterName, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setAsciiStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, x);

            statement.setAsciiStream(parameterName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBinaryStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, x, length);

            statement.setBinaryStream(parameterName, x, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBinaryStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, x);

            statement.setBinaryStream(parameterName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(String parameterName, Blob x) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBlob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, x);

            statement.setBlob(parameterName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBlob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, inputStream, length);

            statement.setBlob(parameterName, inputStream, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setBlob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, inputStream);

            statement.setBlob(parameterName, inputStream);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, reader, length);

            statement.setCharacterStream(parameterName, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setCharacterStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, reader);

            statement.setCharacterStream(parameterName, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(String parameterName, Clob x) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, x);

            statement.setClob(parameterName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setClob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, reader, length);

            statement.setClob(parameterName, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(String parameterName, Reader reader) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, reader);

            statement.setClob(parameterName, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNCharacterStream(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value, length);

            statement.setNCharacterStream(parameterName, value, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNCharacterStream(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            statement.setNCharacterStream(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(String parameterName, NClob value) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            statement.setNClob(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNClob(%s, %s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, reader, length);

            statement.setNClob(parameterName, reader, length);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(String parameterName, Reader reader) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNClob(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, reader);

            statement.setNClob(parameterName, reader);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNString(String parameterName, String value) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setNString(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, value);

            statement.setNString(parameterName, value);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRowId(String parameterName, RowId x) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setRowId(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, x);

            statement.setRowId(parameterName, x);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        CallableStatement statement = getUnderlyingStatement();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] setSQLXML(%s, %s)",
                        jndiName, Constants.SPY_LOGGER_PREFIX_CALLABLE_STATEMENT,
                        parameterName, xmlObject);

            statement.setSQLXML(parameterName, xmlObject);
        } catch (Throwable t) {
            throw checkException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected CallableStatement getWrappedObject() throws SQLException {
        return (CallableStatement) super.getWrappedObject();
    }
}
