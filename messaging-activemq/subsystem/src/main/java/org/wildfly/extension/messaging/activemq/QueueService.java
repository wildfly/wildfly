/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import java.util.function.Supplier;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Service responsible for create ActiveMQ core queues.
 *
 * @author Emanuel Muckenhuber
 */
class QueueService implements Service<Void> {

    private final Supplier<ActiveMQBroker> activeMQServerSupplier;
    private final CoreQueueConfiguration queueConfiguration;
    private final boolean temporary;
    private final boolean createQueue;

    public QueueService(final Supplier<ActiveMQBroker> activeMQServerSupplier, final CoreQueueConfiguration queueConfiguration, final boolean temporary, final boolean createQueue) {
        if(queueConfiguration == null) {
            throw MessagingLogger.ROOT_LOGGER.nullVar("queueConfiguration");
        }
        this.activeMQServerSupplier = activeMQServerSupplier;
        this.queueConfiguration = queueConfiguration;
        this.temporary = temporary;
        this.createQueue = createQueue;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        if (createQueue) {
            try {
                final ActiveMQBroker server = this.activeMQServerSupplier.get();
                MessagingLogger.ROOT_LOGGER.debugf("Deploying queue on server %s with address: %s ,  name: %s, filter: %s ands durable: %s, temporary: %s",
                        server.getNodeID(), new SimpleString(queueConfiguration.getAddress()), new SimpleString(queueConfiguration.getName()),
                        SimpleString.toSimpleString(queueConfiguration.getFilterString()), queueConfiguration.isDurable(), temporary);
                final SimpleString resourceName = new SimpleString(queueConfiguration.getName());
                final SimpleString address = new SimpleString(queueConfiguration.getAddress());
                final SimpleString filterString = SimpleString.toSimpleString(queueConfiguration.getFilterString());
                server.createQueue(address,
                        queueConfiguration.getRoutingType(),
                        resourceName,
                        filterString,
                        queueConfiguration.isDurable(),
                        temporary);
            } catch (Exception e) {
                throw new StartException(e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        try {
            final ActiveMQBroker server = this.activeMQServerSupplier.get();
            server.destroyQueue(new SimpleString(queueConfiguration.getName()), null, false);
            MessagingLogger.ROOT_LOGGER.debugf("Destroying queue from server %s queue with name: %s",server.getNodeID() , new SimpleString(queueConfiguration.getName()));
        } catch(Exception e) {
            MessagingLogger.ROOT_LOGGER.failedToDestroy("queue", queueConfiguration.getName());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }
}
