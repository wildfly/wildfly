/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.jms;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import jakarta.jms.Topic;

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
 * Service responsible for creating and destroying a {@code jakarta.jms.Topic}.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSTopicService implements Service<Topic> {

    static final String JMS_TOPIC_PREFIX = "jms.topic.";

    private final InjectedValue<JMSServerManager> jmsServer = new InjectedValue<JMSServerManager>();
    private final InjectedValue<ExecutorService> executorInjector = new InjectedValue<ExecutorService>();

    private final String name;
    private Topic topic;

    public JMSTopicService(String name) {
        if (name.startsWith(JMS_TOPIC_PREFIX)) {
            this.name = name.substring(JMS_TOPIC_PREFIX.length());
        } else {
            this.name = name;
        }
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        final JMSServerManager jmsManager = jmsServer.getValue();
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    // add back the jms.topic. prefix to be consistent with ActiveMQ Artemis 1.x addressing scheme
                    jmsManager.createTopic(JMS_TOPIC_PREFIX + name, false, name);
                    topic = ActiveMQDestination.createTopic(JMS_TOPIC_PREFIX + name, name);
                    context.complete();
                } catch (Throwable e) {
                    context.failed(MessagingLogger.ROOT_LOGGER.failedToCreate(e, "JMS Topic"));
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
    }

    @Override
    public Topic getValue() throws IllegalStateException {
        return topic;
    }

    public static JMSTopicService installService(final String name, final ServiceName serverServiceName, final ServiceTarget serviceTarget) {
        final JMSTopicService service = new JMSTopicService(name);
        final ServiceName serviceName = JMSServices.getJmsTopicBaseServiceName(serverServiceName).append(name);

        final ServiceBuilder<Topic> serviceBuilder = serviceTarget.addService(serviceName, service);
        serviceBuilder.requires(ActiveMQActivationService.getServiceName(serverServiceName));
        serviceBuilder.addDependency(JMSServices.getJmsManagerBaseServiceName(serverServiceName), JMSServerManager.class, service.jmsServer);
        serviceBuilder.setInitialMode(ServiceController.Mode.PASSIVE);
        org.jboss.as.server.Services.addServerExecutorDependency(serviceBuilder, service.executorInjector);
        serviceBuilder.install();

        return service;
    }
}
