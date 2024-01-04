/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.rar;

import org.jboss.logging.Logger;
import jakarta.resource.ResourceException;
import jakarta.resource.spi.ManagedConnectionMetaData;

/**
 * Multiple2ManagedConnectionMetaData
 *
 * @version $Revision: $
 */
public class Multiple2ManagedConnectionMetaData implements ManagedConnectionMetaData {
    /**
     * The logger
     */
    private static Logger log = Logger.getLogger("Multiple2ManagedConnectionMetaData");

    /**
     * Default constructor
     */
    public Multiple2ManagedConnectionMetaData() {

    }

    /**
     * Returns Product name of the underlying EIS instance connected through the ManagedConnection.
     *
     * @return Product name of the EIS instance
     * @throws ResourceException Thrown if an error occurs
     */
    @Override
    public String getEISProductName() throws ResourceException {
        log.trace("getEISProductName()");
        return null; //TODO
    }

    /**
     * Returns Product version of the underlying EIS instance connected through the ManagedConnection.
     *
     * @return Product version of the EIS instance
     * @throws ResourceException Thrown if an error occurs
     */
    @Override
    public String getEISProductVersion() throws ResourceException {
        log.trace("getEISProductVersion()");
        return null; //TODO
    }

    /**
     * Returns maximum limit on number of active concurrent connections
     *
     * @return Maximum limit for number of active concurrent connections
     * @throws ResourceException Thrown if an error occurs
     */
    @Override
    public int getMaxConnections() throws ResourceException {
        log.trace("getMaxConnections()");
        return 0; //TODO
    }

    /**
     * Returns name of the user associated with the ManagedConnection instance
     *
     * @return Name of the user
     * @throws ResourceException Thrown if an error occurs
     */
    @Override
    public String getUserName() throws ResourceException {
        log.trace("getUserName()");
        return null; //TODO
    }


}
