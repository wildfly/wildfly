/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.ejb.descriptor.replacement.mdb;

import java.util.Date;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.ejb.mdb.JMSMessagingUtil;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.logging.Logger;
import org.junit.Assert;

/**
 *
 * @author rhatlapa
 */
public class MDBTesting {

    private static final Logger logger = Logger.getLogger(MDBJBossSpecTestCase.class);
    protected static final String DEPLOYMENT_JBOSS_SPEC_ONLY = "jboss-spec";
    protected static final String DEPLOYMENT_WITH_REDEFINITION = "ejb3-specVsJboss-spec";

    /**
     * Inner class which setups and destroys queues used for testing Message driven beans via
     * descriptors
     */
    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue("mdbtest/queue", "java:jboss/mdbtest/queue");
            jmsAdminOperations.createJmsQueue("mdbtest/replyQueue", "java:jboss/mdbtest/replyQueue");
            jmsAdminOperations.createJmsQueue("mdbtest/redefinedQueue", "java:jboss/mdbtest/redefinedQueue");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue("mdbtest/queue");
                jmsAdminOperations.removeJmsQueue("mdbtest/replyQueue");
                jmsAdminOperations.removeJmsQueue("mdbtest/redefinedQueue");
                jmsAdminOperations.close();
            }
        }
    }
    
    @EJB(mappedName = "java:module/JMSMessagingUtil")
    private JMSMessagingUtil util;
    // queue which ejb-spec defined MDB listens to     
    @Resource(mappedName = "java:jboss/mdbtest/queue")
    private Queue queue;
    // queue which jboss-spec defined MDB listens to     
    @Resource(mappedName = "java:jboss/mdbtest/redefinedQueue")
    private Queue redefinedQueue;
    /**
     * queue where MDB puts replies
     */
    @Resource(mappedName = "java:jboss/mdbtest/replyQueue")
    private Queue replyQueue;
    
    private static final int TIMEOUT_MS = TimeoutUtil.adjust(1000);

    /**
     * tests MDB by sending different message to two different queues and checking answer from reply
     * queue with defined timeout
     *
     * descriptor should defined queue which MDB listens to
     *
     * @throws Exception
     */
    protected void testMDB(String messageForStandardQueue, String mesageForRedefinedQueue) throws Exception {

        this.util.sendTextMessage(messageForStandardQueue, this.queue, replyQueue);
        this.util.sendTextMessage(mesageForRedefinedQueue, this.redefinedQueue, replyQueue);
        logger.info("Start time of waiting for message in MDB test: " + new Date().getTime());
        final Message reply = this.util.receiveMessage(replyQueue, TIMEOUT_MS);
        logger.info("End time of waiting for message in MDB test: " + new Date().getTime());
        Assert.assertNotNull("Reply message was null on reply queue: " + this.replyQueue, reply);
        final String result = ((TextMessage) reply).getText();
        Assert.assertEquals("MDB should listen on redefinedQueue", "replying " + mesageForRedefinedQueue, result);
    }
}
