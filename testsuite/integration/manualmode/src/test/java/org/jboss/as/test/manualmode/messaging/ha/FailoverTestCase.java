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

import java.io.File;
import java.io.FilenameFilter;

import javax.naming.InitialContext;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public abstract class FailoverTestCase extends AbstractMessagingHATestCase {
    private final Logger log = Logger.getLogger(FailoverTestCase.class);

    protected final String jmsQueueName = "FailoverTestCase-Queue";
    protected final String jmsQueueLookup = "jms/" + jmsQueueName;

    @Test
    public void testBackupActivation() throws Exception {
        ModelControllerClient client2 = createClient2();
        JMSOperations jmsOperations2 = JMSOperationsProvider.getInstance(client2);
        checkJMSQueue(jmsOperations2, jmsQueueName, false);

        InitialContext context1 = createJNDIContextFromServer1();
        sendAndReceiveMessage(context1, jmsQueueLookup);
        // send a message to server1 before it is stopped
        String text = "sent to server1, received from server2 (after failover)";
        sendMessage(context1, jmsQueueLookup, text);
        context1.close();

        log.trace("===================");
        log.trace("STOP SERVER1...");
        log.trace("===================");
        container.stop(SERVER1);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(jmsOperations2, true);
        checkJMSQueue(jmsOperations2, jmsQueueName, true);

        InitialContext context2 = createJNDIContextFromServer2();
        // receive the message that was sent to server1 before failover occurs
        receiveMessage(context2, jmsQueueLookup, text);
        sendAndReceiveMessage(context2, jmsQueueLookup);
        String text2 = "sent to server2, received from server 1 (after failback)";
        sendMessage(context2, jmsQueueLookup, text2);
        context2.close();

        log.trace("====================");
        log.trace("START SERVER1...");
        log.trace("====================");
        // restart the live server
        container.start(SERVER1);

        // let some time for the backup to detect the live node and failback
        ModelControllerClient client1 = createClient1();
        JMSOperations jmsOperations1 = JMSOperationsProvider.getInstance(client1);
        waitForHornetQServerActivation(jmsOperations1, true);
        checkHornetQServerStartedAndActiveAttributes(jmsOperations1, true, true);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(jmsOperations2, false);
        // backup server has been restarted in passive mode
        checkHornetQServerStartedAndActiveAttributes(jmsOperations2, true, false);

        checkJMSQueue(jmsOperations2, jmsQueueName, false);

        context1 = createJNDIContextFromServer1();
        // receive the message that was sent to server2 before failback
        receiveMessage(context1, jmsQueueLookup, text2);
        // send & receive a message from server1
        sendAndReceiveMessage(context1, jmsQueueLookup);
        context1.close();

        log.trace("=============================");
        log.trace("RETURN TO NORMAL OPERATION...");
        log.trace("=============================");
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

        log.trace("############## 1 #############");
        //listSharedStoreDir();

        log.trace("===================");
        log.trace("STOP SERVER1...");
        log.trace("===================");
        container.stop(SERVER1);

        log.trace("############## 2 #############");
        //listSharedStoreDir();

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true);
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);

        InitialContext context2 = createJNDIContextFromServer2();
        // receive the message that was sent to server1 before failover occurs
        receiveMessage(context2, jmsQueueLookup, text);
        // send a message to server2 before server1 fails back
        String text2 = "sent to server2, received from server 1 (after failback)";
        sendMessage(context2, jmsQueueLookup, text2);
        context2.close();

        log.trace("====================");
        log.trace("START SERVER1...");
        log.trace("====================");
        // restart the live server
        container.start(SERVER1);
        // let some time for the backup to detect the live node and failback
        ModelControllerClient client1 = createClient1();
        JMSOperations liveJMSOperations = JMSOperationsProvider.getInstance(client1);
        waitForHornetQServerActivation(liveJMSOperations, true);
        checkHornetQServerStartedAndActiveAttributes(liveJMSOperations, true, true);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, false);
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

        log.trace("==============================");
        log.trace("STOP SERVER1 A 2ND TIME...");
        log.trace("==============================");
        // shutdown server1 a 2nd time
        container.stop(SERVER1);

        // let some time for the backup to detect the failure
        waitForHornetQServerActivation(backupJMSOperations, true);
        checkHornetQServerStartedAndActiveAttributes(backupJMSOperations, true, true);
        checkJMSQueue(backupJMSOperations, jmsQueueName, true);

        context2 = createJNDIContextFromServer2();
        // receive the message that was sent to server1 before failover occurs a 2nd time
        receiveMessage(context2, jmsQueueLookup, text3);
        context2.close();

        //Assert.fail("wth");

    }

    private void listSharedStoreDir() {
        final File SHARED_STORE_DIR = new File(System.getProperty("java.io.tmpdir"), "activemq");
        if (!SHARED_STORE_DIR.exists()) {
            return;
        }
        log.trace("@@@@@@@@@@@@@@@@@@@@@@@@@");
        log.trace("SHARED_STORE_DIR = " + SHARED_STORE_DIR);
        for (File file : SHARED_STORE_DIR.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return true;
            }})) {
            log.trace(String.format("+ %s [r=%s,w=%s,x=%s]\n", file, file.canRead(), file.canWrite(), file.canExecute()));
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    log.trace("    + " + f);
                }
            }
        }
        log.trace("@@@@@@@@@@@@@@@@@@@@@@@@@");
    }
}
