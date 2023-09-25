/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import org.jboss.jca.core.connectionmanager.pool.idle.IdleRemover;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Idle remover service
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
final class IdleRemoverService implements Service<IdleRemover> {

    /**
     * Constructor
     */
    public IdleRemoverService() {
    }

    @Override
    public IdleRemover getValue() throws IllegalStateException {
        return IdleRemover.getInstance();
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            IdleRemover.getInstance().start();
        } catch (Throwable t) {
            throw new StartException(t);
        }
    }

    @Override
    public void stop(StopContext context) {
        try {
            IdleRemover.getInstance().stop();
        } catch (Throwable t) {
            // Ignore
        }
    }
}
