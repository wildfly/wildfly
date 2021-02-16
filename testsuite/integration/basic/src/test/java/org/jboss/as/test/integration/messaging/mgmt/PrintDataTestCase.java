/*
 * Copyright 2020 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.integration.messaging.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.NamingException;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.repository.PathUtil;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class PrintDataTestCase {

    protected final String jmsQueueName = "PrintDataTestCase-Queue";
    protected final String jmsQueueLookup = "jms/" + jmsQueueName;
    private static final Pattern ADDRESS_ID = Pattern.compile(";userRecordType=44;isUpdate=false;compactCount=0;"
            + "PersistentAddressBindingEncoding \\[id=([0-9]+), name=jms.queue.PrintDataTestCase-Queue, "
            + "routingTypes=\\{ANYCAST\\}, autoCreated=false\\]");
    private static final Pattern QUEUE_ID = Pattern.compile("userRecordType=21;isUpdate=false;compactCount=0;PersistentQueueBindingEncoding "
            + "\\[id=([0-9]+), name=jms.queue.PrintDataTestCase-Queue, address=jms.queue.PrintDataTestCase-Queue, filterString=null, "
            + "user=null, autoCreated=false, maxConsumers=-1, purgeOnNoConsumers=false, enabled=true, exclusive=false, lastValue=false,"
            + " lastValueKey=null, nonDestructive=false, consumersBeforeDispatch=0, delayBeforeDispatch=-1, routingType=1, "
            + "configurationManaged=false, groupRebalance=false, groupRebalancePauseDispatch=false, groupBuckets=-1, groupFirstKey=null, "
            + "autoDelete=false, autoDeleteDelay=0, autoDeleteMessageCount=0\\]");
    private static final Pattern SAFE_QUEUE_COUNT = Pattern.compile("queue id [0-9]+,count=1");

    @ContainerResource
    private Context remoteContext;

    @ContainerResource
    private ManagementClient managementClient;

    private Path dataPath;

    protected static void sendMessage(Context ctx, String destinationLookup, String text) throws NamingException, JMSException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        Destination destination = (Destination) ctx.lookup(destinationLookup);
        assertNotNull(destination);

        try (JMSContext context = cf.createContext("guest", "guest", JMSContext.AUTO_ACKNOWLEDGE)) {
            TextMessage message = context.createTextMessage(text);
            message.setJMSDeliveryMode(DeliveryMode.PERSISTENT);
            context.createProducer().send(destination, message);
        }
    }

    protected static void receiveMessage(Context ctx, String destinationLookup, boolean expectReceivedMessage, String expectedText) throws NamingException {
        ConnectionFactory cf = (ConnectionFactory) ctx.lookup("jms/RemoteConnectionFactory");
        assertNotNull(cf);
        Destination destination = (Destination) ctx.lookup(destinationLookup);
        assertNotNull(destination);

        try (JMSContext context = cf.createContext("guest", "guest")) {
            JMSConsumer consumer = context.createConsumer(destination);
            String text = consumer.receiveBody(String.class, TimeoutUtil.adjust(5000));
            if (expectReceivedMessage) {
                assertNotNull(text);
                assertEquals(expectedText, text);
            } else {
                assertNull("should not have received any message", text);
            }
        }
    }

    protected static ModelNode execute(ModelControllerClient client, ModelNode operation) throws Exception {
        ModelNode response = client.execute(operation);
        boolean success = SUCCESS.equals(response.get(OUTCOME).asString());
        if (success) {
            return response.get(RESULT);
        }
        throw new Exception("Operation failed");
    }

    @Before
    public void setUp() {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.createJmsQueue(jmsQueueName, "java:jboss/exported/" + jmsQueueLookup);
        jmsOperations.close();
    }

    @After
    public void tearDown() throws IOException {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.removeJmsQueue(jmsQueueName);
        jmsOperations.close();
        executeReloadAndWaitForCompletion(managementClient, false);
        Files.deleteIfExists(dataPath);
    }

    /**
     * Test that:
     *  - send a message to a queue.
     *  - put the server in admin mode.
     *  - print the journal.
     *  - checks that data are correctly printed.
     *  - checks that sensitive data are printed.
     * @throws Exception
     */
    @Test
    public void testPrintFullData() throws Exception {
        prepareQueue();
        // reload in admin-only mode
        executeReloadAndWaitForCompletion(managementClient, true);

        // export the journal (must be performed in admin-only mode)
        dataPath = new File("print-data.txt").toPath();
        Files.deleteIfExists(dataPath);
        printData(dataPath, false, false);
        String addressId = null;
        String queueId = null;
        boolean inMessages = false;
        boolean hasQueueCount = false;
        for (String line : Files.readAllLines(dataPath)) {
            if (addressId == null) {
                Matcher addressMatcher = ADDRESS_ID.matcher(line);
                if (addressMatcher.find()) {
                    addressId = addressMatcher.group(1);
                }
            } else {
                if (queueId == null) {
                    Matcher queueMatcher = QUEUE_ID.matcher(line);
                    if (queueMatcher.find()) {
                        queueId = queueMatcher.group(1);
                    }
                } else {
                    if (!inMessages) {
                        inMessages = "M E S S A G E S   J O U R N A L".equals(line);
                    } else {
                        if (!hasQueueCount) {
                            hasQueueCount = ("queue id " + queueId + ",count=1").equals(line);
                        }
                    }
                }
            }
        }
        Assert.assertNotNull("Address Id not found", addressId);
        Assert.assertNotNull("Queue Id not found", queueId);
        Assert.assertTrue("Should have a message count of 1 for " + jmsQueueName, hasQueueCount);
    }

    /**
     * Test that:
     *  - send a message to a queue.
     *  - put the server in admin mode.
     *  - print the journal in safe mode.
     *  - checks that data are correctly printed.
     *  - checks that sensitive data are not printed.
     * @throws Exception
     */
    @Test
    public void testPrintSafeData() throws Exception {
        prepareQueue();
        // reload in admin-only mode
        executeReloadAndWaitForCompletion(managementClient, true);

        // export the journal (must be performed in admin-only mode)
        dataPath = new File("print-safe-data.txt").toPath();
        Files.deleteIfExists(dataPath);
        printData(dataPath, true, false);
        checkSafeJournal();
    }

    /**
     * Test that:
     *  - send a message to a queue.
     *  - put the server in admin mode.
     *  - print the journal in safe mode and as an archive.
     *  - unzip the result of the operation.
     *  - checks that data are correctly printed.
     *  - checks that sensitive data are not printed.
     * @throws Exception
     */
    @Test
    public void testPrintSafeArchiveData() throws Exception {
        prepareQueue();
        // reload in admin-only mode
        executeReloadAndWaitForCompletion(managementClient, true);

        // export the journal (must be performed in admin-only mode)
        Path zipaPath = new File("print-safe-data.zip").toPath().toAbsolutePath();
        Files.deleteIfExists(zipaPath);
        printData(zipaPath, true, true);
        PathUtil.unzip(zipaPath, zipaPath.getParent());
        dataPath = zipaPath.getParent().resolve("data-print-report.txt");
        Files.deleteIfExists(zipaPath);
        checkSafeJournal();
    }

    private void checkSafeJournal() throws IOException {
        boolean inMessages = false;
        boolean hasQueueCount = false;
        String addressId = null;
        String queueId = null;
        for (String line : Files.readAllLines(dataPath)) {
            if (addressId == null) {
                Matcher addressMatcher = ADDRESS_ID.matcher(line);
                if (addressMatcher.find()) {
                    addressId = addressMatcher.group(1);
                }
            }
            if (queueId == null) {
                Matcher queueMatcher = QUEUE_ID.matcher(line);
                if (queueMatcher.find()) {
                    queueId = queueMatcher.group(1);
                }
            }
            if (!inMessages) {
                inMessages = "M E S S A G E S   J O U R N A L".equals(line);
            } else {
                if (!hasQueueCount) {
                    hasQueueCount = SAFE_QUEUE_COUNT.matcher(line).find();
                }
            }
        }
        Assert.assertNull("Address Id found", addressId);
        Assert.assertNull("Queue Id found", queueId);
        Assert.assertTrue("Should have a message count of 1 for " + jmsQueueName, hasQueueCount);
    }

    private void prepareQueue() throws Exception {
        // send a persistent message
        removeAllMessagesFromQueue(jmsQueueName);
        String text = "print-safe-data";
        sendMessage(remoteContext, jmsQueueLookup, text);
        Assert.assertEquals(1, countMessagesInQueue(jmsQueueName));
    }

    private void removeAllMessagesFromQueue(String jmsQueueName) throws Exception {
        ModelNode removeAllMessagesOp = new ModelNode();
        removeAllMessagesOp.get(OP_ADDR).add("subsystem", "messaging-activemq");
        removeAllMessagesOp.get(OP_ADDR).add("server", "default");
        removeAllMessagesOp.get(OP_ADDR).add("jms-queue", jmsQueueName);

        removeAllMessagesOp.get(OP).set("remove-messages");
        execute(managementClient.getControllerClient(), removeAllMessagesOp);
    }

    private int countMessagesInQueue(String jmsQueueName) throws Exception {
        ModelNode removeAllMessagesOp = new ModelNode();
        removeAllMessagesOp.get(OP_ADDR).add("subsystem", "messaging-activemq");
        removeAllMessagesOp.get(OP_ADDR).add("server", "default");
        removeAllMessagesOp.get(OP_ADDR).add("jms-queue", jmsQueueName);
        removeAllMessagesOp.get(OP).set("count-messages");
        return execute(managementClient.getControllerClient(), removeAllMessagesOp).asInt();
    }

    private void printData(Path file, boolean secret, boolean archive) throws Exception {
        ModelNode printDataOp = new ModelNode();
        printDataOp.get(OP_ADDR).add("subsystem", "messaging-activemq");
        printDataOp.get(OP_ADDR).add("server", "default");
        printDataOp.get(OP).set("print-data");
        printDataOp.get("secret").set(secret);
        printDataOp.get("archive").set(archive);
        OperationResponse response = managementClient.getControllerClient().executeOperation(Operation.Factory.create(printDataOp), OperationMessageHandler.logging);
        ModelNode result = response.getResponseNode();
        System.out.println("result = " + result);
        boolean success = SUCCESS.equals(result.get(OUTCOME).asString());
        if (success) {
            String uuid = result.get(RESULT).get("uuid").asString();
            try (InputStream in = response.getInputStream(uuid).getStream()) {
                Files.copy(in, file);
            }
        } else {
            throw new Exception("Operation failed " + result.get(FAILURE_DESCRIPTION));
        }
    }
}
