/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.rar;

import java.io.Serializable;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

/**
 * User: jpai
 */
public interface HelloWorldConnectionFactory extends Serializable, Referenceable {

    /**
     * Get connection from factory
     *
     * @return HelloWorldConnection instance
     * @throws ResourceException Thrown if a connection can't be obtained
     */

    HelloWorldConnection getConnection() throws ResourceException;


}
