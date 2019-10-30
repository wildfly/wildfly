/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.DissociatableManagedConnection;
import javax.resource.spi.LazyEnlistableConnectionManager;
import javax.resource.spi.LazyEnlistableManagedConnection;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public class LazyManagedConnection implements ManagedConnection, DissociatableManagedConnection,
        LazyEnlistableManagedConnection {

    private static Logger logger = Logger.getLogger(LazyManagedConnection.class);

    private boolean localTransaction;
    private boolean xaTransaction;
    private boolean enlisted;
    private LazyConnectionImpl connection;
    private List<ConnectionEventListener> listeners;
    private PrintWriter logwriter;
    private LazyLocalTransaction lazyLocalTransaction;
    private LazyXAResource lazyXAResource;
    private ConnectionManager cm;
    private LazyManagedConnectionFactory mcf;

    public LazyManagedConnection(boolean localTransaction, boolean xaTransaction, ConnectionManager cm, LazyManagedConnectionFactory mcf) {
        logger.trace("#LazyManagedConnection");

        this.localTransaction = localTransaction;
        this.xaTransaction = xaTransaction;
        this.cm = cm;
        this.mcf = mcf;
        this.enlisted = false;
        this.listeners = Collections.synchronizedList(new ArrayList<ConnectionEventListener>(1));

        if (localTransaction) { this.lazyLocalTransaction = new LazyLocalTransaction(this); }
        if (xaTransaction) { this.lazyXAResource = new LazyXAResource(this); }
    }

    @Override
    public void dissociateConnections() throws ResourceException {
        logger.trace("#LazyManagedConnection.dissociateConnections");
        if (connection != null) {
            connection.setManagedConnection(null);
            connection = null;
        }

    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        logger.trace("#LazyManagedConnection.getConnection");

        if (connection != null) {
            connection.setManagedConnection(null);
        }

        connection = new LazyConnectionImpl(cm, this, mcf, cri);
        return connection;
    }

    @Override
    public void destroy() throws ResourceException {
        logger.trace("#LazyManagedConnection.destroy");
    }

    @Override
    public void cleanup() throws ResourceException {
        logger.trace("#LazyManagedConnection.cleanup");
        if (connection != null) {
            connection.setManagedConnection(null);
            connection = null;
        }
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        logger.trace("#LazyManagedConnection.associateConnection");
        if (this.connection != null) {
            this.connection.setManagedConnection(null);
        }

        if (connection != null) {
            if (!(connection instanceof LazyConnectionImpl)) { throw new ResourceException("Connection isn't LazyConnectionImpl: " + connection.getClass().getName()); }

            this.connection = (LazyConnectionImpl) connection;
            this.connection.setManagedConnection(this);
        } else {
            this.connection = null;
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        logger.trace("#LazyManagedConnection.addConnectionEventListener");
        if (listener == null) { throw new IllegalArgumentException("Listener is null"); }
        listeners.add(listener);

    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        logger.trace("#LazyManagedConnection.removeConnectionEventListener");
        if (listener == null) { throw new IllegalArgumentException("Listener is null"); }
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        logger.trace("#LazyManagedConnection.getXAResource");
        if (!xaTransaction || localTransaction) {
            throw new NotSupportedException("GetXAResource not supported not supported");
        } else {
            return lazyXAResource;
        }
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        logger.trace("#LazyManagedConnection.getLocalTransaction");
        if (!localTransaction || xaTransaction) {
            throw new NotSupportedException("LocalTransaction not supported");
        } else {
            return lazyLocalTransaction;
        }
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        logger.trace("#LazyManagedConnection.getMetaData");
        return new LazyManagedConnectionMetaData();
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws ResourceException {
        logger.trace("#LazyManagedConnection.setLogWriter");
        logwriter = printWriter;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        logger.trace("#LazyManagedConnection.getLogWriter");
        return logwriter;
    }

    boolean isEnlisted() {
        logger.trace("#LazyManagedConnection.isEnlisted");
        return enlisted;
    }

    void setEnlisted(boolean enlisted) {
        logger.trace("#LazyManagedConnection.setEnlisted");
        this.enlisted = enlisted;
    }

    boolean enlist() {
        logger.trace("#LazyManagedConnection.enlist");
        try {
            if (cm instanceof LazyEnlistableConnectionManager) {
                LazyEnlistableConnectionManager lecm = (LazyEnlistableConnectionManager) cm;
                lecm.lazyEnlist(this);
                return true;
            }
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
        return false;
    }

    void closeHandle(LazyConnection handle) {
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for (ConnectionEventListener cel : listeners) {
            cel.connectionClosed(event);
        }
    }
}
