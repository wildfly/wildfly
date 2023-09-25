/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.manualmode.jca.workmanager.distributed.ra;

import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

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
