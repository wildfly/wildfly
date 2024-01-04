/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.ejb2x;

import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
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
