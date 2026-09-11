/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.input.Tailer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.observability.MessagingSubsystemSetupTask;
import org.jboss.as.test.shared.observability.NotificationProbeServlet;
import org.jboss.as.test.shared.observability.ResourceMetricsCheck;
import org.jboss.as.test.shared.observability.ServerLogTailerListener;
import org.jboss.as.test.shared.observability.signals.PrometheusMetric;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.plugin.tools.server.ServerManager;

/**
 * This test verifies the fix for WFLY-22219 as it relates to WildFly Metrics. Prior to the
 * change, only server resources that were present at boot time were registered with the
 * Metrics system for reporting. That meant, for example, if a JMS resource is added after the
 * server was booted, it would not be reported by the Metrics system. With this Jira, support
 * for adding dynamically added/removed resources was introduced to the subsystem, making use
 * of the WildFly kernel's notification system. This test uses the JMS use to verify that the
 * server can be started, resources added/removed, and that metrics will be reported for these
 * new resources.
 */
@RunWith(Arquillian.class)
@ServerSetup({SnapshotServerSetupTask.class,
        MetricsSubsystemSetupTask.class,
        MessagingSubsystemSetupTask.class})
@RunAsClient
public class MetricsResourceAddRemoveTestCase {
    private static final String DEPLOYMENT = "metrics-notification-probe.war";
    private static final String QUEUE_NAME = "metrics-resource-add-remove";
    private static final String QUEUE_JNDI_NAME = "java:/jms/queue/" + QUEUE_NAME;
    private static final int MESSAGE_COUNT = 10;
    private static final String METRIC_NAME = "wildfly_messaging_activemq_message_count";
    private static final String METRICS_CONTEXT = "/metrics";
    private static final String FAILED_READ = "WFLYCTL0216";
    private static final String FAILED_OPERATION = "WFLYCTL0013";
    private static final String MANIFEST = "Dependencies: org.jboss.as.controller,org.jboss.as.server,org.jboss.msc\n";
    private static final String WEB_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee" version="6.0">
                <servlet>
                    <servlet-name>notification-probe</servlet-name>
                    <servlet-class>""" + NotificationProbeServlet.class.getName() + """
                    </servlet-class>
                </servlet>
                <servlet-mapping>
                    <servlet-name>notification-probe</servlet-name>
                    <url-pattern>/</url-pattern>
                </servlet-mapping>
            </web-app>
            """;

    @ContainerResource
    private ManagementClient managementClient;

    @ContainerResource
    private ServerManager serverManager;

    @ArquillianResource
    private URL url;

    @Deployment(name = DEPLOYMENT, testable = false)
    public static Archive<?> deployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT)
                .addClasses(NotificationProbeServlet.class)
                .addAsManifestResource(new StringAsset(MANIFEST), "MANIFEST.MF")
                .addAsWebInfResource(new StringAsset(WEB_XML), "web.xml");
    }

    @BeforeClass
    public static void skipPreview() {
        // Needs embedded test broker which is not available in WildFly Preview
        AssumeTestGroupUtil.assumeNotWildFlyPreview();
    }

    @Test
    public void addsAndRemovesResourceMetrics() throws Exception {
        ServerLogTailerListener listener = new ServerLogTailerListener();
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        boolean queueCreated = false;
        try (Tailer ignored = Tailer.builder()
                .setFile(getServerLogFile())
                .setTailerListener(listener)
                .setDelayDuration(Duration.ofMillis(500))
                .get()) {
            clearNotifications();
            jmsOperations.createJmsQueue(QUEUE_NAME, QUEUE_JNDI_NAME);
            queueCreated = true;

            assertEventually(() -> assertNotification("resource-added"),
                    "JMS did not emit a resource-added notification for " + QUEUE_NAME);
            sendMessages();
            assertEventually(() -> assertQueueMetric(fetchMetrics(), true),
                    "JMS queue metric was not exported for " + QUEUE_NAME);
            listener.logs.clear();

            jmsOperations.removeJmsQueue(QUEUE_NAME);
            queueCreated = false;
            assertEventually(() -> assertNotification("resource-removed"),
                    "JMS did not emit a resource-removed notification for " + QUEUE_NAME);
            assertEventually(() -> assertQueueMetric(fetchMetrics(), false),
                    "JMS queue metric was not removed for " + QUEUE_NAME);
        } finally {
            if (queueCreated) {
                jmsOperations.removeJmsQueue(QUEUE_NAME);
            }
            jmsOperations.close();
        }
        listener.assertNoFailure();
        assertNoQueueReadErrors(listener);
    }

    private void sendMessages() throws Exception {
        String requestUrl = url + "?send=" + QUEUE_NAME;
        try (Client client = ClientBuilder.newClient()) {
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                try (Response response = client.target(requestUrl).request().get()) {
                    assertEquals(200, response.getStatus());
                }
            }
        }
    }

    private void clearNotifications() throws Exception {
        fetchNotifications(true);
    }

    private void assertNotification(String type) throws Exception {
        List<String> notifications = fetchNotifications(false);
        assertTrue(String.format("Notifications did not contain %s for %s: %s", type, QUEUE_NAME, notifications),
                notifications.stream().anyMatch(notification -> notification.startsWith(type + "|")
                        && notification.contains("jms-queue=" + QUEUE_NAME)));
    }

    private List<String> fetchNotifications(boolean clear) throws Exception {
        String requestUrl = url.toString();
        if (clear) {
            requestUrl += "?clear=true";
        }
        try (Client client = ClientBuilder.newClient(); Response response = client.target(requestUrl).request().get()) {
            assertEquals(200, response.getStatus());
            String body = response.readEntity(String.class);
            return body.isEmpty() ? List.of() : List.of(body.split("\\R"));
        }
    }

    private static void assertQueueMetric(List<PrometheusMetric> metrics, boolean expected) {
        boolean found = metrics.stream().anyMatch(metric -> METRIC_NAME.equals(metric.getKey())
                && "default".equals(metric.getTags().get("server"))
                && QUEUE_NAME.equals(metric.getTags().get("jms_queue")));
        assertEquals("Unexpected JMS queue metric state: " + metrics, expected, found);
    }

    private static void assertNoQueueReadErrors(ServerLogTailerListener listener) {
        assertFalse(listener.logs.stream().anyMatch(line -> line.contains(QUEUE_NAME)
                && (line.contains(FAILED_READ) || line.contains(FAILED_OPERATION))));
    }

    private File getServerLogFile() throws IOException {
        ModelNode address = Operations.createAddress("path", "jboss.server.log.dir");
        ModelNode operation = Operations.createReadAttributeOperation(address, "path");
        return new File(serverManager.executeOperation(operation).asString(), "server.log");
    }

    private List<PrometheusMetric> fetchMetrics() throws Exception {
        try (Client client = ClientBuilder.newClient();
             Response response = client.target(String.format("http://%s:%s%s", managementClient.getMgmtAddress(),
                     managementClient.getMgmtPort(), METRICS_CONTEXT)).request().get()) {
            assertEquals(200, response.getStatus());
            return PrometheusMetric.buildPrometheusMetrics(response.readEntity(String.class));
        }
    }

    private static void assertEventually(ResourceMetricsCheck check, String message) throws Exception {
        AssertionError failure = new AssertionError(message);
        long end = System.currentTimeMillis() + TimeoutUtil.adjust(30_000);
        while (System.currentTimeMillis() < end) {
            try {
                check.evaluate();
                return;
            } catch (AssertionError e) {
                failure = e;
                Thread.sleep(TimeoutUtil.adjust(1_000));
            }
        }
        throw failure;
    }
}
