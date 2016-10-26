package org.jboss.as.test.integration.ejb.mdb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

@MessageDriven(name = "AnnoBasedBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "${destination}") })
@ResourceAdapter("${resource.adapter}")
public class AnnoBasedMDB implements MessageListener {

    @EJB
    private JMSMessagingUtil jmsMessagingUtil;

    private static final Logger logger = Logger.getLogger(DDBasedMDB.class);

    @Override
    public void onMessage(Message message) {
        logger.trace("Received message " + message + " in MDB " + this.getClass().getName());
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
