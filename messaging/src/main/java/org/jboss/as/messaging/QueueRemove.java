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

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;

/**
 * Core queue remove update.
 *
 * @author Emanuel Muckenhuber
 */
public class QueueRemove extends AbstractMessagingSubsystemUpdate<Void> {

    private static final long serialVersionUID = 5921273777496713096L;
    private final String name;

    public QueueRemove(final String name) {
        if(name == null) {
            throw new IllegalArgumentException("null name");
        }
        this.name = name;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(MessagingSubsystemElement element) throws UpdateFailedException {
        if(! element.removeQueue(name)) {
            throw new UpdateFailedException(String.format("queue (%s) does not exist", name));
        }
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> resultHandler, P param) {
        final ServiceController<?> service = context.getServiceContainer().getService(MessagingSubsystemElement.CORE_QUEUE_BASE.append(name));
        if(service == null) {
            resultHandler.handleSuccess(null, param);
        } else {
            service.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(resultHandler, param));
        }
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<MessagingSubsystemElement, ?> getCompensatingUpdate(MessagingSubsystemElement original) {
        final QueueElement queue = original.getQueue(name);
        if(queue == null) {
            return null;
        }
        final QueueAdd update = new QueueAdd(name);
        update.setAddress(queue.getAddress());
        update.setFilter(queue.getFilter());
        update.setDurable(queue.isDurable());
        return update;
    }

}
