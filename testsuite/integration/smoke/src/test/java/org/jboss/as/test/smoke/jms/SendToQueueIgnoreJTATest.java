/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.smoke.jms;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

/**
 * Test of fix for WFLY-9762.
 * Jakarta Messaging message s send and verified if is delivered. Difference is in creation if ConectionFactory (based JMSConnectionFactoryDefinition annotation with the transactional attribute)
 * and also tries rollback.
 *
 * Test is based on test from issue - https://github.com/javaee-samples/javaee7-samples/tree/master/jms/jms-xa
 *
 * @author <a href="jondruse@redhat.com">Jiri Ondrusek</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(CreateQueueSetupTask.class)
public class SendToQueueIgnoreJTATest {

    private static int MAX_WAIT_IN_SECONDS = 15;

    @EJB
    private TransactedQueueMessageSenderIgnoreJTA sender;

    @EJB
    private JMSListener jmsListener;

    private CountDownLatch latch;

    @Before
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
