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
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;

/**
 * A wrapper for a prepared statement.
 *
 * @author <a href="mailto:d_jencks@users.sourceforge.net">David Jencks</a>
 * @author <a href="mailto:adrian@jboss.com">Adrian Brock</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @version $Revision: 71788 $
 */
public abstract class WrappedPreparedStatement extends WrappedStatement implements PreparedStatement {
    private final PreparedStatement ps;

    /**
     * Constructor
     *
     * @param lc       The connection
     * @param ps       The prepared statement
     * @param spy      The spy value
     * @param jndiName The jndi name
     */
    public WrappedPreparedStatement(final WrappedConnection lc, final PreparedStatement ps,
                                    boolean spy, String jndiName) {
        super(lc, ps, spy, jndiName);
        this.ps = ps;
    }

    /**
     * {@inheritDoc}
     */
    public PreparedStatement getUnderlyingStatement() throws SQLException {
        lock();
        try {
            checkState();
            if (ps instanceof CachedPreparedStatement) {
                return ((CachedPreparedStatement) ps).getUnderlyingPreparedStatement();
            } else {
                return ps;
            }
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setBoolean(int parameterIndex, boolean value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setBoolean(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setBoolean(parameterIndex, value);
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
    public void setByte(int parameterIndex, byte value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setByte(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setByte(parameterIndex, value);
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
    public void setShort(int parameterIndex, short value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setShort(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setShort(parameterIndex, value);
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
    public void setInt(int parameterIndex, int value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setInt(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setInt(parameterIndex, value);
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
    public void setLong(int parameterIndex, long value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setLong(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setLong(parameterIndex, value);
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
    public void setFloat(int parameterIndex, float value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setFloat(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setFloat(parameterIndex, value);
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
    public void setDouble(int parameterIndex, double value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setDouble(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setDouble(parameterIndex, value);
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
    public void setURL(int parameterIndex, URL value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setURL(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setURL(parameterIndex, value);
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
    public void setTime(int parameterIndex, Time value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setTime(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setTime(parameterIndex, value);
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
    public void setTime(int parameterIndex, Time value, Calendar calendar) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setTime(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value, calendar);

                ps.setTime(parameterIndex, value, calendar);
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
    public boolean execute() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                checkConfiguredQueryTimeout();

                if (spy)
                    spyLogger.debugf("%s [%s] execute()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT);

                return ps.execute();
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
    public ResultSetMetaData getMetaData() throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] getMetaData()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT);

                return ps.getMetaData();
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
    public ResultSet executeQuery() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                checkConfiguredQueryTimeout();

                if (spy)
                    spyLogger.debugf("%s [%s] executeQuery()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT);

                ResultSet resultSet = ps.executeQuery();
                return registerResultSet(resultSet);
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
    public int executeUpdate() throws SQLException {
        lock();
        try {
            checkTransaction();
            try {
                checkConfiguredQueryTimeout();

                if (spy)
                    spyLogger.debugf("%s [%s] executeUpdate()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT);

                return ps.executeUpdate();
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
    public void addBatch() throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] addBatch()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT);

                ps.addBatch();
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
    public void setNull(int parameterIndex, int sqlType) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setNull(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, sqlType);

                ps.setNull(parameterIndex, sqlType);
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
    public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setNull(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, sqlType, typeName);

                ps.setNull(parameterIndex, sqlType, typeName);
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
    public void setBigDecimal(int parameterIndex, BigDecimal value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setBigDecimal(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setBigDecimal(parameterIndex, value);
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
    public void setString(int parameterIndex, String value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setString(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setString(parameterIndex, value);
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
    public void setBytes(int parameterIndex, byte[] value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setBytes(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, Arrays.toString(value));

                ps.setBytes(parameterIndex, value);
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
    public void setDate(int parameterIndex, Date value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setDate(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setDate(parameterIndex, value);
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
    public void setDate(int parameterIndex, Date value, Calendar calendar) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setDate(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value, calendar);

                ps.setDate(parameterIndex, value, calendar);
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
    public void setTimestamp(int parameterIndex, Timestamp value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setTimestamp(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setTimestamp(parameterIndex, value);
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
    public void setTimestamp(int parameterIndex, Timestamp value, Calendar calendar) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setTimestamp(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value, calendar);

                ps.setTimestamp(parameterIndex, value, calendar);
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
    @Deprecated
    public void setAsciiStream(int parameterIndex, InputStream stream, int length) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setAsciiStream(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, stream, length);

                ps.setAsciiStream(parameterIndex, stream, length);
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
    @Deprecated
    public void setUnicodeStream(int parameterIndex, InputStream stream, int length) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setUnicodeStream(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, stream, length);

                ps.setUnicodeStream(parameterIndex, stream, length);
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
    public void setBinaryStream(int parameterIndex, InputStream stream, int length) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setBinaryStream(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, stream, length);

                ps.setBinaryStream(parameterIndex, stream, length);
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
    public void clearParameters() throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] clearParameters()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT);

                ps.clearParameters();
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
    public void setObject(int parameterIndex, Object value, int sqlType, int scale) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setObject(%s, %s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value, sqlType, scale);

                ps.setObject(parameterIndex, value, sqlType, scale);
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
    public void setObject(int parameterIndex, Object value, int sqlType) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setObject(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value, sqlType);

                ps.setObject(parameterIndex, value, sqlType);
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
    public void setObject(int parameterIndex, Object value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setObject(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setObject(parameterIndex, value);
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
    public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setCharacterStream(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, reader, length);

                ps.setCharacterStream(parameterIndex, reader, length);
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
    public void setRef(int parameterIndex, Ref value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setRef(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setRef(parameterIndex, value);
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
    public void setBlob(int parameterIndex, Blob value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setBlob(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setBlob(parameterIndex, value);
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
    public void setClob(int parameterIndex, Clob value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setClob(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setClob(parameterIndex, value);
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
    public void setArray(int parameterIndex, Array value) throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setArray(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                ps.setArray(parameterIndex, value);
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
    public ParameterMetaData getParameterMetaData() throws SQLException {
        lock();
        try {
            checkState();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] getParameterMetaData()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT);

                return ps.getParameterMetaData();
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
    public boolean isClosed() throws SQLException {
        lock();
        try {
            if (spy)
                spyLogger.debugf("%s [%s] isClosed()",
                        jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT);

            PreparedStatement wrapped = getWrappedObject();
            if (wrapped == null)
                return true;
            return wrapped.isClosed();
        } catch (Throwable t) {
            throw checkException(t);
        } finally {
            unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPoolable() throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] isPoolable()",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT);

                return statement.isPoolable();
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
    public void setPoolable(boolean poolable) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setPoolable(%s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            poolable);

                statement.setPoolable(poolable);
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
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setAsciiStream(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, x, length);

                statement.setAsciiStream(parameterIndex, x, length);
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
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setAsciiStream(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, x);

                statement.setAsciiStream(parameterIndex, x);
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
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setBinaryStream(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, x, length);

                statement.setBinaryStream(parameterIndex, x, length);
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
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setBinaryStream(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, x);

                statement.setBinaryStream(parameterIndex, x);
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
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setBlob(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, inputStream, length);

                statement.setBlob(parameterIndex, inputStream, length);
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
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setBlob(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, inputStream);

                statement.setBlob(parameterIndex, inputStream);
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
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setCharacterStream(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, reader, length);

                statement.setCharacterStream(parameterIndex, reader, length);
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
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setCharacterStream(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, reader);

                statement.setCharacterStream(parameterIndex, reader);
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
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setClob(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, reader, length);

                statement.setClob(parameterIndex, reader, length);
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
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setClob(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, reader);

                statement.setClob(parameterIndex, reader);
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
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setNCharacterStream(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value, length);

                statement.setNCharacterStream(parameterIndex, value, length);
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
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setNCharacterStream(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                statement.setNCharacterStream(parameterIndex, value);
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
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setNClob(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                statement.setNClob(parameterIndex, value);
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
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setNClob(%s, %s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, reader, length);

                statement.setNClob(parameterIndex, reader, length);
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
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setNClob(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, reader);

                statement.setNClob(parameterIndex, reader);
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
    public void setNString(int parameterIndex, String value) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setNString(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, value);

                statement.setNString(parameterIndex, value);
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
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setRowId(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, x);

                statement.setRowId(parameterIndex, x);
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
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        lock();
        try {
            PreparedStatement statement = getUnderlyingStatement();
            try {
                if (spy)
                    spyLogger.debugf("%s [%s] setSQLXML(%s, %s)",
                            jndiName, Constants.SPY_LOGGER_PREFIX_PREPARED_STATEMENT,
                            parameterIndex, xmlObject);

                statement.setSQLXML(parameterIndex, xmlObject);
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
    protected PreparedStatement getWrappedObject() throws SQLException {
        return (PreparedStatement) super.getWrappedObject();
    }
}
