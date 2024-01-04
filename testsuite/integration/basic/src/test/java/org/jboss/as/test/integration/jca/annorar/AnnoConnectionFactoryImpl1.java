/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

import org.jboss.logging.Logger;
import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

/**
 * AnnoConnectionFactoryImpl
 *
 * @version $Revision: $
 */
public class AnnoConnectionFactoryImpl1 implements AnnoConnectionFactory1 {
    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("AnnoConnectionFactoryImpl");

    /**
     * Reference
     */
    private Reference reference;

    /**
     * ManagedConnectionFactory
     */
    private AnnoManagedConnectionFactory1 mcf;

    /**
     * ConnectionManager
     */
    private ConnectionManager connectionManager;

    /**
     * Default constructor
     */
    public AnnoConnectionFactoryImpl1() {

    }

    /**
     * Default constructor
     *
     * @param mcf       ManagedConnectionFactory
     * @param cxManager ConnectionManager
     */
    public AnnoConnectionFactoryImpl1(AnnoManagedConnectionFactory1 mcf,
                                      ConnectionManager cxManager) {
        this.mcf = mcf;
        this.connectionManager = cxManager;
    }

    /**
     * Get connection from factory
     *
     * @return AnnoConnection instance
     * @throws ResourceException Thrown if a connection can't be obtained
     */
    @Override
    public AnnoConnection1 getConnection() throws ResourceException {
        log.trace("getConnection()");
        return (AnnoConnection1) connectionManager
                .allocateConnection(mcf, null);
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
