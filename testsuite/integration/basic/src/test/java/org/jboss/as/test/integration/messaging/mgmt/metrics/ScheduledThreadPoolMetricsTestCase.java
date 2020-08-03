/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;
import java.util.Map;
import java.util.PropertyPermission;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.File;
import java.io.FilePermission;
import java.net.SocketPermission;
import org.jboss.remoting3.security.RemotingPermission;

/**
 * @author Ivan Straka
 */
@RunWith(Arquillian.class)
@ServerSetup({JMSThreadPoolMetricsSetup.class})
public class ScheduledThreadPoolMetricsTestCase {

    private static final Logger logger = Logger.getLogger(ScheduledThreadPoolMetricsTestCase.class);

    @Resource(mappedName = "java:jboss/exported/jms/RemoteConnectionFactory")
    ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:jboss/metrics/queue")
    private Queue queue;

    @Resource(mappedName = "java:jboss/metrics/replyQueue")
    private Queue replyQueue;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static JavaArchive getDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ArtemisScheduledThreadpoolMetricsTestCase.jar");
        ejbJar.addPackage(TimeoutUtil.class.getPackage());
        ejbJar.addClasses(JMSThreadPoolMetricsSetup.class, JMSThreadPoolMetricsMDB.class, JMSThreadPoolMetricsUtil.class);
        ejbJar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr,org.jboss.remoting\n"), "MANIFEST.MF");
        ejbJar.addAsManifestResource(createPermissionsXmlAsset(
                new FilePermission(System.getProperty("jboss.inst") + File.separatorChar + "standalone" + File.separatorChar + "tmp" + File.separatorChar + "auth" + File.separatorChar + "*", "read"),
                new PropertyPermission("ts.timeout.factor", "read"),
                RemotingPermission.CREATE_ENDPOINT,
                RemotingPermission.CONNECT,
                new SocketPermission("localhost", "resolve")), "jboss-permissions.xml");
        return ejbJar;
    }

    @Test
    public void metricsAvailableTest() throws IOException {
        Map<String, ModelNode> resources = JMSThreadPoolMetricsUtil.getResources(managementClient);
        // scheduled thread pool metrics
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.SCHEDULED_ACTIVE_COUNT));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.SCHEDULED_COMPLETED_COUNT));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.SCHEDULED_CURRENT_COUNT));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.SCHEDULED_KEEPALIVE_TIME));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.SCHEDULED_LARGEST_COUNT));
        Assert.assertTrue(resources.containsKey(JMSThreadPoolMetricsUtil.SCHEDULED_TASK_COUNT));
    }

    @Test
    public void scheduledGlobalThreadPoolMetricsTestCase() throws JMSException, IOException, InterruptedException {
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
        try (Connection connection = connectionFactory.createConnection("guest", "guest");
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
            connection.start();
            for (int i = 0; i < JMSThreadPoolMetricsUtil.MESSAGES_COUNT; i++) {
                JMSThreadPoolMetricsUtil.send(session, queue, "id=" + i, true);
            }

            // we are in the middle of processing
            Thread.sleep(TimeoutUtil.adjust(100));
            resources = JMSThreadPoolMetricsUtil.getResources(managementClient);
            JMSThreadPoolMetricsUtil.assertGreater("active count", resources.get(JMSThreadPoolMetricsUtil.ACTIVE_COUNT).asLong(), activeCount);
            JMSThreadPoolMetricsUtil.assertGreater("current count", resources.get(JMSThreadPoolMetricsUtil.CURRENT_COUNT).asLong(), currentCount);

            // there is no point in checking active count in scheduledThreadPool since we can not determine the exact time
            // tasks are being executed
            JMSThreadPoolMetricsUtil.assertGreater("scheduled current count", resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_CURRENT_COUNT).asLong(), 0);
            JMSThreadPoolMetricsUtil.assertGreaterOrEqual("scheduled current count", resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_CURRENT_COUNT).asLong(), scheduledCurrentCount);

            // read responses
            for (int i = 0; i < JMSThreadPoolMetricsUtil.MESSAGES_COUNT; i++) {
                TextMessage textMessage = JMSThreadPoolMetricsUtil.receiveReply(session, replyQueue, 5000);
                logger.trace("got reply for [" + textMessage.getText() + "]");
            }
        }
        // messages have been processed
        resources = JMSThreadPoolMetricsUtil.getResources(managementClient);

        JMSThreadPoolMetricsUtil.assertGreater("completed count", resources.get(JMSThreadPoolMetricsUtil.COMPLETED_COUNT).asLong(), completedCount);
        JMSThreadPoolMetricsUtil.assertGreater("largest count", resources.get(JMSThreadPoolMetricsUtil.LARGEST_COUNT).asLong(), 0);
        JMSThreadPoolMetricsUtil.assertGreaterOrEqual("largest count", resources.get(JMSThreadPoolMetricsUtil.LARGEST_COUNT).asLong(), largestCount);
        JMSThreadPoolMetricsUtil.assertGreater("task count", resources.get(JMSThreadPoolMetricsUtil.TASK_COUNT).asLong(), taskCount);

        JMSThreadPoolMetricsUtil.assertGreater("scheduled completed count", resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_COMPLETED_COUNT).asLong(), scheduledCompletedCount);
        JMSThreadPoolMetricsUtil.assertGreater("scheduled largest count", resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_LARGEST_COUNT).asLong(), 0);
        JMSThreadPoolMetricsUtil.assertGreaterOrEqual("scheduled largest count", resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_LARGEST_COUNT).asLong(), scheduledLargestCount);
        JMSThreadPoolMetricsUtil.assertGreater("scheduled task count", resources.get(JMSThreadPoolMetricsUtil.SCHEDULED_TASK_COUNT).asLong(), scheduledTaskCount);
    }
}
