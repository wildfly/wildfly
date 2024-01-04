/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.jms.context.transactionscoped;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertEquals;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.messaging.jms.context.transactionscoped.auxiliary.AppScopedBean;
import org.jboss.as.test.integration.messaging.jms.context.transactionscoped.auxiliary.ThreadLauncher;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.permission.ElytronPermission;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
@RunWith(Arquillian.class)
public class TransactionScopedJMSContextTestCase {

    public static final String QUEUE_NAME = "java:app/TransactionScopedJMSContextTestCase";

    @Resource(mappedName = "/JmsXA")
    private ConnectionFactory factory;

    @Resource(mappedName = QUEUE_NAME)
    private Queue queue;

    @EJB
    ThreadLauncher launcher;

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "TransactionScopedJMSContextTestCase.jar")
                .addPackage(AppScopedBean.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE,
                        "beans.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                        new ElytronPermission("getSecurityDomain")
                        ), "permissions.xml");
    }

    @Test
    public void sendAndReceiveWithContext() throws JMSException, InterruptedException {
        int numThreads = 5;
        int numMessages = 10;
        launcher.start(numThreads, numMessages);

        int receivedMessages = 0;
        try(JMSContext context = factory.createContext()) {
            JMSConsumer consumer = context.createConsumer(queue);
            Message m;
            do {
                m = consumer.receive(1000);
                if (m != null) {
                    receivedMessages++;
                }
            }
            while (m != null);
        }

        assertEquals(numThreads * numMessages, receivedMessages);
    }
}
