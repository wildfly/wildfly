/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.configproperty;

import javax.naming.NamingException;
import javax.naming.Reference;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

/**
 * ConfigPropertyConnectionFactoryImpl
 *
 * @version $Revision: $
 */
public class ConfigPropertyConnectionFactoryImpl implements ConfigPropertyConnectionFactory {
    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Reference
     */
    private Reference reference;

    /**
     * ManagedConnectionFactory
     */
    private ConfigPropertyManagedConnectionFactory mcf;

    /**
     * ConnectionManager
     */
    private ConnectionManager connectionManager;

    /**
     * Default constructor
     */
    public ConfigPropertyConnectionFactoryImpl() {

    }

    /**
     * Constructor
     *
     * @param mcf       ManagedConnectionFactory
     * @param cxManager ConnectionManager
     */
    public ConfigPropertyConnectionFactoryImpl(ConfigPropertyManagedConnectionFactory mcf, ConnectionManager cxManager) {
        this.mcf = mcf;
        this.connectionManager = cxManager;
    }

    /**
     * Get connection from factory
     *
     * @return ConfigPropertyConnection instance
     * @throws jakarta.resource.ResourceException Thrown if a connection can't be obtained
     */
    @Override
    public ConfigPropertyConnection getConnection() throws ResourceException {
        return (ConfigPropertyConnection) connectionManager.allocateConnection(mcf, null);
    }

    /**
     * Get the Reference instance.
     *
     * @return Reference instance
     * @throws javax.naming.NamingException Thrown if a reference can't be obtained
     */
    @Override
    public Reference getReference() throws NamingException {
        return reference;
    }

    /**
     * Set the Reference instance.
     *
     * @param reference A Reference instance
     */
    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }
}
