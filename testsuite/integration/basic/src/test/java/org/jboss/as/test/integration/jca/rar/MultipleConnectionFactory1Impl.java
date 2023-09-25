/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.rar;

import org.jboss.logging.Logger;
import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ConnectionManager;

/**
 * MultipleConnectionFactory1Impl
 *
 * @version $Revision: $
 */
public class MultipleConnectionFactory1Impl implements
        MultipleConnectionFactory1 {
    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger
     */
    private static Logger log = Logger
            .getLogger("MultipleConnectionFactory1Impl");

    /**
     * Reference
     */
    private Reference reference;

    /**
     * ManagedConnectionFactory
     */
    private MultipleManagedConnectionFactory1 mcf;

    /**
     * ConnectionManager
     */
    private ConnectionManager connectionManager;

    /**
     * Default constructor
     */
    public MultipleConnectionFactory1Impl() {

    }

    /**
     * Default constructor
     *
     * @param mcf       ManagedConnectionFactory
     * @param cxManager ConnectionManager
     */
    public MultipleConnectionFactory1Impl(
            MultipleManagedConnectionFactory1 mcf, ConnectionManager cxManager) {
        this.mcf = mcf;
        this.connectionManager = cxManager;
    }

    /**
     * Get connection from factory
     *
     * @return MultipleConnection1 instance
     * @throws ResourceException Thrown if a connection can't be obtained
     */
    @Override
    public Connection getConnection() throws ResourceException {
        log.trace("getConnection()");
        return (MultipleConnection1) connectionManager.allocateConnection(mcf,
                null);
    }

    /**
     * Get the Reference instance.
     *
     * @return Reference instance
     * @throws NamingException Thrown if a reference can't be obtained
     */
    @Override
    public Reference getReference() throws NamingException {
        log.trace("getReference()");
        return reference;
    }

    /**
     * Set the Reference instance.
     *
     * @param reference A Reference instance
     */
    @Override
    public void setReference(Reference reference) {
        log.trace("setReference()");
        this.reference = reference;
    }

    @Override
    public Connection getConnection(ConnectionSpec arg0)
            throws ResourceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String toString() {
        return this.getClass().toString() + mcf.toString();
    }

}
