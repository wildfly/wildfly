/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import java.io.PrintWriter;
import java.util.Iterator;
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
