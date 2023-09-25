/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.ejb2x;

import jakarta.ejb.EJBException;
import jakarta.ejb.MessageDrivenBean;
import jakarta.ejb.MessageDrivenContext;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
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
