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
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionDefinition;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

/**
 * User: jpai
 */
@ConnectionDefinition(connectionFactory = HelloWorldConnectionFactory.class,
        connectionFactoryImpl = HelloWorldConnectionFactoryImpl.class,
        connection = HelloWorldConnection.class,
        connectionImpl = HelloWorldConnectionImpl.class)
public class HelloWorldManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {


    /**
     * The serialVersionUID
     */

    private static final long serialVersionUID = 1L;


    /**
     * The resource adapter
     */
    private ResourceAdapter ra;

    private PrintWriter writer;

    /**
     * Default constructor
     */

    public HelloWorldManagedConnectionFactory() {


    }


    /**
     * Creates a Connection Factory instance.
     *
     * @return EIS-specific Connection Factory instance or
     *         <p/>
     *         javax.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */

    public Object createConnectionFactory() throws ResourceException {

        throw new ResourceException("This resource adapter doesn't support non-managed environments");

    }


    /**
     * Creates a Connection Factory instance.
     *
     * @param cxManager ConnectionManager to be associated with created EIS
     *                  <p/>
     *                  connection factory instance
     * @return EIS-specific Connection Factory instance or
     *         <p/>
     *         javax.resource.cci.ConnectionFactory instance
     * @throws ResourceException Generic exception
     */

    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {

        return new HelloWorldConnectionFactoryImpl(this, cxManager);

    }


    /**
     * Creates a new physical connection to the underlying EIS resource manager.
     *
     * @param subject       Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection
     *                      <p/>
     *                      request information
     * @return ManagedConnection instance
     * @throws ResourceException generic exception
     */

    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        return new HelloWorldManagedConnection(this);

    }


    /**
     * Returns a matched connection from the candidate set of connections.
     *
     * @param connectionSet Candidate connection set
     * @param subject       Caller's security information
     * @param cxRequestInfo Additional resource adapter specific connection request information
     * @return ManagedConnection if resource adapter finds an acceptable match otherwise null
     * @throws ResourceException generic exception
     */

    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo cxRequestInfo) throws ResourceException {

        return null;

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
     * Get the resource adapter
     *
     * @return The handle
     */

    public ResourceAdapter getResourceAdapter() {

        return ra;

    }


    /**
     * Set the resource adapter
     *
     * @param ra The handle
     */

    public void setResourceAdapter(ResourceAdapter ra) {

        this.ra = ra;

    }

    public boolean equals(Object o) {
        return super.equals(o);
    }

    public int hashCode() {
        return super.hashCode();
    }
}
