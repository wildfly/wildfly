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

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.server.Services.addServerExecutorDependency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.jms.Queue;

import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.jms.server.JMSServerManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq.ActiveMQActivationService;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

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

    private Queue queue;

    public JMSQueueService(final String queueName, String selectorString, boolean durable) {
        this.queueName = queueName;
        this.selectorString = selectorString;
        this.durable = durable;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final JMSServerManager jmsManager = jmsServer.getValue();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    jmsManager.createQueue(false, queueName, selectorString, durable);
                    JMSQueueService.this.queue = new ActiveMQQueue(queueName);
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
    public void stop(final StopContext context) {
    }

    @Override
    public Queue getValue() throws IllegalStateException, IllegalArgumentException {
        return queue;
    }

    public static Service<Queue> installService(final String name, final ServiceTarget serviceTarget, final ServiceName serverServiceName, final String selector, final boolean durable) {
        final JMSQueueService service = new JMSQueueService(name, selector, durable);

        final ServiceName serviceName = JMSServices.getJmsQueueBaseServiceName(serverServiceName).append(name);
        final ServiceBuilder<Queue> serviceBuilder = serviceTarget.addService(serviceName, service)
                .addDependency(ActiveMQActivationService.getServiceName(serverServiceName))
                .addDependency(JMSServices.getJmsManagerBaseServiceName(serverServiceName), JMSServerManager.class, service.jmsServer)
                .setInitialMode(ServiceController.Mode.PASSIVE);
        addServerExecutorDependency(serviceBuilder, service.executorInjector);
        serviceBuilder.install();

        return service;
    }
}
