/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * WildFly DataSource implementation
 *
 * @author <a href="jesper.pedersen@redhat.com">Jesper Pedersen</a>
 */
public class WildFlyDataSource implements DataSource, Serializable {

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** DataSource */
    private transient DataSource delegate;

    /** Service name */
    private transient String jndiName;


    /**
     * Constructor
     * @param delegate The datasource
     * @param jndiName The service name
     */
    public WildFlyDataSource(DataSource delegate, String jndiName) {
        this.delegate = delegate;
        this.jndiName = jndiName;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return delegate.getConnection(username, password);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("Unwrap not supported");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(jndiName);
    }


    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        jndiName = (String) in.readObject();


        try {
            InitialContext context = new InitialContext();

            DataSource originalDs = (DataSource) context.lookup(jndiName);
            this.delegate = originalDs;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
