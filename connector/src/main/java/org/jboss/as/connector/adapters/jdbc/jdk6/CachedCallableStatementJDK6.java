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

import org.jboss.as.connector.adapters.jdbc.CachedCallableStatement;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * CachedCallableStatementJDK6.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 85945 $
 */
@SuppressWarnings("deprecation")
public class CachedCallableStatementJDK6 extends CachedCallableStatement {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param cs The callable statement
     * @throws SQLException Thrown if an error occurs
     */
    public CachedCallableStatementJDK6(CallableStatement cs) throws SQLException {
        super(cs);
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

    /**
     * {@inheritDoc}
     */
    public Reader getCharacterStream(int parameterIndex) throws SQLException {
        return getWrappedObject().getCharacterStream(parameterIndex);
    }

    /**
     * {@inheritDoc}
     */
    public Reader getCharacterStream(String parameterName) throws SQLException {
        return getWrappedObject().getCharacterStream(parameterName);
    }

    /**
     * {@inheritDoc}
     */
    public Reader getNCharacterStream(int parameterIndex) throws SQLException {
        return getWrappedObject().getNCharacterStream(parameterIndex);
    }

    /**
     * {@inheritDoc}
     */
    public Reader getNCharacterStream(String parameterName) throws SQLException {
        return getWrappedObject().getNCharacterStream(parameterName);
    }

    /**
     * {@inheritDoc}
     */
    public NClob getNClob(int parameterIndex) throws SQLException {
        return getWrappedObject().getNClob(parameterIndex);
    }

    /**
     * {@inheritDoc}
     */
    public NClob getNClob(String parameterName) throws SQLException {
        return getWrappedObject().getNClob(parameterName);
    }

    /**
     * {@inheritDoc}
     */
    public String getNString(int parameterIndex) throws SQLException {
        return getWrappedObject().getNString(parameterIndex);
    }

    /**
     * {@inheritDoc}
     */
    public String getNString(String parameterName) throws SQLException {
        return getWrappedObject().getNString(parameterName);
    }

    /**
     * {@inheritDoc}
     */
    public RowId getRowId(int parameterIndex) throws SQLException {
        return getWrappedObject().getRowId(parameterIndex);
    }

    /**
     * {@inheritDoc}
     */
    public RowId getRowId(String parameterName) throws SQLException {
        return getWrappedObject().getRowId(parameterName);
    }

    /**
     * {@inheritDoc}
     */
    public SQLXML getSQLXML(int parameterIndex) throws SQLException {
        return getWrappedObject().getSQLXML(parameterIndex);
    }

    /**
     * {@inheritDoc}
     */
    public SQLXML getSQLXML(String parameterName) throws SQLException {
        return getWrappedObject().getSQLXML(parameterName);
    }

    /**
     * {@inheritDoc}
     */
    public void setAsciiStream(String parameterName, InputStream x, long length) throws SQLException {
        getWrappedObject().setAsciiStream(parameterName, x, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setAsciiStream(String parameterName, InputStream x) throws SQLException {
        getWrappedObject().setAsciiStream(parameterName, x);
    }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStream(String parameterName, InputStream x, long length) throws SQLException {
        getWrappedObject().setBinaryStream(parameterName, x, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStream(String parameterName, InputStream x) throws SQLException {
        getWrappedObject().setBinaryStream(parameterName, x);
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(String parameterName, Blob x) throws SQLException {
        getWrappedObject().setBlob(parameterName, x);
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(String parameterName, InputStream inputStream, long length) throws SQLException {
        getWrappedObject().setBlob(parameterName, inputStream, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setBlob(String parameterName, InputStream inputStream) throws SQLException {
        getWrappedObject().setBlob(parameterName, inputStream);
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterStream(String parameterName, Reader reader, long length) throws SQLException {
        getWrappedObject().setCharacterStream(parameterName, reader, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setCharacterStream(String parameterName, Reader reader) throws SQLException {
        getWrappedObject().setCharacterStream(parameterName, reader);
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(String parameterName, Clob x) throws SQLException {
        getWrappedObject().setClob(parameterName, x);
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(String parameterName, Reader reader, long length) throws SQLException {
        getWrappedObject().setClob(parameterName, reader, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setClob(String parameterName, Reader reader) throws SQLException {
        getWrappedObject().setClob(parameterName, reader);
    }

    /**
     * {@inheritDoc}
     */
    public void setNCharacterStream(String parameterName, Reader value, long length) throws SQLException {
        getWrappedObject().setNCharacterStream(parameterName, value, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setNCharacterStream(String parameterName, Reader value) throws SQLException {
        getWrappedObject().setNCharacterStream(parameterName, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(String parameterName, NClob value) throws SQLException {
        getWrappedObject().setNClob(parameterName, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(String parameterName, Reader reader, long length) throws SQLException {
        getWrappedObject().setNClob(parameterName, reader, length);
    }

    /**
     * {@inheritDoc}
     */
    public void setNClob(String parameterName, Reader reader) throws SQLException {
        getWrappedObject().setNClob(parameterName, reader);
    }

    /**
     * {@inheritDoc}
     */
    public void setNString(String parameterName, String value) throws SQLException {
        getWrappedObject().setNString(parameterName, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setRowId(String parameterName, RowId x) throws SQLException {
        getWrappedObject().setRowId(parameterName, x);
    }

    /**
     * {@inheritDoc}
     */
    public void setSQLXML(String parameterName, SQLXML xmlObject) throws SQLException {
        getWrappedObject().setSQLXML(parameterName, xmlObject);
    }
}
