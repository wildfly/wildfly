/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.mgmt.metrics;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ivan Straka
 */
public class JMSThreadPoolMetricsUtil {

    public static final String SCHEDULED_ACTIVE_COUNT = "global-client-scheduled-thread-pool-active-count";
    public static final String SCHEDULED_COMPLETED_COUNT = "global-client-scheduled-thread-pool-completed-task-count";
    public static final String SCHEDULED_CURRENT_COUNT = "global-client-scheduled-thread-pool-current-thread-count";
    public static final String SCHEDULED_KEEPALIVE_TIME = "global-client-scheduled-thread-pool-keepalive-time";
    public static final String SCHEDULED_LARGEST_COUNT = "global-client-scheduled-thread-pool-largest-thread-count";
    public static final String SCHEDULED_TASK_COUNT = "global-client-scheduled-thread-pool-task-count";

    public static final String ACTIVE_COUNT = "global-client-thread-pool-active-count";
    public static final String COMPLETED_COUNT = "global-client-thread-pool-completed-task-count";
    public static final String CURRENT_COUNT = "global-client-thread-pool-current-thread-count";
    public static final String KEEPALIVE_TIME = "global-client-thread-pool-keepalive-time";
    public static final String LARGEST_COUNT = "global-client-thread-pool-largest-thread-count";
    public static final String TASK_COUNT = "global-client-thread-pool-task-count";
    public static final int MESSAGES_COUNT = 10;

    private static final ModelNode READ_MESSAGING_SUBSYSTEM_OPERATION = new ModelNode();

    static {
        ModelNode serverAddress = new ModelNode();
        serverAddress.add("subsystem", "messaging-activemq");

        READ_MESSAGING_SUBSYSTEM_OPERATION.get("address").set(serverAddress);
        READ_MESSAGING_SUBSYSTEM_OPERATION.get("operation").set("read-resource");
        READ_MESSAGING_SUBSYSTEM_OPERATION.get("include-runtime").set(true);
    }

    public static Map<String, ModelNode> getResources(ManagementClient client) throws IOException {
        Map<String, ModelNode> resources = new HashMap<>();
        List<Property> propertyList = client.getControllerClient().execute(READ_MESSAGING_SUBSYSTEM_OPERATION).require("result").asPropertyList();
        for (Property property : propertyList) {
            resources.put(property.getName(), property.getValue());
        }
        return resources;
    }

    public static void send(final Session session, final Destination dest, final String text, final boolean useRCF) throws JMSException {
        TextMessage message = session.createTextMessage(text);
        message.setJMSReplyTo(dest);
        message.setBooleanProperty("useRCF", useRCF);

        try (MessageProducer messageProducer = session.createProducer(dest)) {
            messageProducer.send(message);
        }
    }

    public static boolean useRCF(TextMessage message) throws JMSException {
        return message.getBooleanProperty("useRCF");
    }

    public static void reply(final Session session, final Destination dest, final TextMessage message) throws JMSException {
        final Message replyMsg = session.createTextMessage(message.getText());
        replyMsg.setJMSCorrelationID(message.getJMSMessageID());

        try (MessageProducer messageProducer = session.createProducer(dest)) {
            messageProducer.send(message);
        }
    }

    public static TextMessage receiveReply(final Session session, final Destination dest, final long waitInMillis) throws JMSException {
        try (MessageConsumer consumer = session.createConsumer(dest)) {
            return (TextMessage) consumer.receive(waitInMillis);
        }
    }

    public static void assertGreater(String message, long greater, long lesser) {
        Assert.assertTrue(String.format("%s: expected <%d> is greater than <%d>", message, greater, lesser), greater > lesser);
    }

    public static void assertGreaterOrEqual(String message, long greater, long lesser) {
        Assert.assertTrue(String.format("%s: expected <%d> is greater or equal to <%d>", message, greater, lesser), greater >= lesser);
    }
}
