/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.jms;

import jakarta.ejb.EJB;
import jakarta.enterprise.event.Observes;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.jms.auxiliary.CreateTopicSetupTask;
import org.jboss.as.test.smoke.jms.auxiliary.TopicMessageDrivenBean;
import org.jboss.as.test.smoke.jms.auxiliary.TransactedTopicMessageSender;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests sending Jakarta Messaging messages to a topic within a transaction
 *
 * @author <a href="jmartisk@redhat.com">Jan Martiska</a>
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(CreateTopicSetupTask.class)
public class SendToTopicFromWithinTransactionTest {

    private static final Logger logger = Logger.getLogger(SendToTopicFromWithinTransactionTest.class);

    @EJB
    private TransactedTopicMessageSender sender;

    @EJB
    private TransactedTopicMessageSender sender2;

    private static volatile boolean messageReceived;

    @BeforeEach
    public void setMessageReceived() {
        messageReceived = false;
    }

    @Deployment
    public static JavaArchive createTestArchive() {
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addClass(TransactedTopicMessageSender.class)
                .addClass(TopicMessageDrivenBean.class)
                .addClass(CreateTopicSetupTask.class)
                .addPackage(JMSOperations.class.getPackage())
                .addAsManifestResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
    }

    @Test
    public void sendSuccessfully() throws Exception {
        try {
            sender.sendToTopicSuccessfully();
            Thread.sleep(2000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Assertions.assertTrue(messageReceived);
    }

    @Test
    public void sendAndRollback() {
        try {
            sender2.sendToTopicAndRollback();
            Thread.sleep(2000);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Assertions.assertFalse(messageReceived);
    }

    public void receivedMessage(@Observes Message message) {
        messageReceived = true;
        try {
            logger.trace("caught event... message=" + ((TextMessage) message).getText());
        } catch (JMSException ex) {
            ex.printStackTrace();
            Assertions.fail(ex.getMessage());
        }
    }

}
