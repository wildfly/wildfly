/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.substitution;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.Session;

import org.jboss.logging.Logger;

/**
 * @author wangchao
 *
 */
@MessageDriven(name = "TestMD", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/queue/testQueue") })
public class SimpleMDB implements MessageListener {

    private static final Logger log = Logger.getLogger(SimpleMDB.class);

    @Resource(mappedName = "${resource.mappedname.connectionfactory}")
    private QueueConnectionFactory connectionFactory;

    @Resource(name = "${resource.name}")
    private String replyMessage;

    @Override
    public void onMessage(Message msg) {
        log.trace("OnMessage working...");
        try {
            Destination destination = msg.getJMSReplyTo();
            Connection conn = connectionFactory.createConnection();
            try {
                Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer replyProducer = session.createProducer(destination);
                MapMessage replyMsg = session.createMapMessage();
                replyMsg.setJMSCorrelationID(msg.getJMSMessageID());
                replyMsg.setString("replyMsg", replyMessage);
                replyProducer.send(replyMsg);
            } finally {
                conn.close();
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

}
