/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.mdb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.jms.*;
import org.jboss.logging.Logger;

/**
 *
 * @author rhatlapa
 */
public class SimpleMessageDrivenBean implements MessageListener {

//    @Resource(lookup = "java:/JmsXA")
    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory factory;
    private Connection connection;
    private Session session;
    private static final Logger logger = Logger.getLogger(SimpleMessageDrivenBean.class);

    public void onMessage(Message message) {
        try {
            logger.info("MDB: Processing message: " + message);
            final Destination destination = message.getJMSReplyTo();
            logger.info("MDB: destination = " + destination);
            // ignore messages that need no reply
            if (destination == null) {
                logger.info("MDB encountered message without specified destination");
                return;
            }
            final MessageProducer replyProducer = session.createProducer(destination);
            logger.info("MDB replyProducer: " + replyProducer);
            logger.info("MDB session: " + session);
            logger.info("MDB text of orig message:" + ((TextMessage) message).getText());
            final Message replyMsg = session.createTextMessage("replying " + ((TextMessage) message).getText());
            logger.info("MDB: Message which shall be replied: " + ((TextMessage) replyMsg).getText());
            replyMsg.setJMSCorrelationID(message.getJMSMessageID());
            logger.info("MDB: Reply Message Correlation ID = " + replyMsg.getJMSCorrelationID());
            replyProducer.send(replyMsg);
            logger.info("Message " + replyMsg + " send");
            replyProducer.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    protected void preDestroy() throws JMSException {
        session.close();
        connection.stop();
        connection.close();
    }

    @PostConstruct
    protected void postConstruct() throws JMSException {
        connection = factory.createConnection();
        connection.start();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);       
    }
}
