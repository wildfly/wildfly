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

package org.jboss.as.test.smoke.jms.auxiliary;

import org.jboss.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * Auxiliary class for JMS smoke tests - receives messages from a queue and fires events afterwards
 *
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
@MessageDriven(
        name = "ShippingRequestProcessor",
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/myAwesomeQueue")
        }
)
public class QueueMessageDrivenBean implements MessageListener {

    private static final Logger logger = Logger.getLogger(QueueMessageDrivenBean.class);

    @Inject
    private Event<Message> event;

    public void onMessage(Message message) {
        try {
            logger.trace("message " + ((TextMessage) message).getText() + " received! Sending event.");
        } catch (JMSException e) {
            e.printStackTrace();
        }
        event.fire(message);
    }

}
