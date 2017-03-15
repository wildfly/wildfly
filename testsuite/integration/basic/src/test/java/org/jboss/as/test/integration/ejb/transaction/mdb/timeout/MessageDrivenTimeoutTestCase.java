/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.transaction.mdb.timeout;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.PropertyPermission;
import javax.inject.Inject;
import javax.jms.Destination;
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
import javax.transaction.xa.XAResource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.transactions.TransactionCheckerSingleton;
import org.jboss.as.test.integration.transactions.TxTestUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test of timeout of global transaction where MDB receiving message
 * and mock {@link XAResource} is added to the mix to get 2PC processing.
 */
@RunWith(Arquillian.class)
@ServerSetup(TransactionTimeoutQueueSetupTask.class)
public class MessageDrivenTimeoutTestCase {

    @ArquillianResource
    private InitialContext initCtx;

    @Inject
    private TransactionCheckerSingleton checker;

    @Deployment
    public static Archive<?> deployment() {
        final Archive<?> deployment = ShrinkWrap.create(JavaArchive.class, "mdb-timeout.jar")
            .addPackage(MessageDrivenTimeoutTestCase.class.getPackage())
            .addPackage(TxTestUtil.class.getPackage())
            .addClass(TimeoutUtil.class)
            // grant necessary permissions for -Dsecurity.manager because of usage TimeoutUtil
            .addAsResource(createPermissionsXmlAsset(
                new PropertyPermission("ts.timeout.factor", "read")), "META-INF/jboss-permissions.xml");
        return deployment;
    }

    @Before
    public void startUp() throws NamingException {
        checker.resetAll();
    }

    /**
     * MDB receives a message where <code>onMessage</code> method using an {@link XAResource} and adding transaction
     * synchronization. The processing should be finished with sucessful 2PC commit.
     */
    @Test
    public void noTimeout() throws Exception {
        String text = "no timeout";
        Queue q = sendMessage(text, TransactionTimeoutQueueSetupTask.NO_TIMEOUT_JNDI_NAME, initCtx);
        Assert.assertEquals("Sent and received message does not match at expected way",
                NoTimeoutMDB.REPLY_PREFIX + text, receiveMessage(q, initCtx));

        Assert.assertEquals("Synchronization before completion has to be called", 1, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion has to be called", 1, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting one test XA resources being commmitted", 1, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }

    /**
     * MDB receives a message when annotated to have transaction timeout defined.
     * MDB using {@link XAResource} but processing takes longer than and timeout time is exceeded.
     * As transaction annotation does not affects MDB processing the commit should happen.
     */
    @Test
    public void transactionTimeoutAnnotation() throws Exception {
        String text = "annotation timeout";
        Queue q = sendMessage(text, TransactionTimeoutQueueSetupTask.ANNOTATION_TIMEOUT_JNDI_NAME, initCtx);
        Assert.assertEquals("Sent and received message does not match at expected way",
                AnnotationTimeoutMDB.REPLY_PREFIX + text, receiveMessage(q, initCtx));

        Assert.assertEquals("Expecting one test XA resources being commmitted", 1, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }

    /**
     * MDB receives a message MDB activation property is used to have transaction timeout defined.
     * MDB using {@link XAResource} but processing takes longer than and timeout time is exceeded.
     * As activation property instruct the RA to set transaction timeout then the transaction
     * should be rolled-back.
     */
    @Test
    public void transactionTimeoutActivationProperty() throws Exception {
        String text = "activation property timeout";
            Queue q = sendMessage(text, TransactionTimeoutQueueSetupTask.PROPERTY_TIMEOUT_JNDI_NAME, initCtx);
        Assert.assertNull("No message should be received as mdb timeouted", receiveMessage(q, initCtx));

        Assert.assertEquals("Expecting no commmit happened", 0, checker.getCommitted());
        Assert.assertTrue("Expecting a rollback happened", checker.getRolledback() > 0);
    }

    static Queue sendMessage(String text, String queueJndi, InitialContext initCtx) throws Exception {
        QueueConnection connection = getConnection(initCtx);
        connection.start();

        Queue replyDestination = null;

        try {
            final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            final Message message = session.createTextMessage(text);
            replyDestination = (Queue) initCtx.lookup(TransactionTimeoutQueueSetupTask.REPLY_QUEUE_JNDI_NAME);
            message.setJMSReplyTo(replyDestination);

            final Destination destination = (Destination) initCtx.lookup(queueJndi);
            final MessageProducer producer = session.createProducer(destination);
            producer.send(message);
            producer.close();
        } finally {
            connection.close();
        }
        return replyDestination;
    }

    static String receiveMessage(Queue replyQueue, InitialContext initCtx) throws Exception {
        QueueConnection connection = getConnection(initCtx);
        connection.start();

        try {
            final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

            final QueueReceiver receiver = session.createReceiver(replyQueue);
            final Message reply = receiver.receive(TimeoutUtil.adjust(5000));
            Thread.sleep(TimeoutUtil.adjust(500)); // waiting for synchro could be finished before checking
            if(reply == null) return null;
            return ((TextMessage) reply).getText();
        } finally {
            connection.close();
        }
    }

    static QueueConnection getConnection(InitialContext initCtx) throws Exception {
        final QueueConnectionFactory factory = (QueueConnectionFactory) initCtx.lookup("java:/JmsXA");
        return factory.createQueueConnection();
    }
}
