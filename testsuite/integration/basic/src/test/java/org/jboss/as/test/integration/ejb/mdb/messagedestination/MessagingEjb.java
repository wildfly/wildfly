/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * User: jpai
 */
@Stateless
public class MessagingEjb {

    private ConnectionFactory connectionFactory;

    private Connection connection;

    private Session session;

    @Resource(name = "myQueue")
    private Queue queue;

    @Resource(name = "myReplyQueue")
    private Queue replyQueue;

    @PreDestroy
    protected void preDestroy() throws JMSException {
        session.close();
        connection.close();
    }

    @PostConstruct
    protected void postConstruct() throws JMSException {
        connection = this.connectionFactory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    @Resource(mappedName = "java:/ConnectionFactory")
    public void setConnectionFactory(final ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void sendTextMessage(final String msg) throws JMSException {
        final TextMessage message = session.createTextMessage(msg);
        this.sendMessage(message, queue, replyQueue);
    }


    public void reply(final Message message) throws JMSException {
        final Destination destination = message.getJMSReplyTo();
        // ignore messages that need no reply
        if (destination == null) {
            return;
        }
        final Message replyMsg = session.createTextMessage("replying to message: " + message.toString());
        replyMsg.setJMSCorrelationID(message.getJMSMessageID());
        this.sendMessage(replyMsg, destination, null);
    }

    public Message receiveMessage(final long waitInMillis) throws JMSException {
        MessageConsumer consumer = this.session.createConsumer(replyQueue);
        return consumer.receive(waitInMillis);
    }

    private void sendMessage(final Message message, final Destination destination, final Destination replyDestination) throws JMSException {
        if (replyDestination != null) {
            message.setJMSReplyTo(replyDestination);
        }
        final MessageProducer messageProducer = session.createProducer(destination);
        messageProducer.send(message);
        messageProducer.close();
    }
}
