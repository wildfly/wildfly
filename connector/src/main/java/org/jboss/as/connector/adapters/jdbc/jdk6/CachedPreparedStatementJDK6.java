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

package org.jboss.as.connector.adapters.jdbc.jdk6;

import org.jboss.as.connector.adapters.jdbc.CachedPreparedStatement;

import java.io.InputStream;
import java.io.Reader;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * CachedPreparedStatementJDK5.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 85945 $
 */
public class CachedPreparedStatementJDK6 extends CachedPreparedStatement {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param ps The prepared statement
     * @throws SQLException Thrown if an error occurs
     */
    public CachedPreparedStatementJDK6(PreparedStatement ps) throws SQLException {
        super(ps);
    }

    /**
     * {@inheritDoc}
     */
    public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
        getWrappedObject().setAsciiStream(parameterIndex, x, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
        getWrappedObject().setAsciiStream(parameterIndex, x);
    }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
        getWrappedObject().setBinaryStream(parameterIndex, x, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
        getWrappedObject().setBinaryStream(parameterIndex, x);
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(int parameterIndex, InputStream inputStream, long length) throws SQLException {
        getWrappedObject().setBlob(parameterIndex, inputStream, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(int parameterIndex, InputStream inputStream) throws SQLException {
        getWrappedObject().setBlob(parameterIndex, inputStream);
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
        getWrappedObject().setCharacterStream(parameterIndex, reader, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
        getWrappedObject().setCharacterStream(parameterIndex, reader);
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
        getWrappedObject().setClob(parameterIndex, reader, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(int parameterIndex, Reader reader) throws SQLException {
        getWrappedObject().setClob(parameterIndex, reader);
    }

    /**
     * {@inheritDoc}
     */
    public void setNCharacterStream(int parameterIndex, Reader value, long length) throws SQLException {
        getWrappedObject().setNCharacterStream(parameterIndex, value, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setNCharacterStream(int parameterIndex, Reader value) throws SQLException {
        getWrappedObject().setNCharacterStream(parameterIndex, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(int parameterIndex, NClob value) throws SQLException {
        getWrappedObject().setNClob(parameterIndex, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
        getWrappedObject().setNClob(parameterIndex, reader, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(int parameterIndex, Reader reader) throws SQLException {
        getWrappedObject().setNClob(parameterIndex, reader);
    }

    /**
     * {@inheritDoc}
     */
    public void setNString(int parameterIndex, String value) throws SQLException {
        getWrappedObject().setNString(parameterIndex, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setRowId(int parameterIndex, RowId x) throws SQLException {
        getWrappedObject().setRowId(parameterIndex, x);
    }

    /**
     * {@inheritDoc}
     */
    public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
        getWrappedObject().setSQLXML(parameterIndex, xmlObject);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isClosed() throws SQLException {
        return getWrappedObject().isClosed();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPoolable() throws SQLException {
        return getWrappedObject().isPoolable();
    }

    /**
     * {@inheritDoc}
     */
    public void setPoolable(boolean poolable) throws SQLException {
        getWrappedObject().setPoolable(poolable);
    }
}
