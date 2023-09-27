/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.lazyconnectionmanager.rar;

import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>
 * @author <a href="mailto:msimka@redhat.com">Martin Simka</a>
 */
public class LazyConnectionFactoryImpl implements LazyConnectionFactory {

    private static Logger logger = Logger.getLogger(LazyConnectionFactoryImpl.class);

    private Reference reference;
    private LazyManagedConnectionFactory mcf;
    private ConnectionManager connectionManager;

    public LazyConnectionFactoryImpl(LazyManagedConnectionFactory mcf, ConnectionManager connectionManager) {
        logger.trace("#LazyConnectionFactoryImpl");
        this.mcf = mcf;
        this.connectionManager = connectionManager;
    }

    @Override
    public LazyConnection getConnection() throws ResourceException {
        logger.trace("#LazyConnectionFactoryImpl.getConnection");
        return (LazyConnection) connectionManager.allocateConnection(mcf, null);
    }

    @Override
    public Reference getReference() throws NamingException {
        logger.trace("#LazyConnectionFactoryImpl.getReference");
        return reference;
    }

    @Override
    public void setReference(Reference reference) {
        logger.trace("#LazyConnectionFactoryImpl.setReference");
        this.reference = reference;
    }
}
