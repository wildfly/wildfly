/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.mdb;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
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
