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

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * Core queue add update.
 *
 * @author Emanuel Muckenhuber
 */
public class QueueAdd extends AbstractMessagingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 2210829361890051692L;

    private final String name;
    private String address;
    private String filter;
    private Boolean durable;

    public QueueAdd(final String name) {
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }
        this.name = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(MessagingSubsystemElement element) throws UpdateFailedException {
        final QueueElement queue = element.addQueue(name);
        if(queue == null) {
            throw new UpdateFailedException("duplicate queue " + name);
        }
        queue.setAddress(address);
        queue.setFilter(filter);
        if(durable != null) queue.setDurable(durable);
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final QueueService service = new QueueService(address, name, filter, durable != null ? true : durable, false);
        context.getBatchBuilder().addService(MessagingSubsystemElement.CORE_QUEUE_BASE.append(name), service)
            .addDependency(MessagingSubsystemElement.JBOSS_MESSAGING, HornetQServer.class, service.getHornetQService())
            .addListener(new UpdateResultHandler.ServiceStartListener<P>(resultHandler, param))
            .setInitialMode(Mode.IMMEDIATE);
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<MessagingSubsystemElement, ?> getCompensatingUpdate(MessagingSubsystemElement original) {
        return new QueueRemove(name);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Boolean getDurable() {
        return durable;
    }

    public void setDurable(Boolean durable) {
        this.durable = durable;
    }

    public String getName() {
        return name;
    }

}
