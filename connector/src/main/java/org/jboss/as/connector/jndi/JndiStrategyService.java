/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.connector.jndi;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.jca.core.naming.ExplicitJndiStrategy;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * The JNDI strategy service
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class JndiStrategyService implements Service<JndiStrategy> {

    private static final Logger log = Logger.getLogger("org.jboss.as.connector.jndi");

    private JndiStrategy value;

    /**
     * Create an instance
     */
    public JndiStrategyService() {
        this(new ExplicitJndiStrategy());
    }

    /**
     * Create an instance with a specified strategy.
     */
    public JndiStrategyService(final JndiStrategy strategy) {
        this.value = strategy;
    }

    @Override
    public JndiStrategy getValue() throws IllegalStateException {
        return ConnectorServices.notNull(value);
    }

    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting sevice %s", ConnectorServices.JNDI_STRATEGY_SERVICE);
    }

    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {
        log.debugf("Stopping sevice %s", ConnectorServices.JNDI_STRATEGY_SERVICE);
    }
}
