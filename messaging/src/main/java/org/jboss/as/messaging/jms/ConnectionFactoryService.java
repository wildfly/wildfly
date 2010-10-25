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

package org.jboss.as.messaging.jms;

import org.hornetq.jms.server.JMSServerManager;
import org.hornetq.jms.server.config.ConnectionFactoryConfiguration;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * {@code Service} responsible for creating and destroying a {@link javax.jms.ConnectionFactory}.
 *
 * @author Emanuel Muckenhuber
 */
class ConnectionFactoryService implements Service<Void> {

    private final String name;
    private final ConnectionFactoryConfiguration configuration;
    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<JMSServerManager>();

    public ConnectionFactoryService(final ConnectionFactoryConfiguration configuration) {
        name = configuration.getName();
        if(name == null) {
            throw new IllegalArgumentException("null cf name");
        }
        this.configuration = configuration;
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {
        final JMSServerManager jmsManager = jmsServer.getValue();
        try {
            jmsManager.createConnectionFactory(false, configuration, configuration.getBindings());
        } catch (Exception e) {
            throw new StartException("failed to create connection-factory", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        final JMSServerManager jmsManager = jmsServer.getValue();
        try {
            jmsManager.destroyConnectionFactory(name);
        } catch (Exception e) {
            Logger.getLogger("org.jboss.messaging").warnf(e ,"failed to destroy connection-factory: %s", name);
        }
    }

    /** {@inheritDoc} */
    public Void getValue() throws IllegalStateException {
        return null;
    }

    InjectedValue<JMSServerManager> getJmsServer() {
        return jmsServer;
    }

}
