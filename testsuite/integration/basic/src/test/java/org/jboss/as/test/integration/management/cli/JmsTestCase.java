/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.cli;

import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@ExtendWith(ArquillianExtension.class)
public class JmsTestCase extends AbstractCliTestBase {

    @BeforeAll
    public static void before() throws Exception {
        AbstractCliTestBase.initCLI();
    }

    @AfterAll
    public static void after() throws Exception {
        AbstractCliTestBase.closeCLI();
    }

    @Test
    public void testAddRemoveJmsQueue() throws Exception {
        testAddJmsQueue();
        testRemoveJmsQueue();
    }

    @Test
    public void testAddRemoveJmsTopic() throws Exception {
        testAddJmsTopic();
        testRemoveJmsTopic();
    }

    private void testAddJmsQueue() throws Exception {
        String queueName = "testJmsQueue";
        // check the queue is not registered
        cli.sendLine("cd /subsystem=messaging-activemq/server=default/jms-queue");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        assertFalse(ls.contains(queueName));

        // create queue
        cli.sendLine(String.format("jms-queue add --queue-address=%s --entries=%s", queueName, queueName));

        // check it is listed
        cli.sendLine("cd /subsystem=messaging-activemq/server=default/jms-queue");
        cli.sendLine("ls");
        ls = cli.readOutput();
        assertTrue(ls.contains(queueName));
    }

    private void testRemoveJmsQueue() throws Exception {
        String queueName = "testJmsQueue";

        // remove the queue
        cli.sendLine("jms-queue remove --queue-address=" + queueName);

        // check it is listed
        cli.sendLine("cd /subsystem=messaging-activemq/server=default/jms-queue");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        assertFalse(ls.contains(queueName));
    }

    private void testAddJmsTopic() throws Exception {

        // check the queue is not registered
        cli.sendLine("cd /subsystem=messaging-activemq/server=default/jms-topic");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        Assertions.assertNull(ls);

        // create topic
        cli.sendLine("jms-topic add --topic-address=testJmsTopic --entries=testJmsTopic");

        // check it is listed
        cli.sendLine("cd /subsystem=messaging-activemq/server=default/jms-topic");
        cli.sendLine("ls");
        ls = cli.readOutput();
        assertTrue(ls.contains("testJmsTopic"));
    }

    private void testRemoveJmsTopic() throws Exception {

        // create queue
        cli.sendLine("jms-topic remove --topic-address=testJmsTopic");

        // check it is listed
        cli.sendLine("cd /subsystem=messaging-activemq/server=default/jms-topic");
        cli.sendLine("ls");
        String ls = cli.readOutput();
        Assertions.assertNull(ls);
    }
}
