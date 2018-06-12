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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.jms.Topic;

import org.apache.activemq.artemis.jms.client.ActiveMQTopic;
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
 * Service responsible for creating and destroying a {@code javax.jms.Topic}.
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
        this.name = name;
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
                    topic = new ActiveMQTopic(JMS_TOPIC_PREFIX + name, name);
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

        final ServiceBuilder<Topic> serviceBuilder = serviceTarget.addService(serviceName, service)
                .addDependency(ActiveMQActivationService.getServiceName(serverServiceName))
                .addDependency(JMSServices.getJmsManagerBaseServiceName(serverServiceName), JMSServerManager.class, service.jmsServer)
                .setInitialMode(ServiceController.Mode.PASSIVE);
        org.jboss.as.server.Services.addServerExecutorDependency(serviceBuilder, service.executorInjector);
        serviceBuilder.install();

        return service;
    }
}
