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
import org.jboss.as.test.integration.common.JMSAdminOperations;
import org.jboss.as.test.smoke.jms.aux.TopicMessageDrivenBean;
import org.jboss.as.test.smoke.jms.aux.TransactedTopicMessageSender;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.ejb.EJB;
import javax.enterprise.event.Observes;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

/**
 * Tests sending JMS messages to a topic within a transaction
 *
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
@RunWith(Arquillian.class)
public class SendToTopicFromWithinTransactionTest {

    private static final Logger logger = Logger.getLogger(SendToTopicFromWithinTransactionTest.class);

    @EJB
    private TransactedTopicMessageSender sender;

    @EJB
    private TransactedTopicMessageSender sender2;

    private static JMSAdminOperations adminOperations;

    private static volatile boolean messageReceived;

    @BeforeClass
    public static void deployQueue() throws Exception {
        adminOperations = new JMSAdminOperations();
        adminOperations.createJmsTopic("myAwesomeTopic30", "topic/myAwesomeTopic30");
    }

    @AfterClass
    public static void removeQueue() throws Exception {
        if (adminOperations != null) {
            adminOperations.removeJmsTopic("myAwesomeTopic30");
            adminOperations.close();
        }
    }

    @Before
    public void setMessageReceived() {
        messageReceived = false;
    }

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClass(TransactedTopicMessageSender.class)
                .addClass(TopicMessageDrivenBean.class)
                .addClass(JMSAdminOperations.class)
                .addAsManifestResource(
                        EmptyAsset.INSTANCE,
                        ArchivePaths.create("beans.xml"));
    }

    @Test
    public void sendSuccessfully() throws Exception {
        try {
            sender.sendToTopicSuccessfully();
            Thread.sleep(2000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Assert.assertTrue(messageReceived);
    }

    @Test
    public void sendAndRollback() {
        try {
            sender2.sendToTopicAndRollback();
            Thread.sleep(2000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Assert.assertFalse(messageReceived);
    }

    public void receivedMessage(@Observes Message message) {
        messageReceived = true;
        try {
            logger.info("caught event... message=" + ((TextMessage) message).getText());
        } catch (JMSException ex) {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }

}
