/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.mgmt.metrics;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.annotation.Resource;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.io.IOException;
import java.util.Map;
import java.util.PropertyPermission;

import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.io.File;
import java.io.FilePermission;
import org.jboss.remoting3.security.RemotingPermission;

/**
 * @author Ivan Straka
 */
@RunWith(Arquillian.class)
@ServerSetup({JMSThreadPoolMetricsSetup.class})
public class ThreadPoolMetricsTestCase {

    private static final Logger logger = Logger.getLogger(ThreadPoolMetricsTestCase.class);

    @Resource(mappedName = "java:/ConnectionFactory")
    ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:jboss/metrics/queue")
    private Queue queue;

    @Resource(mappedName = "java:jboss/metrics/replyQueue")
    private Queue replyQueue;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive getDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ArtemisThreadpoolMetricsTestCase.jar");
        ejbJar.addPackage(TimeoutUtil.class.getPackage());
        ejbJar.addClasses(JMSThreadPoolMetricsSetup.class, JMSThreadPoolMetricsMDB.class, JMSThreadPoolMetricsUtil.class);
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr, org.jboss.remoting\n"), "MANIFEST.MF");
        ejbJar.addAsManifestResource(createPermissionsXmlAsset(
                new FilePermission(System.getProperty("jboss.inst") + File.separatorChar + "standalone" + File.separatorChar + "tmp" + File.separatorChar + "auth" + File.separatorChar + "*", "read"),
                new PropertyPermission("ts.timeout.factor", "read"),
                RemotingPermission.CREATE_ENDPOINT,
                RemotingPermission.CONNECT), "jboss-permissions.xml");
        return ejbJar;
    }

    @Test
    public void metricsAvailableTest() throws IOException {
        Map<String, ModelNode> resources = JMSThreadPoolMetricsUtil.getResources(managementClient);
        // global thread pool metrics
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.ACTIVE_COUNT));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.COMPLETED_COUNT));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.CURRENT_COUNT));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.KEEPALIVE_TIME));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.LARGEST_COUNT));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.TASK_COUNT));
    }

    @Test
    public void globalThreadPoolMetricsTestCase() throws JMSException, IOException, InterruptedException {
        Map<String, ModelNode> resources = JMSThreadPoolMetricsUtil.getResources(managementClient);
        long activeCount = resources.get(JMSThreadPoolMetricsUtil.ACTIVE_COUNT).asLong();
        long completedCount = resources.get(JMSThreadPoolMetricsUtil.COMPLETED_COUNT).asLong();
        long currentCount = resources.get(JMSThreadPoolMetricsUtil.CURRENT_COUNT).asLong();
        long largestCount = resources.get(JMSThreadPoolMetricsUtil.LARGEST_COUNT).asLong();
        long taskCount = resources.get(JMSThreadPoolMetricsUtil.TASK_COUNT).asLong();

        long scheduledCompletedCount = resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_COMPLETED_COUNT).asLong();
        long scheduledCurrentCount = resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_CURRENT_COUNT).asLong();
        long scheduledLargestCount = resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_LARGEST_COUNT).asLong();
        long scheduledTaskCount = resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_TASK_COUNT).asLong();
        try (Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();

            for (int i = 0; i < JMSThreadPoolMetricsUtil.MESSAGES_COUNT; i++) {
                JMSThreadPoolMetricsUtil.send(session, queue, "id=" + i, false);
            }

            // we are in the middle of processing
            Thread.sleep(100);
            resources = JMSThreadPoolMetricsUtil.getResources(managementClient);

            JMSThreadPoolMetricsUtil.assertGreater("active count", resources.get(JMSThreadPoolMetricsUtil.ACTIVE_COUNT).asLong(), activeCount);
            JMSThreadPoolMetricsUtil.assertGreater("current count", resources.get(JMSThreadPoolMetricsUtil.CURRENT_COUNT).asLong(), 0);
            JMSThreadPoolMetricsUtil.assertGreaterOrEqual("current count", resources.get(JMSThreadPoolMetricsUtil.CURRENT_COUNT).asLong(), currentCount);
            Assert.assertEquals("scheduled current count", scheduledCurrentCount, resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_CURRENT_COUNT).asLong());

            // read responses
            for (int i = 0; i < JMSThreadPoolMetricsUtil.MESSAGES_COUNT; i++) {
                TextMessage textMessage = JMSThreadPoolMetricsUtil.receiveReply(session, replyQueue, TimeoutUtil.adjust(5000));
                logger.trace("got reply for [" + textMessage.getText() + "]");
            }

        }
        // messages have been processed
        resources = JMSThreadPoolMetricsUtil.getResources(managementClient);

        JMSThreadPoolMetricsUtil.assertGreater("completed count", resources.get(JMSThreadPoolMetricsUtil.COMPLETED_COUNT).asLong(), completedCount);
        JMSThreadPoolMetricsUtil.assertGreater("largest count", resources.get(JMSThreadPoolMetricsUtil.LARGEST_COUNT).asLong(), 0);
        JMSThreadPoolMetricsUtil.assertGreaterOrEqual("largest count", resources.get(JMSThreadPoolMetricsUtil.LARGEST_COUNT).asLong(), largestCount);
        JMSThreadPoolMetricsUtil.assertGreater("task count", resources.get(JMSThreadPoolMetricsUtil.TASK_COUNT).asLong(), taskCount);

        Assert.assertEquals("scheduled completed count", scheduledCompletedCount, resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_COMPLETED_COUNT).asLong());
        Assert.assertEquals("scheduled  count", scheduledCompletedCount, resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_COMPLETED_COUNT).asLong());
        Assert.assertEquals("scheduled largest count", scheduledLargestCount, resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_LARGEST_COUNT).asLong());
        Assert.assertEquals("scheduled task count", scheduledTaskCount, resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_TASK_COUNT).asLong());
    }
}
