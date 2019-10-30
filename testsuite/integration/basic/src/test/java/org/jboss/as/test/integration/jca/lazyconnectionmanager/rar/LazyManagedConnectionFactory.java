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
import java.util.Iterator;
import java.util.Set;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public class LazyManagedConnectionFactory implements ManagedConnectionFactory, ResourceAdapterAssociation {

    private static final long serialVersionUID = 8167326732027615486L;
    private static Logger logger = Logger.getLogger(LazyManagedConnectionFactory.class);

    private ConnectionManager cm;
    private ResourceAdapter ra;
    private PrintWriter logwriter;

    public LazyManagedConnectionFactory() {
    }

    @Override
    public Object createConnectionFactory(ConnectionManager connectionManager) throws ResourceException {
        logger.trace("#LazyManagedConnectionFactory.createConnectionFactory");

        this.cm = connectionManager;
        return new LazyConnectionFactoryImpl(this, connectionManager);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        throw new ResourceException("This resource adapter doesn't support non-managed environments");
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        logger.trace("#LazyManagedConnectionFactory.createManagedConnection");
        LazyResourceAdapter lra = (LazyResourceAdapter) ra;

        return new LazyManagedConnection(lra.getLocalTransaction().booleanValue(),
                lra.getXATransaction().booleanValue(),
                cm, this);
    }

    @Override
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject, ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
        logger.trace("#LazyManagedConnectionFactory.matchManagedConnections");
        ManagedConnection result = null;
        Iterator it = connectionSet.iterator();
        while (result == null && it.hasNext()) {
            ManagedConnection mc = (ManagedConnection) it.next();
            if (mc instanceof LazyManagedConnection) {
                result = mc;
            }
        }
        return result;
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws ResourceException {
        logger.trace("#LazyManagedConnectionFactory.setLogWriter");

    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        logger.trace("#LazyManagedConnectionFactory.getLogWriter");
        return null;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        logger.trace("#LazyManagedConnectionFactory.getResourceAdapter");
        return null;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceException {
        logger.trace("#LazyManagedConnectionFactory.setResourceAdapter");
        this.ra = resourceAdapter;
    }

    @Override
    public int hashCode() {
        int result = 17;
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) { return false; }
        if (other == this) { return true; }
        if (!(other instanceof LazyManagedConnectionFactory)) { return false; }
        LazyManagedConnectionFactory obj = (LazyManagedConnectionFactory) other;
        boolean result = true;
        return result;
    }


}
