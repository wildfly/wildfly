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
import jakarta.jms.Queue;
import jakarta.jms.Session;

/**
 * Auxiliary class for Jakarta Messaging smoke tests - sends messages to a queue from within a transaction
 *
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
@Stateful
@RequestScoped
public class TransactedQueueMessageSender {

    private static final Logger logger = Logger.getLogger(TransactedQueueMessageSender.class);

    @Resource(lookup = "java:/queue/myAwesomeQueue")
    private Queue queue;

    @Resource(lookup = "java:/ConnectionFactory")
    private ConnectionFactory factory;

    @Resource
    private SessionContext ctx;

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void sendToQueueSuccessfully() throws Exception {
        Connection connection = null;
        Session session = null;
        try {
            logger.trace("Creating a Connection");
            connection = factory.createConnection();
            logger.trace("Creating a Session");
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(queue);
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
    public void sendToQueueAndRollback() throws JMSException {
        Connection connection = null;
        Session session = null;
        try {
            logger.trace("Creating a Connection");
            connection = factory.createConnection();
            logger.trace("Creating a Session");
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(queue);
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
