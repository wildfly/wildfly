/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.manualmode.jca.workmanager.distributed.ra;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

public class DistributedConnectionFactory1Impl implements DistributedConnectionFactory1 {

    private static final long serialVersionUID = 812283381273295931L;

    private Reference reference;
    private DistributedManagedConnectionFactory1 dmcf;
    private ConnectionManager connectionManager;

    public DistributedConnectionFactory1Impl() {
        // empty
    }

    public DistributedConnectionFactory1Impl(DistributedManagedConnectionFactory1 dmcf, ConnectionManager cxManager) {
        this.dmcf = dmcf;
        this.connectionManager = cxManager;
    }

    @Override
    public DistributedConnection1 getConnection() throws ResourceException {
        return (DistributedConnection1) connectionManager.allocateConnection(dmcf, null);
    }

    public Reference getReference() throws NamingException {
        return reference;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }
}
