/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.rar;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

/**
 * User: jpai
 */
public class HelloWorldManagedConnection implements ManagedConnection {


    /**
     * MCF
     */
    private HelloWorldManagedConnectionFactory mcf;


    /**
     * Listeners
     */
    private List<javax.resource.spi.ConnectionEventListener> listeners;


    /**
     * Connection
     */
    private Object connection;

    private PrintWriter writer;


    /**
     * default constructor
     *
     * @param mcf mcf
     */
    public HelloWorldManagedConnection(HelloWorldManagedConnectionFactory mcf) {
        this.mcf = mcf;
        this.listeners = new ArrayList<javax.resource.spi.ConnectionEventListener>();
        this.connection = null;

    }


    /**
     * Creates a new connection handle for the underlying physical connection
     * <p/>
     * represented by the ManagedConnection instance.
     *
     * @param subject       Security context as JAAS subject
     * @param cxRequestInfo ConnectionRequestInfo instance
     * @return generic Object instance representing the connection handle.
     * @throws ResourceException generic exception if operation fails
     */

    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        connection = new HelloWorldConnectionImpl(this, mcf);
        return connection;
    }


    /**
     * Used by the container to change the association of an
     * <p/>
     * application-level connection handle with a ManagedConneciton instance.
     *
     * @param connection Application-level connection handle
     * @throws ResourceException generic exception if operation fails
     */

    public void associateConnection(Object connection) throws ResourceException {

        this.connection = connection;

    }


    /**
     * Application server calls this method to force any cleanup on
     * <p/>
     * the ManagedConnection instance.
     *
     * @throws ResourceException generic exception if operation fails
     */

    public void cleanup() throws ResourceException {

    }


    /**
     * Destroys the physical connection to the underlying resource manager.
     *
     * @throws ResourceException generic exception if operation fails
     */

    public void destroy() throws ResourceException {

        this.connection = null;

    }


    /**
     * Adds a connection event listener to the ManagedConnection instance.
     *
     * @param listener A new ConnectionEventListener to be registered
     */

    public void addConnectionEventListener(ConnectionEventListener listener) {

        if (listener == null)
            throw new IllegalArgumentException("Listener is null");

        listeners.add(listener);

    }


    /**
     * Removes an already registered connection event listener
     * <p/>
     * from the ManagedConnection instance.
     *
     * @param listener Already registered connection event listener to be removed
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {

        if (listener == null)
            throw new IllegalArgumentException("Listener is null");

        listeners.remove(listener);

    }


    /**
     * Returns an <code>javax.resource.spi.LocalTransaction</code> instance.
     *
     * @return LocalTransaction instance
     * @throws ResourceException generic exception if operation fails
     */

    public LocalTransaction getLocalTransaction() throws ResourceException {

        throw new NotSupportedException("LocalTransaction not supported");

    }


    /**
     * Returns an <code>javax.transaction.xa.XAresource</code> instance.
     *
     * @return XAResource instance
     * @throws ResourceException generic exception if operation fails
     */

    public XAResource getXAResource() throws ResourceException {

        throw new NotSupportedException("GetXAResource not supported");
    }


    /**
     * Gets the metadata information for this connection's underlying
     * <p/>
     * EIS resource manager instance.
     *
     * @return ManagedConnectionMetaData instance
     * @throws ResourceException generic exception if operation fails
     */
    public ManagedConnectionMetaData getMetaData() throws ResourceException {

        return new HelloWorldManagedConnectionMetaData();

    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws ResourceException {
        this.writer = printWriter;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return this.writer;
    }


    /**
     * Close handle
     *
     * @param handle The handle
     */

    void closeHandle(HelloWorldConnection handle) {

        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for (javax.resource.spi.ConnectionEventListener cel : listeners) {
            cel.connectionClosed(event);
        }
    }

}
