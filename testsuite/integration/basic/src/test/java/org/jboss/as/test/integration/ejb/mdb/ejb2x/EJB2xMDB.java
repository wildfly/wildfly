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

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

/**
 * A replying EJB 2.x MDB.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class EJB2xMDB implements MessageDrivenBean, MessageListener {

    private static final Logger logger = Logger.getLogger(EJB2xMDB.class);

    private MessageDrivenContext ctx = null;
    private QueueConnection connection;
    private QueueSession session;

    public EJB2xMDB() {
    }

    @Override
    public void setMessageDrivenContext(MessageDrivenContext ctx) {
        this.ctx = ctx;
    }

    public void ejbCreate() {
        try {
            final InitialContext iniCtx = new InitialContext();
            final QueueConnectionFactory factory = (QueueConnectionFactory) iniCtx.lookup("java:/ConnectionFactory");
            connection = factory.createQueueConnection();
            session = connection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            connection.start();
        } catch (Exception e) {
            throw new EJBException("Failed to init EJB2xMDB", e);
        }
    }

    @Override
    public void ejbRemove() {
        ctx = null;
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

    @Override
    public void onMessage(Message message) {
        logger.info("Received message " + message + " in MDB " + this.getClass().getName());
        try {
            if (message.getStringProperty("MessageFormat") != null)
                logger.info("MessageFormat property = " + message.getStringProperty("MessageFormat"));

            Queue destination = (Queue) message.getJMSReplyTo();
            if (destination == null) {
                try {
                    destination = (Queue) this.ctx.lookup("jms/replyQueue");
                } catch (Throwable optional) {}
            }
            if (destination != null) {
                logger.info("replying to " + destination);
                final TextMessage tm = (TextMessage) message;
                final String text = tm.getText() + "processed by: " + hashCode();
                final QueueSender sender = session.createSender(destination);
                final TextMessage reply = session.createTextMessage(text);
                reply.setJMSCorrelationID(message.getJMSMessageID());
                sender.send(reply);
                sender.close();
            }
        } catch (Exception e) {
            logger.error(e);
            throw new EJBException(e);
        }
    }

}
