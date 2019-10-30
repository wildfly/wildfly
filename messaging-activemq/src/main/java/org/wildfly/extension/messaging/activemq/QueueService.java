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

package org.wildfly.extension.messaging.activemq;

import java.util.function.Supplier;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Service responsible for create ActiveMQ core queues.
 *
 * @author Emanuel Muckenhuber
 */
class QueueService implements Service<Void> {

    private final Supplier<ActiveMQServer> activeMQServerSupplier;
    private final CoreQueueConfiguration queueConfiguration;
    private final boolean temporary;
    private final boolean createQueue;

    public QueueService(final Supplier<ActiveMQServer> activeMQServerSupplier, final CoreQueueConfiguration queueConfiguration, final boolean temporary, final boolean createQueue) {
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
                final ActiveMQServer server = this.activeMQServerSupplier.get();
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
            final ActiveMQServer server = this.activeMQServerSupplier.get();
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
