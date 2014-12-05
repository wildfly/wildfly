/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.mdb.ejb2x;

import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import static org.junit.Assert.fail;

/**
 * Common class for EJB 2.x MDB deployment tests.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public abstract class AbstractMDB2xTestCase {

    protected static final Logger logger = Logger.getLogger(AbstractMDB2xTestCase.class);

    protected Connection connection;
    protected Session session;

    @Before
    public void initCommon() {
        try {
            final InitialContext ic = new InitialContext();
            final ConnectionFactory cf = (ConnectionFactory) ic.lookup("java:/ConnectionFactory");
            connection = cf.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void cleanUp() {
        try {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    protected Message receiveMessage(final Destination destination, final long waitInMillis) {
        try {
            final MessageConsumer consumer = session.createConsumer(destination);
            return consumer.receive(waitInMillis);
        } catch (JMSException e) {
            e.printStackTrace();
            fail("Couldn't receive message from " + destination);
        }
        return null;
    }

    protected void sendTextMessage(final String msg, final Destination destination) {
        sendTextMessage(msg, destination, null);
    }

    protected void sendTextMessage(final String msg, final Destination destination, final Destination replyDestination) {
        sendTextMessage(msg, destination, replyDestination, null);
    }

    protected void sendTextMessage(final String msg, final Destination destination, final Destination replyDestination, final String messageFormat) {
        logger.debug("sending text message (" + msg + ") to " + destination);
        MessageProducer messageProducer = null;
        try {
            final TextMessage message = session.createTextMessage(msg);
            if (replyDestination != null) {
                message.setJMSReplyTo(replyDestination);
            }
            if (messageFormat != null) {
                message.setStringProperty("MessageFormat", messageFormat);
            }
            messageProducer = session.createProducer(destination);
            messageProducer.send(message);
            logger.debug("message sent");
        } catch (JMSException e) {
            e.printStackTrace();
            fail("Failed to send message to " + destination);
        } finally {
            try {
                if (messageProducer != null) {
                    messageProducer.close();
                }
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

}
