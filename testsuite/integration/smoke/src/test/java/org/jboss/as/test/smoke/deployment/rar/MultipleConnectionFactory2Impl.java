/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar;

import org.jboss.logging.Logger;
import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

/**
 * MultipleConnectionFactory2Impl
 *
 * @version $Revision: $
 */
public class MultipleConnectionFactory2Impl implements MultipleConnectionFactory2 {
    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("MultipleConnectionFactory2Impl");

    /**
     * Reference
     */
    private Reference reference;

    /**
     * ManagedConnectionFactory
     */
    private MultipleManagedConnectionFactory2 mcf;

    /**
     * ConnectionManager
     */
    private ConnectionManager connectionManager;

    /**
     * Default constructor
     */
    public MultipleConnectionFactory2Impl() {

    }

    /**
     * Default constructor
     *
     * @param mcf       ManagedConnectionFactory
     * @param cxManager ConnectionManager
     */
    public MultipleConnectionFactory2Impl(MultipleManagedConnectionFactory2 mcf, ConnectionManager cxManager) {
        this.mcf = mcf;
        this.connectionManager = cxManager;
    }

    /**
     * Get connection from factory
     *
     * @return MultipleConnection2 instance
     * @throws ResourceException Thrown if a connection can't be obtained
     */
    @Override
    public MultipleConnection2 getConnection() throws ResourceException {
        log.trace("getConnection()");
        return (MultipleConnection2) connectionManager.allocateConnection(mcf, null);
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


}
