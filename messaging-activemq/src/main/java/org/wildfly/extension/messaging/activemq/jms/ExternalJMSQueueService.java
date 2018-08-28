/*
 * Copyright 2018 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq.jms;


import javax.jms.Queue;
import org.apache.activemq.artemis.api.jms.ActiveMQJMSClient;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for creating and destroying a client {@code javax.jms.Queue}.
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalJMSQueueService implements Service<Queue> {

    static final String JMS_QUEUE_PREFIX = "jms.queue.";
    private final String queueName;
    private Queue queue;

    public ExternalJMSQueueService(final String queueName) {
        this.queueName = queueName;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        queue = ActiveMQJMSClient.createQueue(JMS_QUEUE_PREFIX + queueName);
    }

    @Override
    public synchronized void stop(final StopContext context) {
        queue = null;
    }

    @Override
    public Queue getValue() throws IllegalStateException, IllegalArgumentException {
        return queue;
    }

    public static Service<Queue> installService(final String name, final ServiceTarget serviceTarget, final ServiceName serviceName) {
        final ExternalJMSQueueService service = new ExternalJMSQueueService(name);
        final ServiceBuilder<Queue> serviceBuilder = serviceTarget.addService(serviceName, service);
        serviceBuilder.install();
        return service;
    }
}
