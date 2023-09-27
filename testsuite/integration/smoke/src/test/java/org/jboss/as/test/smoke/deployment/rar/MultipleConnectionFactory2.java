/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar;

import java.io.Serializable;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

/**
 * MultipleConnectionFactory2
 *
 * @version $Revision: $
 */
public interface MultipleConnectionFactory2 extends Serializable, Referenceable {
    /**
     * Get connection from factory
     *
     * @return MultipleConnection2 instance
     * @throws ResourceException Thrown if a connection can't be obtained
     */
    MultipleConnection2 getConnection() throws ResourceException;

}
