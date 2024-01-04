/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.mgmt.metrics;

import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.ejb3.annotation.ResourceAdapter;
import org.jboss.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * @author Ivan Straka
 */
@MessageDriven(name = "MetricsBean", activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "jboss/metrics/queue")})
@ResourceAdapter("activemq-ra.rar")
public class JMSThreadPoolMetricsMDB implements MessageListener {

    private static final Logger logger = Logger.getLogger(JMSThreadPoolMetricsMDB.class);

    @Resource(mappedName = "java:jboss/exported/jms/RemoteConnectionFactory")
    ConnectionFactory remoteCF;

    @Resource(mappedName = "java:/ConnectionFactory")
    ConnectionFactory invmCF;

    @Resource(mappedName = "java:jboss/metrics/replyQueue")
    private Queue replyQueue;

    @Override
    public void onMessage(Message m) {
        TextMessage message = (TextMessage) m;
        try (Connection connection = getConnection(message);
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            logger.trace("Simulate processing message: [" + message.getText() + "]");
            Thread.sleep(TimeoutUtil.adjust(500));
            final Destination replyTo = message.getJMSReplyTo();
            if (replyTo == null) {
                return;
            }
            logger.trace("Sending a reply for[" + message.getText() + "] to destination " + replyTo);
            JMSThreadPoolMetricsUtil.reply(session, replyQueue, message);
        } catch (JMSException | InterruptedException e) {
            logger.error("Error processing message ", e);
            throw new RuntimeException(e);
        }
    }

    private Connection getConnection(TextMessage message) throws JMSException {
        return JMSThreadPoolMetricsUtil.useRCF(message) ? remoteCF.createConnection("guest", "guest") : invmCF.createConnection();
    }
}
