/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

import java.io.Serializable;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

/**
 * AnnoConnectionFactory
 *
 * @version $Revision: $
 */
public interface AnnoConnectionFactory extends Serializable, Referenceable {
    /**
     * Get connection from factory
     *
     * @return AnnoConnection instance
     * @throws ResourceException Thrown if a connection can't be obtained
     */
    AnnoConnection getConnection() throws ResourceException;

}
