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

package org.jboss.as.test.integration.ejb.transaction.messaging;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.jms.TemporaryQueue;

/**
 * Message driven bean with TransactionManagementType.CONTAINER tries to access cdi bean with reference to userTransaction.
 * If mdb calls method without userTransaction, it should work. See WFLY-10293 for more details.
 *
 * @author Jiri Ondrusek <jondruse@redhat.com>
 */
@RunWith(Arquillian.class)
public class CmtAccessCdiWithUserTransactionTestCase {
    private static final Logger LOGGER = Logger.getLogger(CmtAccessCdiWithUserTransactionTestCase.class);

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory factory;

    @Resource(mappedName = MDB.JNDI_NAME)
    private Queue queue;

    @Deployment
    public static WebArchive createTestArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "CmtAccessToCdiWithUserTransactionTestCase.war")
                .addPackage(CmtAccessCdiWithUserTransactionTestCase.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return archive;
    }

    @Test
    public void testAcessWithoutUserTransaction() {
        String result = test(MDB.TXT1);
        Assert.assertEquals("Method without transaction should be successfully called.", MDB.TXT1, result);
    }

    @Test
    public void testAcessWithtUserTransaction() {
        String result = test(MDB.TXT2);
        Assert.assertNull("Method with transaction should fail and there should be no returned message.", result);
    }

    private String test(String msg) {
        try (JMSContext context = factory.createContext()) {
            TemporaryQueue replyTo = context.createTemporaryQueue();

            String text = msg;
            context.createProducer()
                .setJMSReplyTo(replyTo)
                .send(queue, text);

            JMSConsumer consumer = context.createConsumer(replyTo);
            String reply = consumer.receiveBody(String.class, 5000);
            return reply;
        }
    }
}
