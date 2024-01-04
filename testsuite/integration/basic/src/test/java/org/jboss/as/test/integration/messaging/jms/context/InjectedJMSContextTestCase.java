/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.jboss.as.test.shared.TimeoutUtil.adjust;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.PropertyPermission;
import java.util.UUID;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.TemporaryQueue;

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
