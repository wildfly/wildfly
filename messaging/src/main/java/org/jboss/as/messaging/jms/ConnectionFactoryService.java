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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.messaging.MessagingLogger.MESSAGING_LOGGER;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.concurrent.ExecutorService;

/**
 * {@code Service} responsible for creating and destroying a {@code javax.jms.ConnectionFactory}.
 *
 * @author Emanuel Muckenhuber
 */
class ConnectionFactoryService implements Service<Void> {

    private final String name;
    private final ConnectionFactoryConfiguration configuration;
    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<JMSServerManager>();
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();

    public ConnectionFactoryService(final ConnectionFactoryConfiguration configuration) {
        name = configuration.getName();
        if(name == null) {
            throw MESSAGES.nullVar("cf name");
        }
        this.configuration = configuration;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        context.asynchronous();
        final JMSServerManager jmsManager = jmsServer.getValue();

        executorInjector.getValue().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    jmsManager.createConnectionFactory(false, configuration, configuration.getBindings());
                    context.complete();
                } catch (Throwable e) {
                    context.failed(MESSAGES.failedToCreate(e, "connection-factory"));
                }
            }
        });
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        // JMS Server Manager uses locking which waits on service completion, use async to prevent starvation
        context.asynchronous();
        final JMSServerManager jmsManager = jmsServer.getValue();

        executorInjector.getValue().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    jmsManager.destroyConnectionFactory(name);
                } catch (Throwable e) {
                    MESSAGING_LOGGER.failedToDestroy("connection-factory", name);
                }
                context.complete();
            }
        });
    }

    /** {@inheritDoc} */
    public Void getValue() throws IllegalStateException {
        return null;
    }

    InjectedValue<JMSServerManager> getJmsServer() {
        return jmsServer;
    }


    public InjectedValue<ExecutorService> getExecutorInjector() {
        return executorInjector;
    }
}
