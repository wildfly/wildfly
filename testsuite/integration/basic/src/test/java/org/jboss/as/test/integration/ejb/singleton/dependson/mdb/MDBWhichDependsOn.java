package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.logging.Logger;

@MessageDriven(name = "AnnoBasedBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "${destination}")})
@DependsOn("CallCounterProxy")
public class MDBWhichDependsOn implements MessageListener {

    private static final Logger logger = Logger.getLogger(MDBWhichDependsOn.class);

    @EJB
    private CallCounterProxy counter;

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;

    @PostConstruct
    public void postConstruct() {
        logger.trace("MDB.postConstruct");
        this.counter.setPostConstruct();
    }

    @PreDestroy
    public void preDestroy() {
        logger.trace("MDB.preDestroy");
        this.counter.setPreDestroy();
    }

    @Override
    public void onMessage(Message message) {
        logger.trace("MDB.message");
        this.counter.setMessage();
        try {
            final Destination replyTo = message.getJMSReplyTo();
            if (replyTo == null) {
                return;
            }

            logger.trace("Sending a reply to destination " + replyTo);
            jmsMessagingUtil.reply(message);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        } finally {
        }
    }
}
