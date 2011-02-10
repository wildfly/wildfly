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

package org.jboss.as.messaging;

import org.hornetq.api.core.SimpleString;
import org.hornetq.core.server.HornetQServer;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for create HornetQ core queues.
 *
 * @author Emanuel Muckenhuber
 */
class QueueService implements Service<Void> {

    private final InjectedValue<HornetQServer> hornetQService = new InjectedValue<HornetQServer>();
    private final String address;
    private final String queueName;
    private final String filter;
    private final boolean durable;
    private final boolean temporary;

    public QueueService(String address, String queueName, String filter, boolean durable, boolean temporary) {
        if(queueName == null) {
            throw new IllegalArgumentException("null queue name");
        }
        this.address = address != null ? address : queueName;
        this.queueName = queueName;
        this.filter = filter;
        this.durable = durable;
        this.temporary = temporary;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        try {
            final HornetQServer hornetQService = this.hornetQService.getValue();
            hornetQService.deployQueue(new SimpleString(address), new SimpleString(queueName),
                    filter != null ? new SimpleString(filter) : null, durable, temporary);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        try {
            final HornetQServer hornetQService = this.hornetQService.getValue();
            hornetQService.destroyQueue(new SimpleString(queueName), null);
        } catch(Exception e) {
            Logger.getLogger("org.jboss.messaging").warnf(e, "failed to destroy queue (%s)", queueName);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    InjectedValue<HornetQServer> getHornetQService() {
        return hornetQService;
    }

}
