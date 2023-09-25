/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation.ra;

/**
 * Connection
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public interface ValidConnection1 {
    /**
     * getResourceAdapterProperty
     *
     * @return String
     */
    int getResourceAdapterProperty();

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
