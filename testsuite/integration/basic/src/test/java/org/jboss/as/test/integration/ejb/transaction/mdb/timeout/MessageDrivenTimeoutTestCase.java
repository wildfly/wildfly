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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.ejb.transaction.utils.SingletonChecker;
import org.jboss.as.test.integration.ejb.transaction.utils.TxTestUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(TransactionTimeoutQueueSetupTask.class)
public class MessageDrivenTimeoutTestCase {

    @ArquillianResource
    private InitialContext initCtx;

    @Inject
    private SingletonChecker checker;
    
    @Deployment
    public static Archive<?> deployment() {
        final Archive<?> deployment = ShrinkWrap.create(JavaArchive.class, "mdb-timeout.jar")
            .addPackage(MessageDrivenTimeoutTestCase.class.getPackage())
            .addPackage(TxTestUtil.class.getPackage())
            .addClass(TimeoutUtil.class);
        return deployment;
    }

    @Before
    public void startUp() throws NamingException {
        checker.resetAll();
    }

    @Test
    public void noTimeout() throws Exception {
        String text = "no timeout";
        Queue q = sendMessage(text, TransactionTimeoutQueueSetupTask.NO_TIMEOUT_JNDI_NAME);
        Assert.assertEquals("Sent and received message does not match at expected way",
                NoTimeoutMDB.REPLY_PREFIX + text, receiveMessage(q));

        Assert.assertEquals("Synchronization before completion has to be called", 1, checker.countSynchronizedBefore());
        Assert.assertEquals("Synchronization after completion has to be called", 1, checker.countSynchronizedAfter());
        Assert.assertEquals("Expecting one test XA resources being commmitted", 1, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }

    @Test
    public void transactionTimeoutAnnotation() throws Exception {
        String text = "annotation timeout";
        Queue q = sendMessage(text, TransactionTimeoutQueueSetupTask.ANNOTATION_TIMEOUT_JNDI_NAME);
        Assert.assertEquals("Sent and received message does not match at expected way",
                NoTimeoutMDB.REPLY_PREFIX + text, receiveMessage(q));
        
        Assert.assertEquals("Expecting one test XA resources being commmitted", 1, checker.getCommitted());
        Assert.assertEquals("Expecting no rollback happened", 0, checker.getRolledback());
    }
    
    @Test
    public void transactionTimeoutActivationProperty() throws Exception {
        String text = "activation property timeout";
            Queue q = sendMessage(text, TransactionTimeoutQueueSetupTask.PROPERTY_TIMEOUT_JNDI_NAME);
        Assert.assertNull("No message should be received as mdb timeouted", receiveMessage(q));
        
        Assert.assertEquals("Expecting no commmit happened", 0, checker.getCommitted());
        Assert.assertTrue("Expecting a rollback happened", checker.getRolledback() > 0);
    }

    private Queue sendMessage(String text, String queueJndi) throws Exception {
        QueueConnection connection = getConnection();
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
    
    private String receiveMessage(Queue replyQueue) throws Exception {
        QueueConnection connection = getConnection();
        connection.start();
        
        try {
            final QueueSession session = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            
            final QueueReceiver receiver = session.createReceiver(replyQueue);
            final Message reply = receiver.receive(5000);
            Thread.sleep(TimeoutUtil.adjust(500)); // waiting for synchro could be finished before checking
            if(reply == null) return null;
            return ((TextMessage) reply).getText();
        } finally {
            connection.close();
        }
    }

    private QueueConnection getConnection() throws Exception {
        final QueueConnectionFactory factory = (QueueConnectionFactory) initCtx.lookup("java:/JmsXA");
        return factory.createQueueConnection();
    }
}
