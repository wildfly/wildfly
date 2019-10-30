package org.jboss.as.test.integration.management.deploy.runtime.ejb.message;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.NamingException;

import org.jboss.logging.Logger;

@MessageDriven(name="POINT",activationConfig = {@ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/exported/" + Constants.QUEUE_JNDI_NAME)})
public class SimpleMDB implements MessageListener {

    private static final Logger log = Logger.getLogger(SimpleMDB.class.getName());
    //NOTE: this is local, above - ActivationConfigProperty has exported JNDI, wicked.
    @Resource(lookup = "java:/JmsXA")
    private ConnectionFactory factory;

    private Connection connection;
    private Session session;

    @Override
    public void onMessage(Message message) {
        try {
            log.trace(this + " received message " + message);
            final Destination destination = message.getJMSReplyTo();
            // ignore messages that need no reply
            if (destination == null) {
                log.trace(this + " noticed that no reply-to destination has been set. Just returning");
                return;
            }
            final MessageProducer replyProducer = session.createProducer(destination);
            final Message replyMsg = session.createTextMessage(Constants.REPLY_MESSAGE_PREFIX + ((TextMessage) message).getText());
            replyMsg.setJMSCorrelationID(message.getJMSMessageID());
            replyProducer.send(replyMsg);
            replyProducer.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    protected void preDestroy() throws JMSException {

        log.trace("@PreDestroy on " + this);
        safeClose(this.connection);

    }

    @PostConstruct
    protected void postConstruct() throws JMSException, NamingException {
        log.trace(this + " MDB @PostConstructed");

        this.connection = this.factory.createConnection();
        this.session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    static void safeClose(final Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Throwable t) {
            // just log
            log.trace("Ignoring a problem which occurred while closing: " + connection, t);
        }
    }
}