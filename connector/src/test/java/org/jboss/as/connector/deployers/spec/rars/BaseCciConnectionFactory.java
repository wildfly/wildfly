/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2009, Red Hat Inc, and individual contributors
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
package org.jboss.as.connector.deployers.spec.rars;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ManagedConnectionFactory;

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
     * @see javax.resource.cci.ConnectionFactory#getConnection()
     */
    @Override
    public Connection getConnection() throws ResourceException {
        return new BaseCciConnection();
    }

    /*
     * getConnection
     *
     * @see javax.resource.cci.ConnectionFactory#getConnection(javax.resource.cci.ConnectionSpec)
     */
    @Override
    public Connection getConnection(ConnectionSpec properties) throws ResourceException {
        return new BaseCciConnection();
    }

    /*
     * getMetaData
     *
     * @see javax.resource.cci.ConnectionFactory#getMetaData()
     */
    @Override
    public ResourceAdapterMetaData getMetaData() throws ResourceException {
        return null;
    }

    /*
     * getRecordFactory
     *
     * @see javax.resource.cci.ConnectionFactory#getRecordFactory()
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
     * @see javax.resource.Referenceable#setReference(javax.naming.Reference)
     */
    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

}
