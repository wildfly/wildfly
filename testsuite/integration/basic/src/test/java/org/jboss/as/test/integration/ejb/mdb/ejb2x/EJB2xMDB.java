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

import javax.ejb.EJBException;
import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

/**
 * A replying EJB 2.x MDB.
 *
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class EJB2xMDB implements MessageDrivenBean, MessageListener {

    private static final Logger logger = Logger.getLogger(EJB2xMDB.class);

    private MessageDrivenContext mdbContext;

    private ConnectionFactory cf;

    public EJB2xMDB() {
    }

    public void ejbCreate() {
        InitialContext iniCtx = null;
        try {
            iniCtx = new InitialContext();
            cf = (ConnectionFactory) iniCtx.lookup("java:/ConnectionFactory");
        } catch (NamingException e) {
            throw new EJBException(e);
        }
    }

    @Override
    public void setMessageDrivenContext(MessageDrivenContext ctx) throws EJBException {
        this.mdbContext = ctx;
    }

    @Override
    public void ejbRemove() throws EJBException {
    }

    @Override
    public void onMessage(Message message) {
        logger.trace("Received message " + message + " in MDB " + this.getClass().getName());
        try {
            if (message.getStringProperty("MessageFormat") != null)
                logger.trace("MessageFormat property = " + message.getStringProperty("MessageFormat"));

            Destination replyTo = message.getJMSReplyTo();
            if (replyTo == null) {
                try {
                    logger.trace("mdbContext = " + mdbContext);
                    replyTo = (Destination) mdbContext.lookup("jms/replyQueue");
                } catch (Throwable e) {
                    logger.warn(e);
                }
            } else {
                logger.trace("Using replyTo from message JMSReplyTo: " + replyTo);
            }
            if (replyTo == null) {
                throw new EJBException("no replyTo Destination");
            }

            final TextMessage tm = (TextMessage) message;
            final String reply = tm.getText() + "processed by: " + hashCode();
            try (JMSContext context = cf.createContext()) {
                context.createProducer()
                        .setJMSCorrelationID(message.getJMSMessageID())
                        .send(replyTo, reply);
            }
        } catch (Exception e) {
            logger.error(e);
            throw new EJBException(e);
        }
    }

}
