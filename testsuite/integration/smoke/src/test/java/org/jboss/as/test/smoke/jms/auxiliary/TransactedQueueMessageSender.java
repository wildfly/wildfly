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

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.jms.*;

/**
 * Auxiliary class for JMS smoke tests - sends messages to a queue from within a transaction
 *
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
@Stateful
@RequestScoped
public class TransactedQueueMessageSender {

    private static final Logger logger = Logger.getLogger(TransactedQueueMessageSender.class);

    @Resource(name = "java:/queue/myAwesomeQueue")
    private Queue queue;

    @Resource(name = "java:/ConnectionFactory")
    private ConnectionFactory factory;

    @Resource
    private SessionContext ctx;

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void sendToQueueSuccessfully() throws Exception {
        Connection connection = null;
        Session session = null;
        try {
            logger.info("Creating a Connection");
            connection = factory.createConnection();
            logger.info("Creating a Session");
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(queue);
            Message message = session.createTextMessage("Hello world!");
            logger.info("Sending message");
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
            logger.info("Creating a Connection");
            connection = factory.createConnection();
            logger.info("Creating a Session");
            session = connection.createSession(true, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(queue);
            Message message = session.createTextMessage("Hello world 2!");
            logger.info("Sending message");
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
