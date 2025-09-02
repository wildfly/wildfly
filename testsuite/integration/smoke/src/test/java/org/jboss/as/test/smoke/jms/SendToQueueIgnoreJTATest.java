/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.jms.auxiliary.CreateQueueSetupTask;
import org.jboss.as.test.smoke.jms.auxiliary.JMSListener;
import org.jboss.as.test.smoke.jms.auxiliary.QueueMessageDrivenBean;
import org.jboss.as.test.smoke.jms.auxiliary.TransactedQueueMessageSenderIgnoreJTA;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.ejb.EJB;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of fix for WFLY-9762.
 * Jakarta Messaging message s send and verified if is delivered. Difference is in creation if ConectionFactory (based JMSConnectionFactoryDefinition annotation with the transactional attribute)
 * and also tries rollback.
 *
 * Test is based on test from issue - https://github.com/javaee-samples/javaee7-samples/tree/master/jms/jms-xa
 *
 * @author <a href="jondruse@redhat.com">Jiri Ondrusek</a>
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(CreateQueueSetupTask.class)
public class SendToQueueIgnoreJTATest {

    private static int MAX_WAIT_IN_SECONDS = 15;

    @EJB
    private TransactedQueueMessageSenderIgnoreJTA sender;

    @EJB
    private JMSListener jmsListener;

    private CountDownLatch latch;

    @BeforeEach
    public void setMessageReceived() {
        latch = new CountDownLatch(1);
        jmsListener.setLatch(latch);
    }

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClass(TransactedQueueMessageSenderIgnoreJTA.class)
                .addClass(JMSListener.class)
                .addClass(QueueMessageDrivenBean.class)
                .addClass(CreateQueueSetupTask.class)
                .addPackage(JMSOperations.class.getPackage())
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        ArchivePaths.create("beans.xml"));
    }

    /**
     * Jakarta Messaging message is send using connection factory with transactional = false. Message should be delivered - main reason of fix.
     */
    @Test
    public void sendIgnoreJTA() throws Exception {
        sender.send(true, false);

        latch.await(MAX_WAIT_IN_SECONDS, SECONDS);

        assertEquals(0, latch.getCount());
    }

    /**
     * Jakarta Messaging message is send using connection factory with transactional = false and with rollback of Jakarta Transactions transaction.
     * Message should be still delivered as Jakarta Transactions transaction is ignored.
     */
    @Test
    public void sendAndRollbackIgnoreJTA() throws Exception {
        sender.send(true, true);

        latch.await(MAX_WAIT_IN_SECONDS, SECONDS);

        assertEquals(0, latch.getCount());
    }

    /**
     * Jakarta Messaging message is send using connection factory with transactional = true.
     * Messaging behaves as a part of Jakarta Transactions transaction, message should be delivered.
     */
    @Test
    public void sendInJTA() throws Exception {
        sender.send(false,false);

        latch.await(MAX_WAIT_IN_SECONDS, SECONDS);

        assertEquals(0, latch.getCount());
    }

    /**
     * Jakarta Messaging message is send using connection factory with transactional = true and Jakarta Transactions rollback
     * Messaging behaves as a part of Jakarta Transactions transaction, message should NOT be delivered.
     */
    @Test
    public void sendAndRollbackInJTA() throws  Exception {
        sender.send(false, true);

        latch.await(MAX_WAIT_IN_SECONDS, SECONDS);

        assertEquals(1, latch.getCount());
    }

}
