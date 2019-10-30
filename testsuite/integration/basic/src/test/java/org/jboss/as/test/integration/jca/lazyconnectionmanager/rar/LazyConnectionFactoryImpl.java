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

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

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
