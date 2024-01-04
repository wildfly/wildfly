/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.apache.activemq.artemis.jms.server.config.ConnectionFactoryConfiguration;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * {@code Service} responsible for creating and destroying a {@code jakarta.jms.ConnectionFactory}.
 *
 * @author Emanuel Muckenhuber
 */
class ConnectionFactoryService implements Service<Void> {

    private final String name;
    private final ConnectionFactoryConfiguration configuration;
    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<>();
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<>();

    public ConnectionFactoryService(final ConnectionFactoryConfiguration configuration) {
        name = configuration.getName();
        if(name == null) {
            throw MessagingLogger.ROOT_LOGGER.nullVar("cf name");
        }
        this.configuration = configuration;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        final JMSServerManager jmsManager = jmsServer.getValue();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    jmsManager.createConnectionFactory(false, configuration, configuration.getBindings());
                    context.complete();
                } catch (Throwable e) {
                    context.failed(MessagingLogger.ROOT_LOGGER.failedToCreate(e, "connection-factory"));
                }
            }
        };
        try {
            executorInjector.getValue().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(final StopContext context) {
        final JMSServerManager jmsManager = jmsServer.getValue();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    jmsManager.destroyConnectionFactory(name);
                } catch (Throwable e) {
                    MessagingLogger.ROOT_LOGGER.failedToDestroy("connection-factory", name);
                }
                context.complete();
            }
        };
        // Jakarta Messaging Server Manager uses locking which waits on service completion, use async to prevent starvation
        try {
            executorInjector.getValue().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
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
