/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.enventry;

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
 * @author <a href="mailto:carlo@nerdnet.nl">Carlo de Wolf</a>
 */
@MessageDriven(name = "TestEnvEntryMD", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:jboss/queue/testEnvEntry") })
public class TestEnvEntryMDBean implements MessageListener {
    private static final Logger log = Logger.getLogger(TestEnvEntryMDBean.class);

    @Resource(mappedName = "java:/ConnectionFactory")
    private QueueConnectionFactory connectionFactory;

    @Resource(name = "maxExceptions")
    private int maxExceptions = 4;

    @Resource
    private int numExceptions = 3;

    private int minExceptions = 1;

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

                replyMsg.setInt("maxExceptions", maxExceptions);
                replyMsg.setInt("numExceptions", numExceptions);
                replyMsg.setInt("minExceptions", minExceptions);

                // System.err.println("reply to: " + destination);
                // System.err.println("maxExceptions: " + maxExceptions);
                // System.err.println("numExceptions: " + numExceptions);
                // System.err.println("minExceptions: " + minExceptions);

                replyProducer.send(replyMsg);
            } finally {
                conn.close();
            }
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
