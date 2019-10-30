package org.jboss.as.test.integration.jca.classloading;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;
import java.sql.Connection;
import java.sql.SQLException;

public class TestXAConnection implements XAConnection {
    @Override
    public XAResource getXAResource() throws SQLException {
        return null;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return new TestConnection();
    }

    @Override
    public void close() throws SQLException {

    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {

    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {

    }

    @Override
    public void addStatementEventListener(StatementEventListener listener) {

    }

    @Override
    public void removeStatementEventListener(StatementEventListener listener) {

    }
}
