/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar;

import java.io.Serializable;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

/**
 * MultipleConnectionFactory1
 *
 * @version $Revision: $
 */
public interface MultipleConnectionFactory1 extends Serializable, Referenceable {
    /**
     * Get connection from factory
     *
     * @return MultipleConnection1 instance
     * @throws ResourceException Thrown if a connection can't be obtained
     */
    MultipleConnection1 getConnection() throws ResourceException;

}
