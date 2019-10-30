/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.mdb.messagedestination;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@MessageDriven(activationConfig = {
        // queue does not exist but it will be corrected by jboss-ejb3.xml
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/mdbtest/notExistingQueue")
})
public class ReplyingMDB implements MessageListener {
    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    @Override
    public void onMessage(Message message) {
        try {
            //System.out.println("Message " + message);
            final Destination replyTo = message.getJMSReplyTo();
            // ignore messages that need no reply
            if (replyTo == null)
                return;
            try (
                    JMSContext context = factory.createContext()
            ) {
                context.createProducer()
                        .setJMSCorrelationID(message.getJMSMessageID())
                        .send(replyTo, "replying " + ((TextMessage) message).getText());
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
