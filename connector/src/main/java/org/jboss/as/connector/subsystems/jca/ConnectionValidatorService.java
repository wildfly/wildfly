/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import org.jboss.jca.core.connectionmanager.pool.validator.ConnectionValidator;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Connection validator service
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
final class ConnectionValidatorService implements Service<ConnectionValidator> {

    /**
     * Constructor
     */
    public ConnectionValidatorService() {
    }

    @Override
    public ConnectionValidator getValue() throws IllegalStateException {
        return ConnectionValidator.getInstance();
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            ConnectionValidator.getInstance().start();
        } catch (Throwable t) {
            throw new StartException(t);
        }
    }

    @Override
    public void stop(StopContext context) {
        try {
            ConnectionValidator.getInstance().stop();
        } catch (Throwable t) {
            // Ignore
        }
    }
}
