/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation.ra;

import java.io.Serializable;
import jakarta.resource.Referenceable;
import jakarta.resource.ResourceException;

/**
 * Connection factory
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public interface ValidConnectionFactory1 extends Serializable, Referenceable {
    /**
     * Get connection from factory
     *
     * @return Connection instance
     * @throws jakarta.resource.ResourceException Thrown if a connection can't be obtained
     */
    ValidConnection getConnection() throws ResourceException;
}
