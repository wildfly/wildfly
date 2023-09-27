/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar.configproperty;

import java.io.Serializable;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

/**
 * ConfigPropertyConnectionFactory
 *
 * @version $Revision: $
 */
public interface ConfigPropertyConnectionFactory extends Serializable, Referenceable {
    /**
     * Get connection from factory
     *
     * @return ConfigPropertyConnection instance
     * @throws jakarta.resource.ResourceException Thrown if a connection can't be obtained
     */
    ConfigPropertyConnection getConnection() throws ResourceException;
}
