/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.configproperty;

/**
 * ConfigPropertyConnectionImpl
 *
 * @version $Revision: $
 */
public class ConfigPropertyConnectionImpl implements ConfigPropertyConnection {
    /**
     * ManagedConnection
     */
    private ConfigPropertyManagedConnection mc;

    /**
     * ManagedConnectionFactory
     */
    private ConfigPropertyManagedConnectionFactory mcf;

    /**
     * Default constructor
     *
     * @param mc  ConfigPropertyManagedConnection
     * @param mcf ConfigPropertyManagedConnectionFactory
     */
    public ConfigPropertyConnectionImpl(ConfigPropertyManagedConnection mc, ConfigPropertyManagedConnectionFactory mcf) {
        this.mc = mc;
        this.mcf = mcf;
    }

    /**
     * Call getResourceAdapterProperty
     *
     * @return String
     */
    public String getResourceAdapterProperty() {
        return ((ConfigPropertyResourceAdapter) mcf.getResourceAdapter()).getProperty();
    }

    /**
     * Call getManagedConnectionFactoryProperty
     *
     * @return String
     */
    public String getManagedConnectionFactoryProperty() {
        return mcf.getProperty();
    }

    /**
     * Close
     */
    public void close() {
        mc.closeHandle(this);
    }
}
