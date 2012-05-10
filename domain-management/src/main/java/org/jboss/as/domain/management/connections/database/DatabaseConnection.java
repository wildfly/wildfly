/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.management.connections.database;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

/**
 * The Database connection.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnection implements Connection {

    private final DatabaseConnectionPool pool;
    private final Connection conn;
    private volatile boolean inuse;
    private volatile long timestamp;


    public DatabaseConnection(Connection conn, DatabaseConnectionPool pool) {
        this.conn=conn;
        this.pool=pool;
        this.inuse=false;
        this.timestamp=0;
    }

    private synchronized void updateTimeStamp() {
        timestamp=System.currentTimeMillis();
    }

    public synchronized boolean lease() {
       if(inuse)  {
           return false;
       } else {
          inuse=true;
          updateTimeStamp();
          return true;
       }
    }

    public boolean validate() {
        try {
            updateTimeStamp();
            conn.getMetaData();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public synchronized  boolean inUse() {
        return inuse;
    }

    public synchronized long getLastUse() {
        return timestamp;
    }

    public void close() throws SQLException {
        updateTimeStamp();
        pool.returnConnection(this);
    }

    protected synchronized void terminateConnection() throws SQLException {
        inuse=false;
        conn.close();
    }

    protected synchronized void expireLease() {
        updateTimeStamp();
        inuse=false;
    }

    protected Connection getConnection() {
        updateTimeStamp();
        return conn;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        updateTimeStamp();
        return conn.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        updateTimeStamp();
        return conn.prepareCall(sql);
    }

    @Override
    public Statement createStatement() throws SQLException {
        updateTimeStamp();
        return conn.createStatement();
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        updateTimeStamp();
        return conn.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        updateTimeStamp();
        conn.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        updateTimeStamp();
        return conn.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        updateTimeStamp();
        conn.commit();
    }

    @Override
    public void rollback() throws SQLException {
        updateTimeStamp();
        conn.rollback();
    }

    @Override
    public boolean isClosed() throws SQLException {
        updateTimeStamp();
        return conn.isClosed();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        updateTimeStamp();
        return conn.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        updateTimeStamp();
        conn.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        updateTimeStamp();
        return conn.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        updateTimeStamp();
        conn.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
        updateTimeStamp();
        return conn.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        updateTimeStamp();
        conn.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        updateTimeStamp();
        return conn.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        updateTimeStamp();
        return conn.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        updateTimeStamp();
        conn.clearWarnings();
    }

    @Override
    public boolean isWrapperFor(Class<?> clazz) throws SQLException {
        updateTimeStamp();
        return conn.isWrapperFor(clazz);
    }

    @Override
    public <T> T unwrap(Class<T> clazz) throws SQLException {
        updateTimeStamp();
        return conn.unwrap(clazz);
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        updateTimeStamp();
        return conn.createArrayOf(typeName, elements);
    }

    @Override
    public Blob createBlob() throws SQLException {
        updateTimeStamp();
        return conn.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        updateTimeStamp();
        return conn.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        updateTimeStamp();
        return conn.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        updateTimeStamp();
        return conn.createSQLXML();
    }

    @Override
    public Statement createStatement(int resultSet, int resultSetConcurrency) throws SQLException {
        updateTimeStamp();
        return conn.createStatement(resultSet,resultSetConcurrency);
    }

    @Override
    public Statement createStatement(int resultSet, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        updateTimeStamp();
        return conn.createStatement(resultSet,resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        updateTimeStamp();
        return conn.createStruct(typeName, attributes);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        updateTimeStamp();
        return conn.getClientInfo();
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        updateTimeStamp();
        return conn.getClientInfo(name);
    }

    @Override
    public int getHoldability() throws SQLException {
        updateTimeStamp();
        return conn.getHoldability();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        updateTimeStamp();
        return conn.getTypeMap();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        updateTimeStamp();
        return conn.isValid(timeout);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        updateTimeStamp();
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        updateTimeStamp();
        return conn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        updateTimeStamp();
        return conn.prepareStatement(sql,autoGeneratedKeys);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        updateTimeStamp();
        return conn.prepareStatement(sql, columnIndexes);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        updateTimeStamp();
        return conn.prepareStatement(sql, columnNames);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        updateTimeStamp();
        return conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        updateTimeStamp();
        return conn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        updateTimeStamp();
        conn.releaseSavepoint(savepoint);
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        updateTimeStamp();
        conn.rollback(savepoint);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        updateTimeStamp();
        conn.setClientInfo(properties);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        updateTimeStamp();
        conn.setClientInfo(name, value);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        updateTimeStamp();
        conn.setHoldability(holdability);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        updateTimeStamp();
        return conn.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        updateTimeStamp();
        return conn.setSavepoint(name);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        updateTimeStamp();
        conn.setTypeMap(map);
    }
}