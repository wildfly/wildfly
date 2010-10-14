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
 * Update removing a {@code JMSTopicElement} from the {@code JMSSubsystemElement}. The
 * runtime action will remove the corresponding {@code JMSTopicService}.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSTopicRemove extends AbstractJMSSubsystemUpdate<Void> {

    private static final long serialVersionUID = 141328949523686903L;
    private final String topicName;

    public JMSTopicRemove(final String topicName) {
        if(topicName == null) {
            throw new IllegalArgumentException("null name");
        }
        this.topicName = topicName;
    }

    /** {@inheritDoc} */
    protected void applyUpdate(JMSSubsystemElement element) throws UpdateFailedException {
        if(! element.removeTopic(topicName)) {
            throw new UpdateFailedException(String.format("topic (%s) does not exist", topicName));
        }
    }

    /** {@inheritDoc} */
    protected <P> void applyUpdate(UpdateContext context, UpdateResultHandler<? super Void, P> handler, P param) {
        final ServiceController<?> service = context.getServiceContainer().getService(JMSSubsystemElement.JMS_TOPIC_BASE.append(topicName));
        if(service == null) {
            handler.handleSuccess(null, param);
        } else {
            service.addListener(new UpdateResultHandler.ServiceRemoveListener<P>(handler, param));
        }
    }

    /** {@inheritDoc} */
    public AbstractSubsystemUpdate<JMSSubsystemElement, ?> getCompensatingUpdate(JMSSubsystemElement original) {
        final JMSTopicElement element = original.getTopic(topicName);
        if(element == null) {
            return null;
        }
        final JMSTopicAdd action = new JMSTopicAdd(topicName);
        action.setBindings(element.getBindings());
        return action;
    }

}
