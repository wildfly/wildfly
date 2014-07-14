package org.jboss.as.test.integration.ejb.singleton.dependson.mdb;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

@MessageDriven(name = "AnnoBasedBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "${destination}") })
@ResourceAdapter("${resource.adapter}")
@DependsOn("CallCounterProxy")
public class MDBWhichDependsOn implements MessageListener {

    private static final Logger logger = Logger.getLogger(MDBWhichDependsOn.class);

    @EJB
    private CallCounterProxy counter;

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;

    @PostConstruct
    public void postConstruct() {
        this.logger.info("MDB.postConstruct");
        this.counter.setPostConstruct();
    }

    @PreDestroy
    public void preDestroy() {
        this.logger.info("MDB.preDestroy");
        this.counter.setPreDestroy();
    }

    @Override
    public void onMessage(Message message) {
        this.logger.info("MDB.message");
        this.counter.setMessage();
        try {
            final Destination replyTo = message.getJMSReplyTo();
            if (replyTo == null) {
                return;
            }

            logger.info("Sending a reply to destination " + replyTo);
            jmsMessagingUtil.reply(message);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        } finally {
        }
    }
}
