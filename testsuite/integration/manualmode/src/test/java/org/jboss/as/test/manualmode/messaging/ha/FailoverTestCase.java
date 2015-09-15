/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.messaging.ha;

import javax.naming.InitialContext;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public abstract class FailoverTestCase extends AbstractMessagingHATestCase {

    protected final String jmsQueueName = "FailoverTestCase-Queue";
    protected final String jmsQueueLookup = "jms/" + jmsQueueName;

    @Override
    protected void setUpServer1(ModelControllerClient client) throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);

    }

    @Override
    protected void setUpServer2(ModelControllerClient client) throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(client);
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
    }

    @Test
    public void testBackupActivation() throws Exception {
        ModelControllerClient client2 = createClient2();
        JMSOperations jmsOperations2 = JMSOperationsProvider.getInstance(client2);
        checkJMSQueue(jmsOperations2, jmsQueueName, false);

        InitialContext context1 = createJNDIContextFromServer1();
        sendAndReceiveMessage(context1, jmsQueueLookup);
        // send a message to server1 before it is stopped
        String text = "sent to server1, should be received on server 2 after failover";
        sendMessage(context1, jmsQueueLookup, text);
        context1.close();

        info("KILL SERVER #1...");
        container.kill(SERVER1);

        info("WAIT FOR SERVER #2 ACTIVATION...");
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(jmsOperations2, true);
        info("SERVER #2 ACTIVATED");

        checkJMSQueue(jmsOperations2, jmsQueueName, true);

        InitialContext context2 = createJNDIContextFromServer2();
        // receive the message that was sent to server1 before failover occurs
        receiveMessage(context2, jmsQueueLookup, text);
        sendAndReceiveMessage(context2, jmsQueueLookup);
        String text2 = "sent to server2, should be received on server 1 after failback";
        sendMessage(context2, jmsQueueLookup, text2);
        context2.close();

        Thread.sleep(EXPIRATION_TIME);

        info("RESTART SERVER #1...");
        // restart the live server
        container.start(SERVER1);

        // let some time for the backup to detect the live node and failback
        ModelControllerClient client1 = createClient1();
        JMSOperations jmsOperations1 = JMSOperationsProvider.getInstance(client1);
        info("WAIT FOR SERVER #1 ACTIVATION...");
        waitForHornetQServerActivation(jmsOperations1, true);
        info("SERVER #1 ACTIVATED...");
        checkHornetQServerStartedAndActiveAttributes(jmsOperations1, true, true);

        info("WAIT FOR SERVER #2 *DE*ACTIVATION...");
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(jmsOperations2, false);
        info("SERVER #2 *DE*ACTIVATED...");
        // backup server has been restarted in passive mode
        checkHornetQServerStartedAndActiveAttributes(jmsOperations2, true, false);

        checkJMSQueue(jmsOperations2, jmsQueueName, false);

        context1 = createJNDIContextFromServer1();
        // receive the message that was sent to server2 before failback
        receiveMessage(context1, jmsQueueLookup, text2);
        // send & receive a message from server1
        sendAndReceiveMessage(context1, jmsQueueLookup);
        context1.close();

        info("RETURN TO NORMAL OPERATION...");

        client2.close();
        client1.close();
    }

    @Test
    public void testBackupFailoverAfterFailback() throws Exception {
        ModelControllerClient client2 = createClient2();
        JMSOperations backupJMSOperations = JMSOperationsProvider.getInstance(client2);
        checkJMSQueue(backupJMSOperations, jmsQueueName, false);

        InitialContext context1 = createJNDIContextFromServer1();
        String text = "sent to server1, received from server2 (after failover)";
        sendMessage(context1, jmsQueueLookup, text);
        context1.close();

        info("KILL SERVER #1...");
        container.kill(SERVER1);

        info("WAIT FOR SERVER #2 ACTIVATION...");
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true);
        info("SERVER #2 ACTIVATED");
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);

        InitialContext context2 = createJNDIContextFromServer2();
        // receive the message that was sent to server1 before failover occurs
        receiveMessage(context2, jmsQueueLookup, text);
        // send a message to server2 before server1 fails back
        String text2 = "sent to server2, received from server 1 (after failback)";
        sendMessage(context2, jmsQueueLookup, text2);
        context2.close();

        Thread.sleep(EXPIRATION_TIME);

        info("RESTART SERVER #1...");
        // restart the live server
        container.start(SERVER1);
        // let some time for the backup to detect the live node and failback
        ModelControllerClient client1 = createClient1();
        JMSOperations liveJMSOperations = JMSOperationsProvider.getInstance(client1);
        info("WAIT FOR SERVER #1 ACTIVATION...");
        waitForHornetQServerActivation(liveJMSOperations, true);
        info("SERVER #1 ACTIVATED...");
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);


        info("WAIT FOR SERVER #2 *DE*ACTIVATION...");
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, false);
        info("SERVER #2 *DE*ACTIVATED...");
        // backup server has been restarted in passive mode
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, false);
        checkJMSQueue(backupJMSOperations, jmsQueueName, false);

        context1 = createJNDIContextFromServer1();
        // receive the message that was sent to server2 before failback
        receiveMessage(context1, jmsQueueLookup, text2);
        String text3 = "sent to server1, received from server2 (after 2nd failover)";
        // send a message to server1 before it is stopped a 2nd time
        sendMessage(context1, jmsQueueLookup, text3);
        context1.close();

        info("KILL SERVER #1 A 2ND TIME...");
        // shutdown server1 a 2nd time
        container.kill(SERVER1);

        info("WAIT FOR SERVER #2 ACTIVATION...");
        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true);
        info("SERVER #2 ACTIVATED...");
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, true);
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);

        context2 = createJNDIContextFromServer2();
        // receive the message that was sent to server1 before failover occurs a 2nd time
        receiveMessage(context2, jmsQueueLookup, text3);
        context2.close();

        info("RETURN TO NORMAL OPERATION...");

        client1.close();
        client2.close();
    }
}
