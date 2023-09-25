/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnectionFactory;

import org.jboss.logging.Logger;

/**
 * BaseConnectionManager
 *
 * @author <a href="mailto:jesper.pedersen@ironjacamar.org">Jesper Pedersen</a>.
 * @version $Revision: $
 */
public class BaseConnectionManager implements ConnectionManager {
    private static final long serialVersionUID = 1L;
    private static Logger log = Logger.getLogger(BaseConnectionManager.class);

    /**
     * Constructor
     */
    public BaseConnectionManager() {
    }

    /**
     * Allocate a connection
     *
     * @param mcf The managed connection factory
     * @param cri The connection request information
     * @return The connection
     * @exception ResourceException Thrown if an error occurs
     */
    public Object allocateConnection(ManagedConnectionFactory mcf, ConnectionRequestInfo cri) throws ResourceException {
        log.trace("allocateConnection " + mcf + " " + cri);
        return null;
    }
}
