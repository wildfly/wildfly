/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars;

import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;
import jakarta.resource.cci.ConnectionSpec;
import jakarta.resource.cci.RecordFactory;
import jakarta.resource.cci.ResourceAdapterMetaData;
import jakarta.resource.spi.ManagedConnectionFactory;

/**
 * BaseCciConnectionFactory
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>.
 */
public class BaseCciConnectionFactory implements ConnectionFactory {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Reference
     */
    private Reference reference;

    /**
     * ManagedConnectionFactory
     */
    private ManagedConnectionFactory mcf;

    /**
     *
     * Create a new BaseCciConnectionFactory.
     *
     * @param cf ManagedConnectionFactory, which creates this
     */
    public BaseCciConnectionFactory(ManagedConnectionFactory cf) {
        mcf = cf;
    }

    /**
     *
     * Create a new BaseCciConnectionFactory.
     *
     */
    public BaseCciConnectionFactory() {
        mcf = null;
    }

    /**
     *
     * get ManagedConnectionFactory
     *
     * @return ManagedConnectionFactory
     */
    public ManagedConnectionFactory getMcf() {
        return mcf;
    }

    /*
     * getConnection
     *
     * @see jakarta.resource.cci.ConnectionFactory#getConnection()
     */
    @Override
    public Connection getConnection() throws ResourceException {
        return new BaseCciConnection();
    }

    /*
     * getConnection
     *
     * @see jakarta.resource.cci.ConnectionFactory#getConnection(jakarta.resource.cci.ConnectionSpec)
     */
    @Override
    public Connection getConnection(ConnectionSpec properties) throws ResourceException {
        return new BaseCciConnection();
    }

    /*
     * getMetaData
     *
     * @see jakarta.resource.cci.ConnectionFactory#getMetaData()
     */
    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return null;
    }

    /*
     * getRecordFactory
     *
     * @see jakarta.resource.cci.ConnectionFactory#getRecordFactory()
     */
    @Override
    public RecordFactory getRecordFactory() throws ResourceException {
        return null;
    }

    /*
     * getReference
     *
     * @see javax.naming.Referenceable#getReference()
     */
    @Override
    public Reference getReference() throws NamingException {
        if (reference == null)
            reference = new BaseReference(this.getClass().getName());
        return reference;
    }

    /*
     * setReference
     *
     * @see jakarta.resource.Referenceable#setReference(javax.naming.Reference)
     */
    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

}
