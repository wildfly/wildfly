/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RunAs;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.EJB;
import jakarta.ejb.MessageDriven;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.TextMessage;

import org.jboss.logging.Logger;

/**
 * MDB with RunAs annotation. Takes data from in queue and replies to reply queue.
 *
 * @author Ondrej Chaloupka
 */
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/exported/queue/TestQueue"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")})
@RunAs("INTERNAL_ROLE")
@PermitAll // needed for access not being denied, see WFLY-8560
public class HelloMDB implements MessageListener {
    private static final Logger log = Logger.getLogger(HowdyBean.class);

    @EJB
    Howdy howdy;

    @Resource(mappedName = "java:/ConnectionFactory")
    private QueueConnectionFactory qFactory;

    @Resource(lookup = "java:global/runasmdbejb-ejb2/GoodBye!org.jboss.as.test.integration.ejb.security.runas.ejb2mdb.GoodByeLocalHome")
    GoodByeLocalHome goodByeHome;

    public void onMessage(Message message) {
        try {
            GoodByeLocal goodBye = goodByeHome.create();
            String messageToReply = String.format("%s! %s.", howdy.sayHowdy(), goodBye.sayGoodBye());

            sendReply(messageToReply, (Queue) message.getJMSReplyTo(), message.getJMSMessageID());
        } catch (Exception e) {
            log.errorf(e, "Can't process message '%s'", message);
        }
    }

    private void sendReply(String msg, Queue destination, String messageID) throws JMSException {
        QueueConnection conn = qFactory.createQueueConnection("guest", "guest");
        try {
            QueueSession session = conn.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            conn.start();
            QueueSender sender = session.createSender(destination);
            TextMessage message = session.createTextMessage(msg);
            message.setJMSCorrelationID(messageID);
            sender.send(message, DeliveryMode.NON_PERSISTENT, 4, 500);
        } finally {
            conn.close();
        }
    }
}
