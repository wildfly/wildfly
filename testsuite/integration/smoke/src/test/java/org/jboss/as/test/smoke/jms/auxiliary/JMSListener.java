package org.jboss.as.test.smoke.jms.auxiliary;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import java.util.concurrent.CountDownLatch;

import org.jboss.logging.Logger;

@MessageDriven(
    activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "java:/app/jms/nonXAQueue"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"), }
)

/**
 * Auxiliary class for Jakarta Messaging smoke tests - receives messages from a queue.
 * Test of fix for WFLY-9762
 *
 * @author <a href="jondruse@redhat.com">Jiri Ondrusek</a>
 */
public class JMSListener implements MessageListener {


    private static final Logger logger = Logger.getLogger(JMSListener.class.getName());

    private CountDownLatch latch;


    public void setLatch(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void onMessage(Message message) {
        try {
            logger.debug("Message received (async): " + message.getBody(String.class));

            latch.countDown();

        } catch (JMSException ex) {
            logger.error("Error onMessage", ex);
        }
    }
}
