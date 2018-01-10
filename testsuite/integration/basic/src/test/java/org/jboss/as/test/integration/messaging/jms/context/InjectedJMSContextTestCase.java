/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.context;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.jboss.as.test.shared.TimeoutUtil.adjust;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertThat;

import java.util.PropertyPermission;
import java.util.UUID;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.messaging.jms.context.auxiliary.TransactedMDB;
import org.jboss.as.test.integration.messaging.jms.context.auxiliary.TransactedMessageProducer;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
public class InjectedJMSContextTestCase {

    public static final String QUEUE_NAME = "java:/InjectedJMSContextTestCaseQueue";

    @Resource(mappedName = "/JmsXA")
    private ConnectionFactory factory;

    @Resource(lookup = QUEUE_NAME)
    private Queue queue;

    @EJB
    private TransactedMessageProducer producerBean;

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "InjectedJMSContextTestCase.jar")
                .addClass(TimeoutUtil.class)
                .addPackage(TransactedMDB.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
    }

    @After
    public void tearDown() throws JMSException {
        // drain the queue to remove any pending messages from it
        try(JMSContext context = factory.createContext()) {
            JMSConsumer consumer = context.createConsumer(queue);
            Message m;
            do {
                m = consumer.receiveNoWait();
            }
            while (m != null);
        }
    }

    @Test
    public void sendAndReceiveWithContext() throws JMSException {
        String text = UUID.randomUUID().toString();

        try (JMSContext context = factory.createContext()) {

            TemporaryQueue tempQueue = context.createTemporaryQueue();

            context.createProducer()
                    .send(tempQueue, text);

            assertMessageIsReceived(tempQueue, context, text, false);
        }
    }

    @Test
    public void testSendWith_REQUIRED_transaction() throws JMSException {
        sendWith_REQUIRED_transaction(false);
    }

    @Test
    public void testSendWith_REQUIRED_transactionAndRollback() throws JMSException {
        sendWith_REQUIRED_transaction(true);
    }

    private void sendWith_REQUIRED_transaction(boolean rollback) throws JMSException {
        String text = UUID.randomUUID().toString();

        try (JMSContext context = factory.createContext()) {
            TemporaryQueue tempQueue = context.createTemporaryQueue();

            producerBean.sendToDestination(tempQueue, text, rollback);

            assertMessageIsReceived(tempQueue, context, text, rollback);
        }
    }

    @Test
    public void testSendAndReceiveFromMDB() throws JMSException {
        sendAndReceiveFromMDB(false);
    }

    @Test
    public void testSendAndReceiveFromMDBWithRollback() throws JMSException {
        sendAndReceiveFromMDB(true);
    }

    private void sendAndReceiveFromMDB(boolean rollback) throws JMSException {
        String text = "sendAndReceiveFromMDB " + rollback;
        System.out.println("InjectedJMSContextTestCase.sendAndReceiveFromMDB");
        System.out.println(">>>> queue = " + queue);

        try (JMSContext context = factory.createContext()) {
            TemporaryQueue replyTo = context.createTemporaryQueue();

            context.createProducer()
                    .setJMSReplyTo(replyTo)
                    .setProperty("rollback", rollback)
                    .send(queue, text);

            assertMessageIsReceived(replyTo, context, text, rollback);
        }
    }

    private void assertMessageIsReceived(Destination destination, JMSContext ctx, String expectedText, boolean rollback) {
        try (JMSConsumer consumer = ctx.createConsumer(destination)) {
            String t = consumer.receiveBody(String.class, adjust(2000));

            if (rollback) {
                assertThat("from " + destination, t, is(nullValue()));
            } else {
                assertThat("from " + destination, t, is(expectedText));
            }
        }
    }
}
