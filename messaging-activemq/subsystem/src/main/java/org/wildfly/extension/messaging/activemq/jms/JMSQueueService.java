/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.server.Services.addServerExecutorDependency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import jakarta.jms.Queue;

import org.apache.activemq.artemis.jms.client.ActiveMQDestination;
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
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Service responsible for creating and destroying a {@code jakarta.jms.Queue}.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSQueueService implements Service<Queue> {

    static final String JMS_QUEUE_PREFIX = "jms.queue.";

    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<JMSServerManager>();
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();

    private final String queueName;
    private final String selectorString;
    private final boolean durable;

    private Queue queue;

    public JMSQueueService(final String name, String selectorString, boolean durable) {
        if (name.startsWith(JMS_QUEUE_PREFIX)) {
            this.queueName = name.substring(JMS_QUEUE_PREFIX.length());
        } else {
            this.queueName = name;
        }
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
                    // add back the jms.queue. prefix to be consistent with ActiveMQ Artemis 1.x addressing scheme
                    jmsManager.createQueue(false, JMS_QUEUE_PREFIX + queueName, queueName, selectorString, durable);
                    JMSQueueService.this.queue = ActiveMQDestination.createQueue(JMS_QUEUE_PREFIX + queueName, queueName);
                    context.complete();
                } catch (Throwable e) {
                    context.failed(MessagingLogger.ROOT_LOGGER.failedToCreate(e, "JMS Queue"));
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
        final ServiceBuilder<Queue> serviceBuilder = serviceTarget.addService(serviceName, service);
        serviceBuilder.requires(ActiveMQActivationService.getServiceName(serverServiceName));
        serviceBuilder.addDependency(JMSServices.getJmsManagerBaseServiceName(serverServiceName), JMSServerManager.class, service.jmsServer);
        serviceBuilder.setInitialMode(ServiceController.Mode.PASSIVE);
        addServerExecutorDependency(serviceBuilder, service.executorInjector);
        serviceBuilder.install();

        return service;
    }
}
