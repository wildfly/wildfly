/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.beanvalidation.ra;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionMetaData;

/**
 * Managed connection metadata
 *
 * @author <a href="mailto:vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
public class ValidManagedConnectionMetaData implements ManagedConnectionMetaData {
    /**
     * Default constructor
     */
    public ValidManagedConnectionMetaData() {
    }

    /**
     * Returns Product name of the underlying EIS instance connected through the ManagedConnection.
     *
     * @return Product name of the EIS instance
     * @throws jakarta.resource.ResourceException Thrown if an error occurs
     */
    @Override
    public String getEISProductName() throws ResourceException {
        return null;
    }

    /**
     * Returns Product version of the underlying EIS instance connected through the ManagedConnection.
     *
     * @return Product version of the EIS instance
     * @throws jakarta.resource.ResourceException Thrown if an error occurs
     */
    @Override
    public String getEISProductVersion() throws ResourceException {
        return null;
    }

    /**
     * Returns maximum limit on number of active concurrent connections
     *
     * @return Maximum limit for number of active concurrent connections
     * @throws jakarta.resource.ResourceException Thrown if an error occurs
     */
    @Override
    public int getMaxConnections() throws ResourceException {
        return 0;
    }

    /**
     * Returns name of the user associated with the ManagedConnection instance
     *
     * @return Name of the user
     * @throws jakarta.resource.ResourceException Thrown if an error occurs
     */
    @Override
    public String getUserName() throws ResourceException {
        return null;
    }
}
