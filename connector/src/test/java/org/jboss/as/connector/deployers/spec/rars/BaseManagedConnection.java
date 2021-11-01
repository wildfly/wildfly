/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2009, Red Hat Inc, and individual contributors
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
package org.jboss.as.connector.deployers.spec.rars;

import java.io.PrintWriter;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

import org.jboss.logging.Logger;

/**
 * BaseManagedConnection.
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>
 */
public class BaseManagedConnection implements ManagedConnection {
    private static Logger log = Logger.getLogger(BaseManagedConnection.class);

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     *
     * @param listener a new ConnectionEventListener to be registered
     **/
    public void addConnectionEventListener(ConnectionEventListener listener) {
        log.debug("call addConnectionEventListener");

    }

    /**
     * Used by the container to change the association of an application-level connection handle with a ManagedConneciton
     * instance.
     *
     * @param connection Application-level connection handle
     *
     * @throws ResourceException Failed to associate the connection handle with this ManagedConnection instance
     **/
    public void associateConnection(Object connection) throws ResourceException {
        log.debug("call associateConnection");

    }

    /**
     * Application server calls this method to force any cleanup on the ManagedConnection instance.
     *
     * @throws ResourceException generic exception if operation fails
     **/
    public void cleanup() throws ResourceException {
        log.debug("call cleanup");

    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     *
     * @throws ResourceException generic exception if operation failed
     **/
    public void destroy() throws ResourceException {
        log.debug("call destroy");

    }

    /**
     * Creates a new connection handle for the underlying physical connection represented by the ManagedConnection instance.
     *
     * @param subject security context as JAAS subject
     * @param cxRequestInfo ConnectionRequestInfo instance
     * @return generic Object instance representing the connection handle.
     * @throws ResourceException generic exception if operation fails
     *
     **/
    public Object getConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        log.debug("call getConnection");
        return null;
    }

    /**
     * Returns an <code>javax.resource.spi.LocalTransaction</code> instance.
     *
     * @return LocalTransaction instance
     *
     * @throws ResourceException generic exception if operation fails
     **/
    public LocalTransaction getLocalTransaction() throws ResourceException {
        log.debug("call getLocalTransaction");
        return null;
    }

    /**
     * Gets the log writer for this ManagedConnection instance.
     *
     * @return Character ourput stream associated with this Managed- Connection instance
     *
     * @throws ResourceException generic exception if operation fails
     **/
    public PrintWriter getLogWriter() throws ResourceException {
        log.debug("call getLogWriter");
        return null;
    }

    /**
     * <p>
     * Gets the metadata information for this connection's underlying EIS resource manager instance.
     *
     * @return ManagedConnectionMetaData instance
     *
     * @throws ResourceException generic exception if operation fails
     **/
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        log.debug("call destroy");
        return null;
    }

    /**
     * Returns an <code>javax.transaction.xa.XAresource</code> instance.
     *
     * @return XAResource instance
     *
     * @throws ResourceException generic exception if operation fails
     **/
    public XAResource getXAResource() throws ResourceException {
        log.debug("call getXAResource");
        return null;
    }

    /**
     * Removes an already registered connection event listener from the ManagedConnection instance.
     *
     * @param listener already registered connection event listener to be removed
     **/
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        log.debug("call removeConnectionEventListener");

    }

    /**
     * Sets the log writer for this ManagedConnection instance.
     *
     * @param out Character Output stream to be associated
     *
     * @throws ResourceException generic exception if operation fails
     **/
    public void setLogWriter(PrintWriter out) throws ResourceException {
        log.debug("call setLogWriter");

    }

}
