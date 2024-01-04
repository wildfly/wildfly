/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context;

import static jakarta.jms.DeliveryMode.NON_PERSISTENT;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.PropertyPermission;
import java.util.UUID;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import jakarta.jms.TemporaryQueue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.messaging.jms.context.auxiliary.BeanManagedMessageConsumer;
import org.jboss.as.test.integration.messaging.jms.context.auxiliary.RequestScopedMDB;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
public class ScopedInjectedJMSContextTestCase {

    public static final String QUEUE_NAME_FOR_REQUEST_SCOPE = "java:global/env/ScopedInjectedJMSContextTestCaseQueue-requestScoped";
    public static final String QUEUE_NAME_FOR_TRANSACTION_SCOPE = "java:global/env/ScopedInjectedJMSContextTestCaseQueue-transactionScoped";

    @Resource(mappedName = QUEUE_NAME_FOR_REQUEST_SCOPE)
    private Queue queueForRequestScope;

    @Resource(mappedName = QUEUE_NAME_FOR_TRANSACTION_SCOPE)
    private Queue queueForTransactionScope;

    @Resource(mappedName = "/JmsXA")
    private ConnectionFactory factory;

    @EJB
    private BeanManagedMessageConsumer transactedConsumer;

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "ScopedInjectedJMSContextTestCase.jar")
                .addClass(BeanManagedMessageConsumer.class)
                .addClass(RequestScopedMDB.class)
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                    new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")), "permissions.xml");
    }

    /**
     * Test that a transaction-scoped JMSContext is properly cleaned up after the transaction completion.
     */
    @Test
    public void transactionScoped() throws Exception {
        String text = UUID.randomUUID().toString();

        try (JMSContext context = factory.createContext()) {

            context.createProducer()
                    .setDeliveryMode(NON_PERSISTENT)
                    .send(queueForTransactionScope, text);

            assertTrue(transactedConsumer.receive(queueForTransactionScope, text));
        }
    }

    /**
     * Test that a request-scoped JMSContext is properly cleaned up after the transaction completion.
     */
    @Test
    public void requestScoped() throws Exception {
        String text = UUID.randomUUID().toString();

        try (JMSContext context = factory.createContext()) {

            TemporaryQueue tempQueue = context.createTemporaryQueue();
            // send a request/reply message to ensure that the MDB as received the message
            // and set its consumer field
            context.createProducer()
                    .setJMSReplyTo(tempQueue)
                    .setDeliveryMode(NON_PERSISTENT)
                    .send(queueForRequestScope, text);
            JMSConsumer consumer = context.createConsumer(tempQueue);
            String reply = consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
            assertNotNull(reply);

            JMSConsumer consumerInMDB = RequestScopedMDB.consumer;
            assertNotNull(consumerInMDB);

            try {
                consumerInMDB.receiveBody(String.class);
                fail("the EJB must throw an exception as its injected JMSContext must be closed after the MDB handled the message");
            } catch (Exception e) {
            }
        }
    }
}
