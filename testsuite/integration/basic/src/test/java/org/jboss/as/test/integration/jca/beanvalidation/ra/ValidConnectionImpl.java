/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation.ra;

/**
 * Connection implementation
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ValidConnectionImpl implements ValidConnection {
    /**
     * ManagedConnection
     */
    private ValidManagedConnection mc;

    /**
     * ManagedConnectionFactory
     */
    private ValidManagedConnectionFactory mcf;

    /**
     * Default constructor
     *
     * @param mc  ManagedConnection
     * @param mcf ManagedConnectionFactory
     */
    public ValidConnectionImpl(ValidManagedConnection mc, ValidManagedConnectionFactory mcf) {
        this.mc = mc;
        this.mcf = mcf;
    }

    /**
     * Call getResourceAdapterProperty
     *
     * @return String
     */
    public int getResourceAdapterProperty() {
        return ((ValidResourceAdapter) mcf.getResourceAdapter()).getRaProperty();
    }

    /**
     * Call getManagedConnectionFactoryProperty
     *
     * @return String
     */
    public String getManagedConnectionFactoryProperty() {
        return mcf.getCfProperty();
    }

    /**
     * Close
     */
    public void close() {
        mc.closeHandle(this);
    }
}
