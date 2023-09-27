/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars;

import java.io.PrintWriter;
import java.util.Set;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.ResourceAdapter;
import jakarta.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

import org.jboss.logging.Logger;

/**
 * BaseManagedConnectionFactory
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>.
 */
public class BaseManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {
    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(BaseManagedConnectionFactory.class);

    private ResourceAdapter ra;
    private PrintWriter logwriter;

    /**
     * Constructor
     */
    public BaseManagedConnectionFactory() {
        ra = null;
        logwriter = null;
    }

    /**
     * Creates a Connection Factory instance.
     *
     * @param cxManager ConnectionManager to be associated with created EIS connection factory instance
     * @return EIS-specific Connection Factory instance or jakarta.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        if (ra == null)
            throw new IllegalStateException("RA is null");

        log.debug("call createConnectionFactory");
        return new BaseCciConnectionFactory();
    }

    /**
     * Creates a Connection Factory instance.
     *
     * @return EIS-specific Connection Factory instance or jakarta.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */
    public Object createConnectionFactory() throws ResourceException {
        if (ra == null)
            throw new IllegalStateException("RA is null");

        log.debug("call createConnectionFactory");
        return createConnectionFactory(new BaseConnectionManager());
    }

    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     *
     * @param subject Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection request information
     * @throws ResourceException generic exception
     * @return ManagedConnection instance
     */
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        if (ra == null)
            throw new IllegalStateException("RA is null");

        log.debug("call createManagedConnection");
        return null;
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     *
     * @param connectionSet candidate connection set
     * @param subject caller's security information
     * @param cxRequestInfo additional resource adapter specific connection request information
     *
     * @throws ResourceException generic exception
     * @return ManagedConnection if resource adapter finds an acceptable match otherwise null
     **/
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        if (ra == null)
            throw new IllegalStateException("RA is null");

        log.debug("call matchManagedConnections");
        return null;
    }

    /**
     * Get the log writer for this ManagedConnectionFactory instance.
     *
     * @return PrintWriter
     * @throws ResourceException generic exception
     */
    public PrintWriter getLogWriter() throws ResourceException {
        log.debug("call getLogWriter");
        return logwriter;
    }

    /**
     * Set the log writer for this ManagedConnectionFactory instance.
     * </p>
     *
     * @param out PrintWriter - an out stream for error logging and tracing
     * @throws ResourceException generic exception
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        log.debug("call setLogWriter");
        logwriter = out;
    }

    /**
     * Get the resource adapter
     *
     * @return The handle
     */
    public ResourceAdapter getResourceAdapter() {
        log.debug("call getResourceAdapter");
        return ra;
    }

    /**
     * Set the resource adapter
     *
     * @param ra The handle
     */
    public void setResourceAdapter(ResourceAdapter ra) {
        log.debugf("call setResourceAdapter(%s)", ra);
        this.ra = ra;
    }

    /**
     * Hash code
     *
     * @return The hash
     */
    @Override
    public int hashCode() {
        return 42;
    }

    /**
     * Equals
     *
     * @param other The other object
     * @return True if equal; otherwise false
     */
    public boolean equals(Object other) {
        if (other == null)
            return false;

        return getClass().equals(other.getClass());
    }
}
