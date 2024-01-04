/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueReceiver;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.PropertyPermission;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(CreateQueueSetupTask.class)
public class JMSMessageDrivenBeanTestCase {
    @Deployment
    public static Archive<?> deployment() {
        final Archive<?> deployment = ShrinkWrap.create(JavaArchive.class, "JMSMessageDrivenBeanTestCase.jar")
                .addClasses(ReplyingMDB.class,JMSMessagingUtil.class)
                .addClass(CreateQueueSetupTask.class)
                .addClass(TimeoutUtil.class)
                .addAsResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
        return deployment;
    }

    @Test
    public void testSendMessage() throws JMSException, NamingException {
        final InitialContext ctx = new InitialContext();
        final QueueConnectionFactory factory = (QueueConnectionFactory) ctx.lookup("java:/JmsXA");
        final QueueConnection connection = factory.createQueueConnection();
        connection.start();
        try {
            final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            final Queue replyDestination = session.createTemporaryQueue();
            final QueueReceiver receiver = session.createReceiver(replyDestination);
            final Message message = session.createTextMessage("Test");
            message.setJMSReplyTo(replyDestination);
            final Destination destination = (Destination) ctx.lookup("queue/myAwesomeQueue");
            final MessageProducer producer = session.createProducer(destination);
            producer.send(message);
            producer.close();

            final Message reply = receiver.receive(TimeoutUtil.adjust(5000));
            assertNotNull(reply);
            final String result = ((TextMessage) reply).getText();
            assertEquals("replying Test", result);
        } finally {
            connection.close();
        }
    }
}
