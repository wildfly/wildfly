/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.configproperty;

/**
 * ConfigPropertyConnection
 *
 * @version $Revision: $
 */
public interface ConfigPropertyConnection {
    /**
     * getResourceAdapterProperty
     *
     * @return String
     */
    String getResourceAdapterProperty();

    /**
     * getManagedConnectionFactoryProperty
     *
     * @return String
     */
    String getManagedConnectionFactoryProperty();

    /**
     * Close
     */
    void close();
}
