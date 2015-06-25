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

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.config.CoreQueueConfiguration;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Service responsible for create ActiveMQ core queues.
 *
 * @author Emanuel Muckenhuber
 */
class QueueService implements Service<Void> {

    private final InjectedValue<ActiveMQServer> activeMQServer = new InjectedValue<ActiveMQServer>();
    private final CoreQueueConfiguration queueConfiguration;
    private final boolean temporary;

    public QueueService(final CoreQueueConfiguration queueConfiguration, final boolean temporary) {
        if(queueConfiguration == null) {
            throw MessagingLogger.ROOT_LOGGER.nullVar("queueConfiguration");
        }
        this.queueConfiguration = queueConfiguration;
        this.temporary = temporary;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            final ActiveMQServer server = this.activeMQServer.getValue();
            server.deployQueue(new SimpleString(queueConfiguration.getAddress()), new SimpleString(queueConfiguration.getName()),
                    SimpleString.toSimpleString(queueConfiguration.getFilterString()), queueConfiguration.isDurable(),
                    temporary);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        try {
            final ActiveMQServer server = this.activeMQServer.getValue();
            server.destroyQueue(new SimpleString(queueConfiguration.getName()), null, false);
        } catch(Exception e) {
            MessagingLogger.ROOT_LOGGER.failedToDestroy("queue", queueConfiguration.getName());
        }
    }

    /** {@inheritDoc} */
    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    InjectedValue<ActiveMQServer> getActiveMQServer() {
        return activeMQServer;
    }

}
