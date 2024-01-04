/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.rar;

import java.io.Serializable;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;
import jakarta.resource.cci.Connection;
import jakarta.resource.cci.ConnectionFactory;

/**
 * MultipleConnectionFactory1
 *
 * @version $Revision: $
 */
public interface MultipleConnectionFactory1 extends Serializable, Referenceable, ConnectionFactory {
    /**
     * Get connection from factory
     *
     * @return MultipleConnection1 instance
     * @throws ResourceException Thrown if a connection can't be obtained
     */
    Connection getConnection() throws ResourceException;

}
