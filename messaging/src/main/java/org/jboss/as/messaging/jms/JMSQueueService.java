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

import static org.jboss.as.messaging.logging.MessagingLogger.MESSAGING_LOGGER;
import static org.jboss.as.server.Services.addServerExecutorDependency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.jms.Queue;

import org.hornetq.jms.client.HornetQQueue;
import org.hornetq.jms.server.JMSServerManager;
import org.jboss.as.messaging.HornetQActivationService;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for creating and destroying a {@code javax.jms.Queue}.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSQueueService implements Service<Queue> {

    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<JMSServerManager>();
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();

    private final String queueName;
    private final String selectorString;
    private final boolean durable;
    private final String[] jndi;

    private Queue queue;

    public JMSQueueService(final String queueName, String selectorString, boolean durable, String[] jndi) {
        this.queueName = queueName;
        this.selectorString = selectorString;
        this.durable = durable;
        this.jndi = jndi;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final JMSServerManager jmsManager = jmsServer.getValue();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    jmsManager.createQueue(false, queueName, selectorString, durable, jndi);
                    queue = new HornetQQueue(queueName);
                    context.complete();
                } catch (Throwable e) {
                    context.failed(MessagingLogger.ROOT_LOGGER.failedToCreate(e, "queue"));
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

    @Override
    public synchronized void stop(final StopContext context) {
        final JMSServerManager jmsManager = jmsServer.getValue();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    jmsManager.removeQueueFromJNDI(queueName);
                    queue = null;
                } catch (Throwable e) {
                    MESSAGING_LOGGER.failedToDestroy(e, "queue", queueName);
                }
                context.complete();
            }
        };
        // JMS Server Manager uses locking which waits on service completion, use async to prevent starvation
        try {
            executorInjector.getValue().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public Queue getValue() throws IllegalStateException, IllegalArgumentException {
        return queue;
    }

    public static Service<Queue> installService(final String name, final ServiceTarget serviceTarget, final ServiceName hqServiceName, final String selector, final boolean durable, final String[] jndiBindings) {
        final JMSQueueService service = new JMSQueueService(name, selector, durable, jndiBindings);

        final ServiceName serviceName = JMSServices.getJmsQueueBaseServiceName(hqServiceName).append(name);
        final ServiceBuilder<Queue> serviceBuilder = serviceTarget.addService(serviceName, service)
                .addDependency(HornetQActivationService.getHornetQActivationServiceName(hqServiceName))
                .addDependency(JMSServices.getJmsManagerBaseServiceName(hqServiceName), JMSServerManager.class, service.jmsServer)
                .setInitialMode(ServiceController.Mode.PASSIVE);
        addServerExecutorDependency(serviceBuilder, service.executorInjector, false);
        serviceBuilder.install();
        return service;
    }
}
