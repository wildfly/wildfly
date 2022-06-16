/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
