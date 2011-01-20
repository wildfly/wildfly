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

package org.jboss.as.messaging.jms;

import org.jboss.as.model.AbstractSubsystemUpdate;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.msc.service.ServiceController;

/**
 * Update removing a {@code JMSQueueElement} from the {@code JMSSubsystemElement}. The
 * runtime action will remove the corresponding {@code JMSQueueService}.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSQueueRemove extends AbstractJMSSubsystemUpdate<Void> {

    private static final long serialVersionUID = -635741968258725932L;
    private final String queueName;

    public JMSQueueRemove(final String queueName) {
        if(queueName == null) {
            throw new IllegalArgumentException("null name");
        }
        this.queueName = queueName;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(JMSSubsystemElement element) throws UpdateFailedException {
        if(! element.removeQueue(queueName)) {
            throw new UpdateFailedException(String.format("queue (%s) does not exist", queueName));
        }
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> handler, P param) {
        final ServiceController<?> service = context.getServiceRegistry().getService(JMSServices.JMS_QUEUE_BASE.append(queueName));
        if(service == null) {
            handler.handleSuccess(null, param);
        } else {
            service.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(handler, param));
        }
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<JMSSubsystemElement, ?> getCompensatingUpdate(JMSSubsystemElement original) {
        final JMSQueueElement element = original.getQueue(queueName);
        if(element == null) {
            return null;
        }
        final JMSQueueAdd action = new JMSQueueAdd(queueName);
        action.setBindings(element.getBindings());
        action.setSelector(element.getSelector());
        action.setDurable(element.getDurable());
        return action;
    }

}
