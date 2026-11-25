package org.jboss.as.test.integration.messaging.jms.deployment;

import jakarta.inject.Inject;
import jakarta.jms.Destination;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;

public abstract class BaseConnectionPerSessionMDB implements MessageListener {

    public static final String SINGLE_CONNECTION_SYSTEM_PROPERTY_NAME = "mdb.single.connection";
    public static final String QUEUE_NAME = "myQueue";
    public static final String QUEUE_LOOKUP = "jms/queue/" + QUEUE_NAME;

    @Inject
    protected JMSContext context;

    @Override
    public void onMessage(final Message m) {
        try {
            TextMessage message = (TextMessage) m;
            Destination replyTo = m.getJMSReplyTo();

            if (replyTo != null) {
                context.createProducer()
                        .setJMSCorrelationID(message.getJMSMessageID())
                        .send(replyTo, message.getText());
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
