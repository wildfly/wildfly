/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms.auxiliary;

import org.jboss.logging.Logger;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

/**
 * Auxiliary class for Jakarta Messaging smoke tests - receives messages from a topic and fires events afterwards
 *
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
@MessageDriven(
        name = "ShippingRequestProcessor",
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
                @ActivationConfigProperty(propertyName = "destination", propertyValue = "topic/myAwesomeTopic")
        }
)
public class TopicMessageDrivenBean implements MessageListener {

    private static final Logger logger = Logger.getLogger(TopicMessageDrivenBean.class);

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
