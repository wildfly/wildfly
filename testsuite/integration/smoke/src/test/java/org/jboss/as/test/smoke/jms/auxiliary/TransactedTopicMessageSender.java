/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms.auxiliary;

import org.jboss.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.context.RequestScoped;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/**
 * Auxiliary class for Jakarta Messaging smoke tests - sends messages to a topic from within a transaction
 *
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
@Stateful
@RequestScoped
public class TransactedTopicMessageSender {

    private static final Logger logger = Logger.getLogger(TransactedTopicMessageSender.class);

    @Resource(lookup = "java:/topic/myAwesomeTopic")
    private Topic topic;

    @Resource(lookup = "java:/ConnectionFactory")
    private ConnectionFactory factory;

    @Resource
    private SessionContext ctx;

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void sendToTopicSuccessfully() throws Exception {
        Connection connection = null;
        Session session = null;
        try {
            logger.trace("Creating a Connection");
            connection = factory.createConnection();
            logger.trace("Creating a Session");
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(topic);
            Message message = session.createTextMessage("Hello world!");
            logger.trace("Sending message");
            producer.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void sendToTopicAndRollback() throws JMSException {
        Connection connection = null;
        Session session = null;
        try {
            logger.trace("Creating a Connection");
            connection = factory.createConnection();
            logger.trace("Creating a Session");
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(topic);
            Message message = session.createTextMessage("Hello world 2!");
            logger.trace("Sending message");
            producer.send(message);
            // ROLLBACK
            ctx.setRollbackOnly();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }

        }


    }


}
